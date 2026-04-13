# Plan: Batch Remote Execution for Josh

## Context

Cloud Run has hard ceilings (32 GiB memory, 60-min timeout, 8 vCPUs) that block HPC-scale simulations. The existing `runRemote` uses HTTP streaming — code and data travel in the request body, results stream back via wire format. This breaks when .jshd files are large. We're adding a parallel execution path (`batchRemote`) that uses MinIO as the staging layer instead of HTTP transport.

**Key design decisions:**
- **Staging and execution are separate concerns.** `stageToMinio` / `stageFromMinio` handle I/O. `run`, `preprocess`, `validate` work on local files. The dispatch mechanism (HTTP, K8s, SSH) just says "go."
- **`runRemote` is untouched.** It's the production Josh Cloud path. `batchRemote` is a parallel path, not a replacement.
- **Results go directly to MinIO** via `minio://` export paths in the Josh script. No wire format, no streaming results back to the client.
- **Composable target system.** Users register compute targets as JSON profiles (`~/.josh/targets/<name>.json`). One command (`batchRemote --target=<name>`) works with any backend.

## Workflow

All PRs build into `feat/enhanced_remote`. For each PR: branch off → implement → open PR targeting `feat/enhanced_remote` → review → merge → demonstrate with real model run. Final rollup PR targets `dev` after colleague review.

**Important:** PRs target `feat/enhanced_remote`, NOT `dev`.

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
PR1 ✅ → PR2 ✅ → PR3 ✅ (cleanup) → PR4 ✅ (/runBatch) → PR5 (profiles) → PR6 (batchRemote) → PR7 (K8sTarget) → PR8 (Dockerfile) → PR9 (preprocessBatch)
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

### PR 5: Fabric8 dependency + target profile system
**Branch: `feat/k8s-target-profiles`**

Add `io.fabric8:kubernetes-client:7.0.0` to build.gradle. New files: `TargetProfile`, `HttpTargetConfig`, `KubernetesTargetConfig`, `TargetProfileLoader`. The loader reads `~/.josh/targets/<name>.json` and returns a `RemoteBatchTarget`.

**Risk: HIGH (Fabric8 dep conflicts) — verify with `./gradlew dependencies` first**

### PR 6: `batchRemote` command + HttpBatchTarget + BatchJobStrategy
**Branch: `feat/batch-remote`**

New top-level command. Uses `BatchJobStrategy` (target-agnostic orchestration) with `HttpBatchTarget` as first implementation (POST to `/runBatch`). Does NOT touch `runRemote`.

### PR 7: KubernetesTarget with Fabric8
**Branch: `feat/k8s-target-impl`**

Implements `RemoteBatchTarget` for K8s indexed Jobs. Plugs into same `BatchJobStrategy`.

### PR 8: Dockerfile + e2e integration

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
| Fabric8 dep conflicts | **HIGH** | Verify with `./gradlew dependencies` before code (PR 5) |
| `--upload-*` removal | MEDIUM | joshpy bottling supersedes; `stageToMinio` covers explicit uploads |
| K8s Job failure cases | MEDIUM | `backoffLimit: 3` + `activeDeadlineSeconds` + poll |
| MinIO cred passing | MEDIUM | Env vars via HierarchyConfig; K8s Secrets later |
