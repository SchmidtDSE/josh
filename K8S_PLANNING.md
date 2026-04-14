# Plan: Batch Remote Execution for Josh

## Context

Cloud Run has hard ceilings (32 GiB memory, 60-min timeout, 8 vCPUs) that block HPC-scale simulations. The existing `runRemote` uses HTTP streaming — code and data travel in the request body, results stream back via wire format. This breaks when .jshd files are large. We're adding a parallel execution path (`batchRemote`) that uses MinIO as the staging layer instead of HTTP transport.

**Key design decisions:**
- **Staging and execution are separate concerns.** `stageToMinio` / `stageFromMinio` handle I/O. `run`, `preprocess`, `validate` work on local files. The dispatch mechanism (HTTP, K8s, SSH) just says "go."
- **`runRemote` is untouched.** It's the production Josh Cloud path. `batchRemote` is a parallel path, not a replacement.
- **Results go directly to MinIO** via `minio://` export paths in the Josh script. No wire format, no streaming results back to the client.
- **Composable target system.** Users register compute targets as JSON profiles (`~/.josh/targets/<name>.json`). One command (`batchRemote --target=<name>`) works with any backend.

## Workflow

PRs 1–4 built into `feat/enhanced_remote`. Rollup PR #383 merged `feat/enhanced_remote` → `dev`. PRs 4a and 4b target `dev` directly. Subsequent PRs (5+) will also target `dev`.

For each PR: branch off → implement → open PR → review → merge → demonstrate with real model run.

## Architecture

### Execution flow (same for all targets)
```
1. STAGE:    stageToMinio --input-dir=./sim/ --prefix=batch-jobs/<uuid>/inputs/
2. DISPATCH: tell target to run (HTTP POST / K8s Job / SSH — varies by type)
3. EXECUTE:  worker does stageFromMinio → cd workdir → run (identical everywhere)
4. RESULTS:  land in MinIO via minio:// export paths in the Josh script
```

### `RemoteBatchTarget` interface
```
RemoteBatchTarget (interface)
  +-- HttpBatchTarget       (POST /runBatch — Cloud Run, self-hosted, anything running joshsim server)
  +-- KubernetesTarget      (submit K8s indexed Job — GKE, EKS, Nautilus, self-hosted)
  +-- SshTarget             (future)
```

Each implementation answers one question: "how do I tell this compute resource to run `stageFromMinio && run`?"

### User experience
```bash
joshsim batchRemote simulation.josh Main --target=cloudrun-prod --replicates=10
joshsim batchRemote simulation.josh Main --target=nautilus --replicates=50
```

### Target profiles (`~/.josh/targets/<name>.json`)

**HTTP** (Cloud Run, self-hosted server):
```json
{
  "type": "http",
  "http": { "endpoint": "https://josh-executor-prod-....run.app/runBatch", "apiKey": "..." },
  "minio_endpoint": "https://storage.googleapis.com",
  "minio_access_key": "...", "minio_secret_key": "...", "minio_bucket": "josh-storage"
}
```

**Kubernetes** (GKE, EKS, Nautilus):
```json
{
  "type": "kubernetes",
  "kubernetes": {
    "context": "nautilus", "namespace": "joshsim-lab",
    "image": "ghcr.io/schmidtdse/joshsim-job:latest",
    "resources": { "requests": { "cpu": "2", "memory": "4Gi" }, "limits": { "memory": "256Gi" } },
    "parallelism": 10, "timeoutSeconds": 3600
  },
  "minio_endpoint": "...", "minio_access_key": "...", "minio_secret_key": "...", "minio_bucket": "..."
}
```

### Contrast with `runRemote` (existing, untouched)

`runRemote` streams code+data in the HTTP body and parses wire-format results. Works for small jobs. `batchRemote` decouples transport from execution via MinIO staging. Both coexist.

### Fabric8 K8s client (Apache 2.0)
Used for `KubernetesTarget` only. Cleaner fluent DSL than official `io.kubernetes:client-java`, smaller dep footprint (~15 vs ~25 transitives). All K8s calls isolated in one file (~250 lines) — swappable if needed.

---

## PR Plan

```
PR1 ✅ → PR2 ✅ → PR3 ✅ (cleanup) → PR4 ✅ (/runBatch) → PR4a ✅ (stageFromMinio opt-in) → PR4b ✅ (async+status) → PR5 ✅ (profiles+polling) → PR6 ✅ (batchRemote+HttpTarget) → PR7 ✅ (Fabric8+K8sTarget+K8sPolling) → PR8 (Dockerfile) → PR9 (preprocessBatch)
```

### Regression gates (every PR)
- `./gradlew test` passes
- `./gradlew checkstyleMain` passes
- `./gradlew fatJar` builds
- Existing `runRemote` HTTP behavior unchanged

---

### PR 1 ✅: MinIO handler download/list/delete — #375
Added `downloadFile()`, `downloadStream()`, `listObjects()`, `deleteObjects()` to `MinioHandler`. 7 new unit tests.

### PR 2 ✅: stageToMinio + stageFromMinio — #379
Symmetric MinIO staging commands. Deleted `RunFromMinioCommand` (from #376, superseded).

```bash
joshsim stageToMinio   --input-dir=./sim/     --prefix=batch-jobs/abc/inputs/ [MinioOptions]
joshsim stageFromMinio --output-dir=/tmp/work/ --prefix=batch-jobs/abc/inputs/ [MinioOptions]
```

Integration tested: full round-trip against GCS — stage up, stage down, diff matches, run simulation, results in MinIO.

Design note: originally built `runFromMinio` and `preprocessFromMinio` as per-command wrappers, realized this conflated I/O with execution (N commands × M backends = combinatorial growth). Extracted staging as orthogonal commands instead. See #374 comments for full rationale.

---

### PR 3 ✅: Remove vestigial `--upload-*` flags
**Branch: `feat/preprocess-minio-cleanup`**

**Removed:**
- `RunCommand.java` — removed `--upload-source`, `--upload-config`, `--upload-data` flags + `uploadArtifacts()` + `saveToMinio()`
- `ValidateCommand.java` — removed `--upload-source` flag + `saveToMinio()` + `MinioOptions` mixin (only used for uploads)
- `RunRemoteCommand.java` — removed `--upload-source`, `--upload-config`, `--upload-data` flags + `uploadArtifacts()`
- `JoshSimCommander.java` — removed `saveToMinio()` static method (no remaining callers)
- Deleted `RunCommandArtifactUploadTest.java` and `RunRemoteCommandArtifactUploadTest.java`
- Updated `README.md` and `llms-full.txt` to remove upload flag documentation

**Rationale:** joshpy bottling handles reproducibility. `stageToMinio` handles explicit uploads. The `--upload-*` flags are vestigial.

**Note:** Adding `MinioOptions` mixin to `PreprocessCommand` was originally planned here but deferred to PR 9. Just adding the mixin doesn't wire anything — preprocess writes .jshd to a local `FileOutputStream`, not to MinIO. The proper solution is a full `preprocessBatch` path that reuses the target profile system. See PR 9.

**Risk: MEDIUM (breaking change — users must switch to `stageToMinio`)**

---

### PR 4 ✅: Add `/runBatch` endpoint to JoshSimServer
**Branch: `feat/server-run-batch`**

**New files:**
- `cloud/JoshSimBatchHandler.java` (~180 lines) — thin Undertow `HttpHandler` for `/runBatch`
- `cloud/LocalFileUtil.java` (~70 lines) — shared file discovery utilities

**Modify:**
- `cloud/JoshSimServer.java` — registered `/runBatch` path (1 line)

**Design principle:** Staging and execution are separate concerns. The handler assumes files are already local — the caller (K8s entrypoint, CLI script, etc.) stages first via `stageFromMinio`.

**Handler flow:**
```
POST /runBatch
Form fields: apiKey, jobId, simulation, workDir
Optional:    stageFromMinio (boolean), minioPrefix (required if staging)
```
1. Validate API key via `ApiKeyUtil.checkApiKey()`
2. Extract required form fields (`jobId`, `simulation`, `workDir`)
3. Validate `workDir` exists and is a directory (400 if not)
4. Set `JvmCompatibilityLayer` for thread-safe export queue services
5. Find `.josh` script in `workDir` via `LocalFileUtil.findScriptFile()`
6. Parse Josh program via `JoshSimFacadeUtil.parse()` + `interpret()`
7. Build `InputOutputLayer` with `JvmMappedInputGetter` (maps filenames to workDir paths) + `MinioOptions` from env vars (enables `minio://` exports)
8. Run simulation via `JoshSimFacadeUtil.runSimulation()` with parallel patches
9. Return `{"status":"complete","jobId":"..."}`

**Key differences from `/runReplicate`:**
- No wire format streaming — results go to MinIO, not back over HTTP
- No `SandboxInputOutputLayer` / virtual files — uses real local files
- No code in request body — code is in `workDir`
- No staging — caller handles that separately
- Response is JSON status, not streamed data

**Note on `CompatibilityLayerKeeper`:** The handler must call `CompatibilityLayerKeeper.set(new JvmCompatibilityLayer())` before running. Without it, the fallback `EmulatedCompatibilityLayer` (WASM shim) makes export queue writes synchronous, causing concurrent CSV header corruption with parallel patches.

**Test:**
- [x] 11 unit tests for `JoshSimBatchHandler` (HTTP validation, API key, missing fields, workDir validation, error responses)
- [x] Existing `/runReplicate` and `/runReplicates` unchanged

**Role in target system:** This is the server-side handler for `HttpBatchTarget` (PR 6). Any machine running `joshsim server` gets `/runBatch`. Users register it as an HTTP target profile.

**Risk: LOW — additive endpoint, existing handlers untouched**

---

### PR 4a ✅: Opt-in stageFromMinio for serverless `/runBatch` — #384
**Branch: `feat/server-run-batch` (merged into `feat/enhanced_remote`)**

Added opt-in `stageFromMinio` form field to `/runBatch` so serverless environments (Cloud Run, Lambda) can stage and execute atomically within a single request instead of requiring a separate staging step.

**Changes:**
- `cloud/JoshSimBatchHandler.java` — added `stageFromMinio` (boolean) and `minioPrefix` form fields, auto-creates `workDir` if staging
- `util/MinioStagingUtil.java` (~70 lines) — extracted shared staging logic from `StageFromMinioCommand` so both the CLI command and server handler can reuse it
- `command/StageFromMinioCommand.java` — refactored to delegate to `MinioStagingUtil`

**Design note:** This keeps staging and execution as separate concerns at the abstraction level — `MinioStagingUtil` is a reusable utility, not handler-specific logic. The opt-in flag is for environments where the network topology makes a separate staging step impractical (e.g., Cloud Run containers that boot, execute, and die).

**Risk: LOW — additive field, existing behavior unchanged when field omitted**

---

### PR 4b ✅: Make `/runBatch` async with MinIO status tracking — #387
**Branch: `feat/async-run-batch`**

Changed `/runBatch` from synchronous (blocks until simulation completes) to asynchronous (returns 202 immediately, writes status to MinIO). This enables concurrent batch jobs against serverless workers.

**Changes:**
- `cloud/JoshSimBatchHandler.java` — simulation runs in `CompletableFuture.runAsync()` on a `BATCH_EXECUTOR` thread pool; handler returns 202 with `statusPath` immediately; `runBatchWithStatus()` writes `running`/`complete`/`error` lifecycle to MinIO
- `util/MinioHandler.java` — added `putBytes(byte[], String, String)` for writing small JSON payloads directly to MinIO
- `llms-full.txt` — updated `/runBatch` docs: 202 response, status file lifecycle, polling workflow

**Status file lifecycle (`batch-status/<jobId>/status.json`):**
- `{"status":"running","jobId":"...","startedAt":"..."}`
- `{"status":"complete","jobId":"...","completedAt":"..."}`
- `{"status":"error","jobId":"...","message":"...","failedAt":"..."}`

Status writes are best-effort — missing MinIO credentials don't prevent simulation execution.

**Client workflow:**
1. POST `/runBatch` → 202 with `statusPath`
2. Poll `status.json` in MinIO until `complete` or `error`
3. Results land via `minio://` export paths in the Josh script

**Test:**
- [x] 11+1 unit tests for `JoshSimBatchHandler` (existing validation tests + new 202 acceptance test)
- [x] 3 unit tests for `MinioHandler.putBytes()`
- [x] `./gradlew test` passes, `./gradlew checkstyleMain` passes

**Risk: LOW — same endpoint, additive behavior (async vs sync), validation errors still synchronous**

---

### PR 5 ✅: Target profile system + polling strategy interfaces — #393
**Branch: `feat/target-profiles`**

Target profiles, dispatch interface, and polling strategy. No new dependencies — Fabric8 deferred to PR 7 where it's consumed. All new files, nothing modified except tests.

**Package: `org.joshsim.pipeline.target`** (alongside existing `pipeline/remote/`)

**New files:**

Interfaces:
- `RemoteBatchTarget.java` — dispatch interface, one method: `dispatch(String jobId, String minioPrefix, String simulation)`
- `BatchPollingStrategy.java` — status polling interface: `poll(String jobId)` returns `JobStatus`
- `JobStatus.java` — polling result: status enum (`PENDING`, `RUNNING`, `COMPLETE`, `ERROR`) + optional message + optional timestamp

Profile loading:
- `TargetProfile.java` — parsed JSON profile. Fields: `type` (discriminator), `httpConfig`, `kubernetesConfig`, MinIO creds (`minioEndpoint`, `minioAccessKey`, `minioSecretKey`, `minioBucket`)
- `HttpTargetConfig.java` — `endpoint`, `apiKey`
- `KubernetesTargetConfig.java` — `context`, `namespace`, `image`, `resources` (map), `parallelism`, `timeoutSeconds`
- `TargetProfileLoader.java` — reads `~/.josh/targets/<name>.json`, returns `TargetProfile`. Uses Jackson `ObjectMapper` (already a transitive dep via MinIO SDK)

Polling:
- `MinioPollingStrategy.java` — implements `BatchPollingStrategy`. Reads `batch-status/<jobId>/status.json` from MinIO. This is the default strategy — works for all target types. Later, `KubernetesPollingStrategy` (PR 7) can check K8s Job status API for infrastructure-level failures (OOMKill, scheduling failures, image pull errors) that never reach the status file.

**Design decisions:**
- **No `HierarchyConfig` reuse.** Target profiles are loaded from a specific JSON file, not from CLI/env/config hierarchy. The profile IS the config. Different targets = different profiles.
- **Fabric8 deferred.** `KubernetesTargetConfig` is just a data class — stores config values, doesn't call K8s APIs. Fabric8 only needed in PR 7 when `KubernetesTarget.dispatch()` actually creates K8s Jobs.
- **Polling is composable.** `BatchPollingStrategy` is a separate concern from dispatch. HTTP and K8s targets both default to `MinioPollingStrategy`. PR 7 adds `KubernetesPollingStrategy` for richer error info (pod OOMKill, scheduling failures — important for HPC simulations with emergent memory behavior).
- **Target profiles hold MinIO creds.** Each profile is self-contained. No env var fallback — if you want different creds, make a different profile.

**Target profile JSON format (`~/.josh/targets/<name>.json`):**

HTTP:
```json
{
  "type": "http",
  "http": { "endpoint": "https://josh-executor-prod-....run.app", "apiKey": "..." },
  "minio_endpoint": "https://storage.googleapis.com",
  "minio_access_key": "...", "minio_secret_key": "...", "minio_bucket": "josh-storage"
}
```

Kubernetes:
```json
{
  "type": "kubernetes",
  "kubernetes": {
    "context": "nautilus", "namespace": "joshsim-lab",
    "image": "ghcr.io/schmidtdse/joshsim-job:latest",
    "resources": { "requests": { "cpu": "2", "memory": "4Gi" }, "limits": { "memory": "256Gi" } },
    "parallelism": 10, "timeoutSeconds": 3600
  },
  "minio_endpoint": "...", "minio_access_key": "...", "minio_secret_key": "...", "minio_bucket": "..."
}
```

**Test:**
- [ ] `TargetProfileLoader` — loads HTTP profile, loads K8s profile, handles missing file, handles malformed JSON
- [ ] `MinioPollingStrategy` — parses running/complete/error status, handles missing status file, handles null MinIO handler
- [ ] `JobStatus` — enum values, message extraction
- [ ] `./gradlew test` passes, `./gradlew checkstyleMain` passes

**Risk: LOW — all new files, no new dependencies, no modifications to existing code**

---

### PR 6 ✅: `batchRemote` command + HttpBatchTarget + BatchJobStrategy — #394
**Branch: `feat/batch-remote`**

The client-side command that ties everything together. `BatchRemoteCommand` is the picocli entry point, `BatchJobStrategy` orchestrates stage → dispatch → poll, and `HttpBatchTarget` is the first `RemoteBatchTarget` implementation (POST to `/runBatch`). No new dependencies — uses `java.net.http.HttpClient` (already used in `RunRemoteOffloadLeaderStrategy`).

**Key design principle: replicates are the target's responsibility.**
The CLI passes `--replicates=N` through to `RemoteBatchTarget.dispatch()`. How those replicates actually run depends on the target:
- `HttpBatchTarget` → passes `replicates` to `/runBatch`, which runs N replicates in-process (same as local `run --replicates=N` — JIT warmup, one container)
- `KubernetesTarget` (PR 7) → creates an indexed Job with N pod completions (K8s handles parallelism)
- Future custom targets → split however makes sense (SLURM `--array`, SSH round-robin, etc.)

The caller (joshpy, scripts) can also handle parallelism itself by calling `batchRemote` multiple times with different jobIds. This is bring-your-own-infrastructure — the target defines how it runs, the caller defines how many times it calls.

**Interface change from PR 5:**
```java
// PR 5 (current)
void dispatch(String jobId, String minioPrefix, String simulation) throws Exception;

// PR 6 (updated)
void dispatch(String jobId, String minioPrefix, String simulation, int replicates) throws Exception;
```

**New files:**

`pipeline/target/HttpBatchTarget.java` (~80 lines):
- Implements `RemoteBatchTarget`
- Constructor takes `HttpTargetConfig` (endpoint, apiKey)
- `dispatch(jobId, minioPrefix, simulation, replicates)` → POST form to `<endpoint>/runBatch` with fields: `apiKey`, `jobId`, `simulation`, `replicates`, `workDir=/tmp/batch-<jobId>`, `stageFromMinio=true`, `minioPrefix`
- Uses `java.net.http.HttpClient` with form-encoded body (same pattern as `RunRemoteOffloadLeaderStrategy`)
- Validates response: 202 accepted → success; anything else → throw with message from response body

`pipeline/target/BatchJobStrategy.java` (~120 lines):
- Constructor takes `RemoteBatchTarget`, `BatchPollingStrategy`, `MinioHandler` (for staging), `OutputOptions`
- `execute(File inputDir, String simulation, String jobId, int replicates)`:
  1. **Stage**: upload `inputDir` to `batch-jobs/<jobId>/inputs/` via `MinioHandler`
  2. **Dispatch**: call `target.dispatch(jobId, minioPrefix, simulation, replicates)`
  3. **Poll**: loop on `poller.poll(jobId)` at configurable intervals until `isTerminal()`
  4. **Report**: print final status (complete with timestamp, or error with message)
- `executeNoWait(...)`: stage + dispatch, skip polling, print statusPath
- Poll timeout: configurable, default 3600s

`command/BatchRemoteCommand.java` (~100 lines):
- Picocli `@Command(name = "batchRemote")`
- Positional params: `File script`, `String simulation`
- `--target=<name>` (required) — loads profile via `TargetProfileLoader`
- `--replicates=<N>` (default 1) — passed through to `target.dispatch()`
- `--no-wait` — stage and dispatch, skip polling
- `--poll-interval=<seconds>` (default 5)
- `--timeout=<seconds>` (default 3600)
- Wiring: loads profile → builds `MinioHandler` from profile creds → builds `HttpBatchTarget` from `httpConfig` → builds `MinioPollingStrategy` from same `MinioHandler` → creates `BatchJobStrategy` → calls `execute()`

**Modify:**
- `JoshSimCommander.java` — add `BatchRemoteCommand.class` to subcommands array (1 line)
- `MinioHandler.java` — add second constructor taking raw strings: `MinioHandler(String endpoint, String accessKey, String secretKey, String bucket, OutputOptions output)`. Needed because `TargetProfile` holds MinIO creds directly, not via `MinioOptions` picocli hierarchy.
- `RemoteBatchTarget.java` — add `replicates` parameter to `dispatch()` signature
- `cloud/JoshSimBatchHandler.java` — accept optional `replicates` form field, pass through to `JoshSimFacadeUtil.runSimulation()`. Default 1.

**User experience:**
```bash
# Single job, single replicate, wait for completion
joshsim batchRemote simulation.josh Main --target=cloudrun-prod

# Single job, 10 replicates (target decides how to run them), wait
joshsim batchRemote simulation.josh Main --target=cloudrun-prod --replicates=10

# Fire and forget
joshsim batchRemote simulation.josh Main --target=nautilus --replicates=50 --no-wait
```

**Output:**
```
Loading target profile: cloudrun-prod
Staging to MinIO (batch-jobs/a1b2c3/inputs/)...  done (1 file)
Dispatching to https://josh-executor-prod... (10 replicates)
  [0s] accepted (batch-status/a1b2c3/status.json)
  [5s] running
  [45s] complete
Results in MinIO via minio:// export paths in simulation.josh
```

**Test:**
- [ ] `HttpBatchTarget` — unit tests with mock HTTP responses (202 accepted, 400 error, connection refused)
- [ ] `BatchJobStrategy` — unit tests with mock target + mock poller (stage → dispatch → poll complete, poll error, poll timeout)
- [ ] `BatchRemoteCommand` — unit test for argument parsing and profile loading
- [ ] `MinioHandler` — test new raw-string constructor
- [ ] `JoshSimBatchHandler` — test replicates form field parsing (default 1, explicit value)
- [ ] Integration: end-to-end against dev Cloud Run
- [ ] `./gradlew test` passes, `./gradlew checkstyleMain` passes, `./gradlew fatJar` builds

**Risk: LOW — mostly new files. Small modifications: JoshSimCommander (1 line), MinioHandler (new constructor), RemoteBatchTarget (add parameter), JoshSimBatchHandler (replicates field).**

---

### PR 7 ✅: Fabric8 dependency + KubernetesTarget + KubernetesPollingStrategy
**Branch: `feat/k8s-target`**

Added `io.fabric8:kubernetes-client:7.0.0` and `kubernetes-server-mock:7.0.0` (test). Implements `RemoteBatchTarget` for K8s indexed Jobs and `KubernetesPollingStrategy` for native Job status API polling.

**Dependency risk turned out to be LOW** (not HIGH). Fabric8 7.0.0 uses Jackson 2.18.2 (same as Josh), Vert.x HTTP transport (not OkHttp), SLF4J 2.0.16 → resolved to 2.0.17. Zero conflicts.

**New files:**
- `pipeline/target/KubernetesTarget.java` (~220 lines) — creates K8s indexed Jobs via Fabric8 fluent API. `completionMode: Indexed`, each pod runs 1 replicate. MinIO creds passed as env vars (picked up by `HierarchyConfig` automatically). Container command: `stageFromMinio → find .josh → run`.
- `pipeline/target/KubernetesPollingStrategy.java` (~230 lines) — reads K8s Job API. Detects infrastructure failures on Job failure only (minimizes API chatter): OOMKill, ImagePullBackOff, scheduling failures, DeadlineExceeded, BackoffLimitExceeded.

**Modified files:**
- `build.gradle` — Fabric8 deps
- `pipeline/target/KubernetesTargetConfig.java` — added `jarPath` field (default `/app/joshsim-fat.jar`)
- `command/BatchRemoteCommand.java` — added "kubernetes" case in `buildTarget()`, `buildPoller()` selects `KubernetesPollingStrategy` for K8s targets

**Key design decisions:**
- **MinIO creds to KubernetesTarget**: passed as explicit strings from `TargetProfile`, keeping `KubernetesTarget` decoupled from profile parsing
- **Client sharing**: `KubernetesTarget.getClient()` shared with `KubernetesPollingStrategy` — one connection per cluster
- **Pod inspection only on failure**: `extractFailureReason()` lists pods only when Job condition is `Failed`, not on every poll
- **Container jar path configurable**: `jarPath` field in `KubernetesTargetConfig` for custom container images

**Test:**
- [x] 8 unit tests for `KubernetesTarget` (Job spec, env vars, resources, parallelism cap, name format, command)
- [x] 9 unit tests for `KubernetesPollingStrategy` (PENDING, RUNNING, COMPLETE, ERROR, DeadlineExceeded, BackoffLimitExceeded, OOMKill, ImagePull, scheduling)
- [x] All tests use Mockito (Fabric8 mock server has JDK 21 SSL compat issues)
- [x] `./gradlew test` passes, `./gradlew checkstyleMain checkstyleTest` passes, `./gradlew fatJar` builds

**Risk: LOW — dependency conflicts did not materialize. No modifications to existing behavior.**

### PR 8: Dockerfile + e2e integration + CI workflow
**Branch: off `feat/k8s-batch`**

Batch worker Docker image and end-to-end integration testing, both locally and in CI.

**New files:**
- `cloud-img/Dockerfile.batch` — batch worker image: `FROM eclipse-temurin:21-jre`, copies fat jar + `entrypoint.sh`
- `.github/workflows/test-k8s.yaml` — K8s integration test workflow (pattern follows `test-minio.yaml`)

**Dockerfile.batch:**
```dockerfile
FROM eclipse-temurin:21-jre
COPY build/libs/joshsim-fat.jar /app/joshsim-fat.jar
COPY cloud-img/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
```

**CI workflow (`test-k8s.yaml`) — end-to-end with live Kind cluster + MinIO:**

Same trigger pattern as `test-minio.yaml` (push to main/dev, PRs). Uses GitHub Actions services for MinIO and [Kind](https://kind.sigs.k8s.io/) for a real K8s cluster.

```
Steps:
1. Checkout + setup Java 21
2. Start MinIO service container (same as test-minio.yaml)
3. ./gradlew fatJar
4. Build batch worker image: docker build -f cloud-img/Dockerfile.batch -t joshsim-batch:ci .
5. Create Kind cluster: kind create cluster
6. Load image into Kind: kind load docker-image joshsim-batch:ci
7. Deploy MinIO inside Kind (or expose host MinIO to pods via Kind networking)
8. Create target profile (~/.josh/targets/ci-k8s.json) pointing at Kind cluster
9. Run batchRemote e2e:
   - Stage test simulation to MinIO
   - joshsim batchRemote sim.josh Main --target=ci-k8s --replicates=2 --timeout=120
   - Verify results in MinIO via mc stat
   - Verify K8s Job completed: kubectl get jobs
10. Test failure detection:
    - Submit job with bad image → verify KubernetesPollingStrategy reports ImagePullBackOff
    - Submit job with tight memory limit → verify OOMKilled detection
11. Cleanup: kind delete cluster
```

**Key design considerations:**
- MinIO must be reachable from inside Kind pods. Options: (a) deploy MinIO as a K8s Deployment+Service inside Kind, or (b) use Kind's `extraPortMappings` + host.docker.internal. Option (a) is more realistic.
- The batch worker image needs the fat jar built first — `docker build` step depends on `./gradlew fatJar`
- Kind cluster creation takes ~30s, acceptable for CI
- Test timeout should be generous (pods need to pull image from Kind's local registry)

**Modify:**
- `cloud/JoshSimServer.java` — no changes (server already has `/runBatch`)
- `build.gradle` — optional: add `testKubernetes` Gradle task for running K8s integration tests separately

**Test:**
- [ ] `docker build -f cloud-img/Dockerfile.batch` succeeds
- [ ] `entrypoint.sh` runs correctly inside container (manual: `docker run --rm joshsim-batch:ci /app/entrypoint.sh --help`)
- [ ] `batchRemote --target=ci-k8s --replicates=2` completes in Kind cluster
- [ ] Results appear in MinIO
- [ ] KubernetesPollingStrategy detects ImagePullBackOff for bad image
- [ ] KubernetesPollingStrategy detects OOMKilled for tight memory limit
- [ ] `./gradlew test`, `./gradlew checkstyleMain`, `./gradlew fatJar` still pass

**Risk: MEDIUM — Kind + Docker-in-Docker in CI can be flaky. MinIO-in-Kind networking needs testing.**

### PR 9: `preprocessBatch` — remote preprocessing via target profiles

**Why this matters:** Preprocessing is the most time-consuming step for large simulations (converting GeoTIFF/NetCDF to .jshd). Currently it runs only locally. For HPC-scale workflows, preprocessing should be offloadable to the same compute targets used for simulation.

**Key insight:** `preprocessBatch` is a natural consumer of the target profile system (PRs 5-7). The dispatch flow is identical to `batchRemote` — only the operation differs:

```
1. STAGE:    stageToMinio (upload raw data files + .josh script)
2. DISPATCH: target.dispatch(jobId, "preprocess", minioPrefix)
3. EXECUTE:  worker does stageFromMinio → cd workdir → preprocess → upload .jshd to MinIO
4. RESULTS:  .jshd files land in MinIO, ready for stageFromMinio before simulation
```

**What "wiring MinioOptions into preprocess" actually requires:**
- Just adding `@Mixin MinioOptions` to `PreprocessCommand` is not sufficient — it only declares CLI flags with nothing consuming them. `PreprocessCommand` writes output to a local `FileOutputStream` ([PreprocessCommand.java:327](src/main/java/org/joshsim/command/PreprocessCommand.java#L327)), not via the export facade / `minio://` path system.
- Full wiring would need: (a) a `/preprocessBatch` server endpoint (analogous to `/runBatch` from PR 4) that does `stageFromMinio → preprocess → upload .jshd to MinIO`, and (b) a `preprocessBatch` client command that stages inputs and dispatches via `RemoteBatchTarget`.

**New files:**
- `cloud/JoshSimPreprocessBatchHandler.java` — Undertow `HttpHandler` for `/preprocessBatch`
- `command/PreprocessBatchCommand.java` — client command, reuses `BatchJobStrategy` with preprocess operation

**Modify:**
- `cloud/JoshSimServer.java` — register `/preprocessBatch` endpoint
- `JoshSimCommander.java` — register `PreprocessBatchCommand` subcommand
- `BatchJobStrategy` (or equivalent) — accept operation type parameter (run vs. preprocess)

**The target profiles don't change.** Same JSON, same creds, same `HttpBatchTarget` / `KubernetesTarget`. The only difference is the operation dispatched.

**Parallel preprocessing:** For large datasets with many timesteps, multiple preprocessing jobs can run concurrently using `--timestep` to split work across K8s indexed Jobs (each worker processes one timestep, writes to a separate .jshd, then `--amend` combines them).

**Test:**
- [ ] `/preprocessBatch` endpoint: POST with MinIO prefix containing raw data → .jshd appears in MinIO
- [ ] `preprocessBatch` command: end-to-end with HTTP target
- [ ] Existing `preprocess` command unchanged

**Risk: LOW — additive, all new files, existing commands untouched**

---

## Risk Summary

| Risk | Level | Mitigation |
|------|-------|------------|
| Fabric8 dep conflicts | ~~HIGH~~ **DONE** | Resolved in PR 7 — zero conflicts (Jackson 2.18.2, SLF4J 2.0.17, Vert.x transport) |
| `--upload-*` removal | ~~MEDIUM~~ DONE | Completed in PR 3. joshpy bottling supersedes; `stageToMinio` covers explicit uploads |
| K8s Job failure cases | MEDIUM | `backoffLimit: 3` + `activeDeadlineSeconds` + `KubernetesPollingStrategy` for native status (OOMKill, scheduling) |
| MinIO cred passing | MEDIUM | Target profiles hold creds directly; K8s Secrets later |
| Emergent memory behavior | MEDIUM | K8s polling (PR 7) catches OOMKill that MinIO status misses — critical for HPC-scale ecological sims |
