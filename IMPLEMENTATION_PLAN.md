# Josh MCP Server — Implementation Plan

## Goal

Expose Josh as a Model Context Protocol (MCP) server so that LLM
clients can invoke Josh commands — primarily `preprocess` and `run` —
through a standardized, self-describing tool interface.

Two deployment modes are in scope long-term:

1. **Local mode (v1 focus).** MCP server runs as a stdio subprocess
   under the user's MCP client (e.g. opencode). Tools take paths to
   files on the user's local disk. Compute may run in-JVM locally or
   dispatch to Josh Cloud / k8s via existing remote-execution
   infrastructure — in both cases the user's filesystem is the I/O
   substrate.
2. **Hosted mode (future).** MCP server runs on Josh-operated
   infrastructure, reachable via HTTP from clients like claude.ai web
   that have no local Josh install and no shared filesystem with the
   server. Tools take MCP resource URIs (or, for small inputs, inline
   content) instead of paths. Compute runs in Josh's own cluster.

The two modes share the compute and pipeline layers but necessarily
diverge at the tool I/O layer (paths vs resources). See "Deployment
modes" below for the design that enables both without rework.

## Non-goals (for v1)

- Building hosted mode itself. v1 ships local mode only; the file
  layout and abstractions are chosen so hosted mode is additive, not
  a rewrite.
- HTTP / Streamable HTTP transport. Stdio only in v1.
- Path sandboxing / root allow-lists. CWD is the implicit sandbox in
  local mode, same as every other canonical MCP server (filesystem,
  git, sqlite, etc.). Sandboxing becomes relevant for hosted mode.
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

## Deployment modes

The local-vs-hosted distinction is not about transport or compute;
it is about the **I/O channel** between the LLM and the tool.

### Local mode (v1)

```
opencode  ──stdio──▶  java -jar joshsim-fat.jar mcp
                       │
                       ├─ tools take string paths
                       ├─ paths resolve against CWD
                       │  (user's project directory)
                       │
                       ▼
                     Backend (Local or Remote)
                       │
                  ┌────┴────┐
                  ▼         ▼
              in-JVM    Josh Cloud / k8s
                        (files staged to MinIO,
                         then re-downloaded locally)
```

I/O substrate is the user's filesystem. Both compute backends preserve
that contract; remote compute does its own MinIO staging transparently
and writes results back to the local disk before the tool returns.

### Hosted mode (future)

```
claude.ai web  ──HTTPS/MCP──▶  hosted joshsim mcp server
                                │
                                ├─ tools take MCP resource URIs
                                │  (or inline content for small files)
                                ├─ server materializes resources to
                                │  its own temp filesystem
                                │
                                ▼
                              Backend (Remote only, typically)
                                │
                                ▼
                            Josh Cloud / k8s
                                │
                                ▼
                         outputs re-published as
                         MCP resources / URIs the
                         client can fetch
```

There is no user filesystem visible to the server. Resources are the
only viable file abstraction. Auth via OAuth 2.1 or bearer token — the
user's Josh API key becomes their MCP auth credential, and the server
uses its own k8s creds internally.

### What's shared, what diverges

| Layer                                    | Local mode      | Hosted mode     | Shared? |
|------------------------------------------|-----------------|-----------------|---------|
| Transport                                | stdio           | Streamable HTTP | No      |
| Auth                                     | none (subprocess) | OAuth / bearer | No      |
| Tool input schemas                       | path strings    | resource URIs / inline content | **No** |
| Tool handler I/O wrapping                | resolve path    | materialize resource → temp file | **No** |
| `Backend` interface + impls              | identical       | identical       | **Yes** |
| `RunPipeline` / `RemoteRunPipeline` etc. | identical       | identical       | **Yes** |
| Josh facade calls                        | identical       | identical       | **Yes** |
| Progress emission                        | identical       | identical       | **Yes** |

The honest answer: schemas and the thin "args → files → args" shim
diverge per mode and shouldn't be unified (a `path | uri | content`
union would degrade both tool surfaces). Everything below that line
is shared verbatim.

### How v1 enables hosted mode

- Tool classes live under `mcp/tool/local/` (not just `mcp/tool/`),
  signalling that a sibling `mcp/tool/hosted/` package will appear
  later. No content there in v1.
- The `Backend` interface takes `Path`-typed arguments, not strings.
  Local-mode tools resolve their string inputs to `Path` via
  `JoshPaths.resolve`. Hosted-mode tools, later, will materialize
  resources to a server-owned temp directory and pass those `Path`s in.
  Backends are oblivious to the distinction.
- Refactors that extract `RunPipeline` / `RemoteRunPipeline` /
  `RemotePreprocessPipeline` from the existing CLI command classes are
  done in v1 and reused unchanged by hosted mode.

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
    `validate(...)`. All file arguments typed as `java.nio.file.Path`,
    not `String` — keeps the interface oblivious to whether the path
    came from a user-supplied string (local mode) or a materialized
    MCP resource (future hosted mode). Implementations:
    - `LocalBackend` — delegates to `JoshSimFacadeUtil.runSimulation`,
      `PreprocessUtil.preprocess`, `JoshSimCommander.getJoshProgram`.
    - `RemoteBackend` — delegates to logic extracted from
      `RunRemoteCommand`, `PreprocessBatchCommand`, including MinIO
      staging via `StageToMinioCommand` / `StageFromMinioCommand`
      internals and progress streaming via `RemoteResponseHandler`.

- `src/main/java/org/joshsim/mcp/tool/local/`  *(path-based tools; v1)*
  - `ValidateTool.java`
  - `PreprocessTool.java`
  - `RunSimulationTool.java`
  - `DiscoverConfigTool.java`
  - `InspectExportsTool.java`
  - `InspectJshdTool.java`
  - Each defines:
    1. `Tool` (name, description, inputSchema). Input schemas take
       string paths. Descriptions are written for an LLM reader —
       explicit about Josh semantics (`.josh`, `.jshd`, `.jshc`,
       simulation names, units).
    2. A `callHandler` that parses args, resolves paths via
       `JoshPaths`, dispatches to the chosen `Backend`, emits MCP
       progress notifications when a `progressToken` is supplied,
       wraps results into `CallToolResult` (using `isError(true)` for
       recoverable failures).

  *(Sibling `mcp/tool/hosted/` package added later when hosted mode
  ships. Same tool names, different input schemas — resource URIs
  and inline content blocks instead of paths — and a thin shim that
  materializes inputs to temp files and re-publishes outputs as
  resources. The `Backend` is reused unchanged.)*

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

### Local mode

1. **Phase 1 — MVP, local backend only.**
   - `mcp` subcommand, stdio transport.
   - Refactor: `RunPipeline` extraction from `RunCommand`.
   - Tools under `mcp/tool/local/`: `validate_simulation`,
     `preprocess_data`, `run_simulation`, `discover_config`.
   - Tests: per-tool unit + integration smoke + stdout audit.
   - opencode connectivity demo.

2. **Phase 2 — remote compute backend.**
   - Refactor: `RemoteRunPipeline` from `RunRemoteCommand`,
     `RemotePreprocessPipeline` from `PreprocessBatchCommand`.
   - `RemoteBackend` implementation.
   - `execution` arg on `preprocess_data` and `run_simulation` lets
     the LLM (or `--default-backend`) choose local vs remote
     compute. Inputs and outputs still flow through the user's
     local filesystem; remote staging via MinIO is invisible to the
     LLM.
   - Remote progress streaming via `RemoteResponseHandler`.
   - Tests: integration against a mock cloud endpoint.

3. **Phase 3 — read-only tools & resources.**
   - Tools: `inspect_exports`, `inspect_jshd`.
   - MCP `resources` capability: expose `.jshd` / `.jshc` files in
     CWD and a static language-spec resource pointing at
     `LanguageSpecification.md`.

### Hosted mode

4. **Phase 4 — HTTP transport.**
   - Add `mcp --http --port N --bind addr` flag.
   - Reuse Undertow (already in deps) for the transport. The Java
     MCP SDK ships an HTTP transport provider; swap it in at the
     `McpServer.sync(...)` call site, otherwise no changes to tool
     code.
   - Add `--token` / `--root` flags for auth and sandboxing — both
     become relevant the moment the server stops being a child
     process. Path resolver swaps in the root-fencing implementation
     at the `JoshPaths.resolve` seam.
   - Audit handler re-entrancy under multi-client load. Verify
     per-call state lifting from Phase 1 (esp. `SharedRandom`)
     holds up.

5. **Phase 5 — hosted tool surface.**
   - New `mcp/tool/hosted/` package mirroring `mcp/tool/local/`,
     same tool names, different input schemas:
     - File inputs become `{ "anyOf": [ { "resourceUri": ... },
       { "content": ... } ] }`.
     - File outputs are returned as MCP resource URIs the client
       can fetch on demand.
   - Per-session temp directory on the server; resources
     materialize there before `Backend` is called; cleaned up in
     `finally`.
   - Compute is `RemoteBackend` by default (the hosted server is
     co-located with the cluster); `LocalBackend` available for
     small inline runs if appetite exists.
   - Auth: OAuth 2.1 per the current MCP spec direction, falling
     back to bearer tokens. The user's Josh API key is their MCP
     credential; the hosted server uses its own k8s creds
     internally to dispatch jobs.
   - A tool-selection layer ensures clients see only the tool
     package appropriate to the deployment mode (clients should
     never see both `local` and `hosted` variants of the same
     tool simultaneously).

6. **Phase 6 — production hosted deployment.**
   - Containerize, deploy behind a load balancer in Josh's infra.
   - Rate limiting, request-scoped logging, metric collection per
     MCP server best practices.
   - claude.ai web connectivity demo.

## References

- MCP Java SDK server docs: https://java.sdk.modelcontextprotocol.io/latest/server/
- MCP Java SDK GitHub: https://github.com/modelcontextprotocol/java-sdk
- MCP tools specification: https://modelcontextprotocol.io/specification/2025-06-18/server/tools
- MCP architecture overview: https://modelcontextprotocol.io/docs/learn/architecture
- Official server examples: https://github.com/modelcontextprotocol/servers

---

## Implementation Details

### What was built (Phase 1 complete)

**Core files created:**
- `src/main/java/org/joshsim/mcp/Backend.java` — Interface with four methods and inner result POJOs (`ValidateResult`, `DiscoverConfigResult`, `PreprocessResult`, `RunSimulationResult`) plus `PreprocessOptions`.
- `src/main/java/org/joshsim/mcp/LocalBackend.java` — In-JVM implementation delegating to `JoshSimCommander.getJoshProgram`, `JoshConfigDiscoveryVisitor`, `PreprocessUtil.preprocess`, and `JoshSimFacadeUtil.runSimulation`.
- `src/main/java/org/joshsim/mcp/JoshMcpServer.java` — Wires the MCP SDK, loads `McpJsonMapper` via `ServiceLoader<McpJsonMapperSupplier>`, registers all four tools.
- `src/main/java/org/joshsim/mcp/JoshPaths.java` — Path resolution utility (absolute + normalized).
- `src/main/java/org/joshsim/mcp/StderrOutputOptions.java` — Routes all diagnostic output to `System.err`, keeping `System.out` clean for JSON-RPC.
- `src/main/java/org/joshsim/mcp/tool/local/ValidateTool.java` — `validate_simulation` MCP tool.
- `src/main/java/org/joshsim/mcp/tool/local/DiscoverConfigTool.java` — `discover_config` MCP tool.
- `src/main/java/org/joshsim/mcp/tool/local/PreprocessTool.java` — `preprocess_data` MCP tool.
- `src/main/java/org/joshsim/mcp/tool/local/RunSimulationTool.java` — `run_simulation` MCP tool.
- `src/main/java/org/joshsim/command/McpCommand.java` — Picocli `mcp` subcommand.

**Modified files:**
- `build.gradle` — Added `mavenLocal()` repository and `io.modelcontextprotocol.sdk:mcp:1.1.3` dependency.
- `src/main/java/org/joshsim/JoshSimCommander.java` — Added `McpCommand.class` to subcommands array.

**Dependency workaround:**
- `org.geotools:gt-*:33.0` and `edu.ucar:cdm-core:5.7.0` / `netcdf4:5.7.0` are unavailable from their repos (403 errors). Stub JARs are placed in `~/.m2/repository/` via `mavenLocal()` so compilation succeeds without the real libraries. The stubs provide only the method signatures needed by the source and test files; they return null/empty at runtime. The real JARs would need to be sourced from a private mirror or the team's artifact cache for production use.

### Deviations from plan

- MCP SDK actual API (1.1.3) differs from what the plan described:
  - `StdioServerTransportProvider(McpJsonMapper)` not `(ObjectMapper)`.
  - `Tool.builder().inputSchema(McpJsonMapper, String)` not `.inputSchema(String)`.
  - `McpJsonMapper` loaded via `ServiceLoader<McpJsonMapperSupplier>`, not constructed directly.
  - `CallToolResult.Builder.isError(Boolean)` takes boxed `Boolean`.
- `LocalBackend.runSimulation` uses `JoshSimCommander.getJoshProgram(ValueSupportFactory, EngineGeometryFactory, File, OutputOptions, InputOutputLayer)` overload (5 args) rather than the 3-arg version used in `validate()`, to match what `RunCommand` actually does.

### Test coverage

22 tests across 3 test classes, all passing:
- `JoshPathsTest` (6 tests) — path resolution, absolute/relative, normalization.
- `LocalBackendTest` (11 tests) — validate, discoverConfig, result POJO fields, StderrOutputOptions.
- `McpStdoutCleanlinessTest` (5 tests) — verifies no writes to `System.out` during backend operations.

### Known limitations

- The stub JARs in `~/.m2/repository/` are machine-local; CI/CD will need the real jars or a private mirror.
- `runSimulation` is not tested end-to-end (would require a real simulation run); unit tests cover the result POJOs and backend wiring only.
- The MCP server sends several `notifications/tools/list_changed` events on startup (one per `addTool` call); this is an SDK behavior and not harmful.
- opencode MCP server config: https://opencode.ai/docs/mcp-servers/
- MCP Inspector (testing tool): `npx @modelcontextprotocol/inspector`
