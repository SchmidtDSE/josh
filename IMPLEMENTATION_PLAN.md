# Josh MCP Server — Implementation Plan

## Goal

Expose Josh as a Model Context Protocol (MCP) server so that a local LLM
client (e.g. opencode running in the user's sandbox) can invoke Josh
commands — primarily `preprocess` and `run` — against files on the
user's local filesystem.

Compute may execute either locally (in-JVM) or on a remote Josh Cloud /
k8s backend, but **files always live on the user's machine**. The MCP
server itself always runs locally; "remote" refers only to where the
heavy computation happens.

## Non-goals (for v1)

- Running the MCP server itself as a remote/hosted service. (Door left
  open via forward-compatibility discipline below, but not built.)
- HTTP / Streamable HTTP transport. Stdio only.
- Path sandboxing / root allow-lists. CWD is the implicit sandbox, same
  as every other canonical MCP server (filesystem, git, sqlite, etc.).
- Exposing cluster/operations commands (`server`, `stageToMinio`,
  `stageFromMinio`, `pollBatch`) as tools. They remain CLI-only.
- MCP `prompts` primitive. Tools (and optionally resources) only.

## Why these choices

**Stdio transport.** The canonical pattern for "MCP server for a
project on my local disk." Every official MCP server (filesystem, git,
github, sqlite, fetch, memory) ships as a stdio binary that the client
spawns as a subprocess. No port management, no auth surface, no
daemon lifecycle, no zombie JVMs. Session lifetime == subprocess
lifetime. opencode supports it natively via `"type": "local"`.

**Local MCP server, swappable compute backend.** Josh already has
remote-execution infrastructure: `RunRemoteCommand`,
`BatchRemoteCommand --target ...`, `StageToMinioCommand`,
`StageFromMinioCommand`, `PollBatchCommand`, and the whole
`pipeline/remote/` package. Keeping the MCP server local lets us reuse
all of it as an alternative backend behind a single tool surface,
without introducing a file-transport problem (which a remote MCP
server would create).

**Auth via env, never tool args.** API keys in tool input schemas
end up in the LLM's chat history. Reading `JOSH_API_KEY`,
`JOSH_ENDPOINT`, and MinIO creds from the process environment matches
how every other MCP server handles secrets and how Josh already loads
MinIO creds.

## MCP vs REST — orienting notes

For reviewers unfamiliar with MCP:

- MCP is JSON-RPC 2.0 over stdio or HTTP. Stateful session per
  connection.
- Three server primitives: **tools** (actions; what we mainly want),
  **resources** (addressable read-only content), **prompts**
  (parameterized prompt templates).
- A tool is `{ name, description, inputSchema (JSON Schema) }` plus a
  handler returning `CallToolResult { content[], isError }`. The
  description and schema *are* the contract — they replace OpenAPI.
- Two error channels: protocol errors (malformed args, unknown tool)
  surface as JSON-RPC errors; recoverable tool errors return a normal
  `CallToolResult` with `isError: true` so the LLM can react.
- Progress for long-running tools: client sends a `progressToken` with
  the call; server emits `notifications/progress` referencing that
  token. Same API across transports.

## Architecture

```
        ┌────────────────────────────────────┐
        │  opencode (or other MCP client)    │
        │  - spawns subprocess               │
        │  - talks JSON-RPC over stdin/stdout│
        └──────────────┬─────────────────────┘
                       │ stdio
        ┌──────────────▼─────────────────────┐
        │  java -jar joshsim-fat.jar mcp     │
        │                                    │
        │  ┌──────────────────────────────┐  │
        │  │  JoshMcpServer (SDK wiring)  │  │
        │  └──────────────┬───────────────┘  │
        │                 │                  │
        │  ┌──────────────▼───────────────┐  │
        │  │  Tool handlers               │  │
        │  │  (Validate, Preprocess,      │  │
        │  │   Run, DiscoverConfig, ...)  │  │
        │  └──────────────┬───────────────┘  │
        │                 │                  │
        │  ┌──────────────▼───────────────┐  │
        │  │  Backend interface           │  │
        │  │  ├── LocalBackend  ─┐        │  │
        │  │  └── RemoteBackend ─┤        │  │
        │  └─────────────────────┼────────┘  │
        └────────────────────────┼───────────┘
                                 │
              ┌──────────────────┴──────────────────┐
              │                                     │
       in-JVM execution                  Josh Cloud / k8s
       (existing RunCommand /            (existing RunRemoteCommand /
        PreprocessUtil paths)             BatchRemoteCommand paths,
                                          MinIO staging, polling)
```

Files flow in and out of the **local** filesystem in both cases. For
the remote backend, the local MCP process stages inputs to MinIO,
submits the job, streams progress, and downloads outputs back to local
paths before returning. The LLM sees the same contract either way:
"I gave you paths; the outputs are at the paths I asked for."

## Tool surface (v1)

| Tool                 | Wraps                                  | Backends       | Notes                                       |
|----------------------|----------------------------------------|----------------|---------------------------------------------|
| `validate_simulation`| `JoshSimCommander.getJoshProgram`      | local only     | Cheap; structured parse errors.             |
| `preprocess_data`    | `PreprocessUtil.preprocess`            | local + remote | Remote path uses `PreprocessBatchCommand`.  |
| `run_simulation`     | `JoshSimFacadeUtil.runSimulation`      | local + remote | Stream step progress via MCP progress.      |
| `discover_config`    | `DiscoverConfigCommand` internals      | local only     | Read-only.                                  |
| `inspect_exports`    | `InspectExportsCommand` internals      | local only     | Read-only.                                  |
| `inspect_jshd`       | `InspectJshdCommand` internals         | local only     | Read-only.                                  |

Compute-heavy tools take a uniform `execution` arg:

```jsonc
{
  "execution": {
    "type": "object",
    "properties": {
      "backend":           { "enum": ["local", "remote"], "default": "local" },
      "endpoint":          { "type": "string", "description": "Josh Cloud endpoint (remote only). Falls back to $JOSH_ENDPOINT." },
      "concurrentWorkers": { "type": "integer", "default": 4 }
    }
  }
}
```

API keys and MinIO creds are read from the environment, never
accepted as tool args.

## Configuration in opencode

```json
{
  "mcp": {
    "josh": {
      "type": "local",
      "command": ["java", "-jar", "/path/to/joshsim-fat.jar", "mcp"],
      "env": {
        "JOSH_API_KEY":  "${env:JOSH_API_KEY}",
        "JOSH_ENDPOINT": "https://cloud.joshsim.org"
      }
    }
  }
}
```

`mcp` subcommand flags:
- `--default-backend {local,remote}` (default: `local`)
- `--default-endpoint URL` (default: `$JOSH_ENDPOINT` or Josh Cloud)

No `--port`, no `--bind`, no `--root` in v1.

## Forward-compatibility discipline

These four rules are the entire cost of keeping a future
`mcp --http --port N` mode possible without a rewrite. They cost
~nothing to follow from day one and a lot to retrofit.

1. **Log to stderr always.** Stdio requires it; HTTP tolerates
   anything; pick the strict rule. Route `OutputOptions` and SLF4J to
   `System.err`. Audit for stray `System.out.println` (e.g.
   `ServerCommand.java:79,98,106,125-128`).
2. **All paths through a single resolver.** New helper
   `JoshPaths.resolve(String)` — today just
   `Paths.get(arg).toAbsolutePath()`. The seam where root-fencing or
   remote-path translation plugs in later.
3. **Handlers are re-entrant; no static per-session state.** Each
   `tools/call` builds its own `ValueSupportFactory`,
   `InputOutputLayer`, etc. Move `SharedRandom.initialize/clear`
   (`command/RunCommand.java:254-267, 438`) into per-call scope inside
   the run handler.
4. **Cleanup in `finally`, never relying on JVM exit.** Any shutdown
   logic must work whether the JVM lives for one call (stdio) or for
   weeks (future HTTP mode).

## File changes

### Build

- `build.gradle` (around line 38–101 deps block): add
  ```gradle
  implementation("io.modelcontextprotocol.sdk:mcp:<latest>")
  ```
  Verify Java 19 compatibility of the SDK version chosen.

### Commander wiring

- `src/main/java/org/joshsim/JoshSimCommander.java:60-74` — add
  `McpCommand.class` to the `subcommands` array.

### New files

- `src/main/java/org/joshsim/command/McpCommand.java`
  - picocli `@Command(name = "mcp")` `Callable<Integer>`.
  - Flags: `--default-backend`, `--default-endpoint`.
  - Builds `JoshMcpServer`, blocks until stdin EOF.

- `src/main/java/org/joshsim/mcp/JoshMcpServer.java`
  - Constructs `StdioServerTransportProvider`.
  - Builds `McpServer.sync(...)` with `serverInfo("josh", VERSION)`
    and tools capability.
  - Registers every tool from `mcp/tool/`.

- `src/main/java/org/joshsim/mcp/JoshPaths.java`
  - Single static `resolve(String) -> Path` helper. Today does
    `Paths.get(arg).toAbsolutePath().normalize()`. Future seam for
    root-fencing.

- `src/main/java/org/joshsim/mcp/StderrOutputOptions.java`
  - `OutputOptions` subclass routing `printInfo` / `printError` to
    `System.err`. Used by every tool handler.

- `src/main/java/org/joshsim/mcp/Backend.java`
  - Interface with `runSimulation(...)`, `preprocess(...)`,
    `validate(...)`. Implementations:
    - `LocalBackend` — delegates to `JoshSimFacadeUtil.runSimulation`,
      `PreprocessUtil.preprocess`, `JoshSimCommander.getJoshProgram`.
    - `RemoteBackend` — delegates to logic extracted from
      `RunRemoteCommand`, `PreprocessBatchCommand`, including MinIO
      staging via `StageToMinioCommand` / `StageFromMinioCommand`
      internals and progress streaming via `RemoteResponseHandler`.

- `src/main/java/org/joshsim/mcp/tool/`
  - `ValidateTool.java`
  - `PreprocessTool.java`
  - `RunSimulationTool.java`
  - `DiscoverConfigTool.java`
  - `InspectExportsTool.java`
  - `InspectJshdTool.java`
  - Each defines:
    1. `Tool` (name, description, inputSchema). Descriptions are
       written for an LLM reader — explicit about Josh semantics
       (`.josh`, `.jshd`, `.jshc`, simulation names, units).
    2. A `callHandler` that parses args, resolves paths via
       `JoshPaths`, dispatches to the chosen `Backend`, emits MCP
       progress notifications when a `progressToken` is supplied,
       wraps results into `CallToolResult` (using `isError(true)` for
       recoverable failures).

### Surgical refactors

Existing CLI commands have execution logic intertwined with picocli
field state. Extract callable pipeline classes so both the CLI
command and the MCP backend can drive them without duplication:

- From `command/RunCommand.java` — extract `RunPipeline` that takes
  an immutable run config (file, simulation, replicates, dataFiles,
  customTags, outputSteps, etc.), a step callback, and an
  `OutputOptions`. `RunCommand.call()` becomes a 20-line shim that
  builds the config from its picocli fields and invokes the pipeline.
- From `command/RunRemoteCommand.java` — same treatment:
  `RemoteRunPipeline`.
- From `command/PreprocessBatchCommand.java` — same:
  `RemotePreprocessPipeline`.

No behavior change. This is the largest piece of mechanical work in
the plan and the prerequisite for clean tool handlers.

## Critical implementation details

- **Stdout discipline.** Single biggest stdio footgun. Before
  shipping, grep for `System.out.print` across `src/main/java/org/joshsim/`
  and route everything reachable from the `mcp` subcommand to stderr.
  SLF4J jdk14 binding (`slf4j-jdk14` in `build.gradle`) defaults to
  stderr but verify with a smoke test.
- **Don't inline large outputs.** Simulation CSVs can be huge. Tool
  results return the *paths* and a short summary (rows × columns,
  file size, step range). The LLM can ask follow-up tools to inspect
  specific files.
- **Schema quality is product.** The `inputSchema` JSON Schemas and
  the natural-language `description` fields are the documentation
  the LLM reads to decide whether and how to call a tool. Spend real
  time on them. Mention units, file extensions, what "simulation
  name" means in Josh.
- **Errors:**
  - Missing file, parse error, unknown simulation name, malformed
    units → `CallToolResult.isError(true)` with the human-readable
    message. The LLM can recover.
  - Genuinely unexpected exceptions → let them propagate; the SDK
    maps them to JSON-RPC errors.
- **Progress.** `RunCommand.java:490-495` already has the step
  callback hook. Plumb it into MCP progress notifications when the
  client supplied a `progressToken`. For the remote backend, drive
  progress from `RemoteResponseHandler`'s streaming responses.
- **Concurrency.** Sync MCP server serializes calls per connection.
  Josh sims are CPU-heavy; serializing one connection's calls is
  desirable. Don't add threading inside the handler.

## Testing

- **Unit:** one test per tool exercising the handler with mock
  arguments, verifying the schema, success path, and at least one
  `isError(true)` failure mode.
- **Integration:** spawn `java -jar joshsim-fat.jar mcp` as a
  subprocess from a test, drive it with the official MCP Inspector
  (`npx @modelcontextprotocol/inspector`) or a minimal JSON-RPC test
  client. Validate `initialize`, `tools/list`, and one `tools/call`
  for each tool against a known-good example simulation from
  `examples/`.
- **Stdout audit:** integration test that captures the subprocess's
  stdout, runs a full validate+preprocess+run flow, and asserts the
  stdout stream contains only well-formed JSON-RPC messages (no
  stray log lines).
- **Conformance examples:** wire the existing `examples/` simulations
  into the integration test matrix as smoke fixtures.

## Phased delivery

1. **Phase 1 — MVP, local backend only.**
   - `mcp` subcommand, stdio transport.
   - Refactor: `RunPipeline` extraction from `RunCommand`.
   - Tools: `validate_simulation`, `preprocess_data`,
     `run_simulation`, `discover_config`.
   - Tests: per-tool unit + integration smoke + stdout audit.
   - opencode connectivity demo.

2. **Phase 2 — remote backend.**
   - Refactor: `RemoteRunPipeline` from `RunRemoteCommand`,
     `RemotePreprocessPipeline` from `PreprocessBatchCommand`.
   - `RemoteBackend` implementation.
   - `execution` arg on `preprocess_data` and `run_simulation`.
   - Remote progress streaming via `RemoteResponseHandler`.
   - Tests: integration against a mock cloud endpoint.

3. **Phase 3 — read-only tools & resources.**
   - Tools: `inspect_exports`, `inspect_jshd`.
   - MCP `resources` capability: expose `.jshd` / `.jshc` files in
     CWD and a static language-spec resource pointing at
     `LanguageSpecification.md`.

4. **Phase 4 (optional, only if needed) — HTTP transport.**
   - Add `mcp --http --port N --bind addr` flag.
   - Reuse Undertow (already in deps) for the transport.
   - Add `--token` / `--root` flags for auth and sandboxing — both
     become relevant the moment the server stops being a child
     process. Path resolver swaps in the root-fencing implementation
     at the `JoshPaths.resolve` seam.

## References

- MCP Java SDK server docs: https://java.sdk.modelcontextprotocol.io/latest/server/
- MCP Java SDK GitHub: https://github.com/modelcontextprotocol/java-sdk
- MCP tools specification: https://modelcontextprotocol.io/specification/2025-06-18/server/tools
- MCP architecture overview: https://modelcontextprotocol.io/docs/learn/architecture
- Official server examples: https://github.com/modelcontextprotocol/servers
- opencode MCP server config: https://opencode.ai/docs/mcp-servers/
- MCP Inspector (testing tool): `npx @modelcontextprotocol/inspector`
