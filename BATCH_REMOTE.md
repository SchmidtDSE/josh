# Batch Remote Execution

## Overview

`batchRemote` and `preprocessBatch` dispatch simulations to remote compute via MinIO/GCS staging. Two target types share the same client flow but differ in execution and polling:

```
Client (CLI / joshpy)
  │
  ├─ Stage local files ──→ GCS (batch-jobs/<jobId>/inputs/)
  │
  ├─ Dispatch ────────────→ HTTP target: POST /runBatch to Cloud Run
  │                         K8s target:  create Job + Secret via K8s API
  │
  ├─ Poll ────────────────→ HTTP target: read status.json from GCS (MinioPollingStrategy)
  │                         K8s target:  read Job API (KubernetesPollingStrategy)
  │
  └─ Results ─────────────→ GCS via minio:// export paths in .josh script
                            (preprocessBatch also downloads result .jshd)
```

### HTTP target (Cloud Run)
- Server runs all replicates sequentially in one container
- Server writes `status.json` to GCS at lifecycle boundaries (running → complete/error)
- Cloud Run has a 60-min request timeout
- Profile type: `"type": "http"`

### K8s target (GKE Autopilot, Nautilus, EKS)
- Creates an indexed Job — one pod per replicate, parallel up to `parallelism`
- Each pod uses `--replicate-index=$JOB_COMPLETION_INDEX` for unique `{replicate}` paths
- No status.json in GCS — poller reads Job API directly
- `-XX:+ExitOnOutOfMemoryError` in entrypoints — exit code 3 for OOM, detected by poller
- Spot VM support via `"spot": true`, Job TTL cleanup via `"ttlSecondsAfterFinished"`
- Profile type: `"type": "kubernetes"`

---

## Architecture

### Target profiles (`~/.josh/targets/<name>.json`)

**HTTP** (Cloud Run):
```json
{
  "type": "http",
  "http": {
    "endpoint": "https://josh-executor-dev-....run.app",
    "apiKey": "..."
  },
  "minio_endpoint": "https://storage.googleapis.com",
  "minio_bucket": "dse-nps-josh-batch-storage"
}
```

**Kubernetes** (GKE Autopilot):
```json
{
  "type": "kubernetes",
  "kubernetes": {
    "context": "gke_dse-nps_us-west1_josh-k8s-gke",
    "namespace": "joshsim",
    "image": "ghcr.io/schmidtdse/josh/joshsim-batch:latest",
    "pod_minio_endpoint": "https://storage.googleapis.com",
    "resources": { "requests": { "cpu": "1", "memory": "2Gi" }, "limits": { "memory": "4Gi" } },
    "parallelism": 5,
    "timeoutSeconds": 600,
    "ttlSecondsAfterFinished": 3600,
    "spot": true
  },
  "minio_endpoint": "https://storage.googleapis.com",
  "minio_bucket": "dse-nps-josh-batch-storage"
}
```

**Credential resolution:** MinIO credentials resolved via `HierarchyConfig`: profile JSON → environment variables. Secrets don't need to live in the profile.

### Server endpoints

- `POST /runBatch` — async simulation execution, returns 202 + statusPath
- `POST /preprocessBatch` — async preprocessing, returns 202 + statusPath, uploads .jshd to GCS

### Batch worker image

`ghcr.io/schmidtdse/josh/joshsim-batch:latest` — single image with both entrypoints:
- `/app/run-entrypoint.sh` — stages from GCS, runs simulation at `--replicate-index`
- `/app/preprocess-entrypoint.sh` — stages from GCS, preprocesses, uploads result .jshd

Built by `buildBatchImage` job in `.github/workflows/build.yaml` on push to main/dev/feat/k8s-batch.

---

## Infrastructure

### GKE cluster
- **Cluster:** `josh-k8s-gke` in `us-west1`, project `dse-nps`
- **Namespace:** `joshsim`
- **Deployed via:** SchmidtDSE/fire-recovery-iac#1
- **GCS bucket:** `dse-nps-josh-batch-storage`
- **HMAC keys:** Secret Manager (`josh-k8s-minio-access-key`, `josh-k8s-minio-secret-key`)
- **IAM:** `josh-k8s-gcs-sa@dse-nps` with `roles/storage.objectAdmin` + `roles/storage.bucketViewer`

### Cloud Run
- **Dev:** `https://josh-executor-dev-1007495489273.us-west1.run.app`
- **API key:** enforced via `JOSH_API_KEYS` env var
- **Server env vars:** `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`

### Client setup

```bash
# GKE credentials (K8s targets only)
gcloud container clusters get-credentials josh-k8s-gke --region us-west1 --project dse-nps

# GCS HMAC credentials (both targets)
export MINIO_ACCESS_KEY=$(gcloud secrets versions access latest --secret=josh-k8s-minio-access-key --project=dse-nps)
export MINIO_SECRET_KEY=$(gcloud secrets versions access latest --secret=josh-k8s-minio-secret-key --project=dse-nps)

# Install target profiles
cp examples/test/gke/*.json ~/.josh/targets/
```

---

## Integration Test Results

### K8s target — GKE Autopilot (2026-04-17)

| Test | What it validates | Result |
|------|-------------------|--------|
| T1 | Smoke test (single replicate) | **PASS** |
| T2 | Multi-replicate fan-out (5 pods, unique CSVs) | **PASS** |
| T3 | Spot VM scheduling (node selector + toleration) | **PASS** |
| T4 | Preprocessing (GeoTIFF → jshd round-trip) | **PASS** |
| T5 | Stress test (100 steps, 10 replicates on Spot) | **PASS** |
| T6a | Timeout detection (DeadlineExceeded) | **PASS** |
| T6b | OOM detection (exit code 3, clear message) | **PASS** |
| T6c | Bad image detection (ErrImagePull) | **PASS** |
| T7 | No-wait mode (dispatch and exit) | **PASS** |
| T8 | GCS credential resolution (env vars, no creds in profile) | **PASS** |
| T9 | Job TTL cleanup (30s auto-delete) | **PASS** |
| T10 | Concurrent jobs (two simultaneous dispatches) | **PASS** |

### HTTP target — Cloud Run dev (not yet tested)

| Test | What it validates | Status |
|------|-------------------|--------|
| H1 | batchRemote smoke test | [ ] |
| H2 | Multi-replicate (sequential in-process) | [ ] |
| H3 | preprocessBatch via HTTP | [ ] |
| H4 | Parse error (async status.json) | [ ] |
| H5 | Wrong simulation name (async status.json) | [ ] |
| H6 | No-wait mode + manual GCS status poll | [ ] |

### `/runBatch` endpoint validation (2026-04-12, direct curl)

| Test | What it validates | Result |
|------|-------------------|--------|
| Endpoint-1 | Happy path CSV export | **PASS** |
| Endpoint-2 | Missing minioPrefix → 400 | **PASS** |
| Endpoint-3 | Missing required fields → 400 | **PASS** |
| Endpoint-4 | Invalid workDir → 400 | **PASS** |
| Endpoint-5 | Bad MinIO prefix → 500 | **PASS** |
| Endpoint-6 | Parse error (async) | **RETEST** |
| Endpoint-7 | Wrong simulation (async) | **RETEST** |
| Endpoint-8 | Existing endpoints unaffected | **PASS** |

---

## Discoveries during integration testing

Issues found and fixed during GKE testing that weren't in the original plan:

1. **`--replicate-index` flag** — K8s indexed Jobs need each pod at its own index. All pods were writing `{replicate}=0`. Fixed by adding `--replicate-index` to `RunCommand` and using `$JOB_COMPLETION_INDEX` in entrypoint.

2. **Poller false positives on autoscaling clusters** — `PodScheduled: False` and `ContainerCreating` are normal transient states on GKE Autopilot (and Nautilus, EKS+Karpenter). The poller was treating them as terminal errors. Fixed: scheduling delays are never terminal (deadline handles it); only `BackOff`/`Err`/`Crash` waiting states are reported.

3. **OOM detection** — Exit code 1 (generic JVM error) is indistinguishable from any crash. Fixed: `-XX:+ExitOnOutOfMemoryError` in entrypoints gives exit code 3. Poller reports `"OutOfMemoryError (JVM heap exhausted — increase memory limits)"`.

4. **Directory isolation** — `run-entrypoint.sh` uses `find *.josh | head -1`. Multiple josh files in the staging directory causes wrong-script selection. Each test simulation needs its own directory.

5. **`roles/storage.bucketViewer`** — `roles/storage.objectAdmin` doesn't include `storage.buckets.get`. The MinIO SDK's `bucketExists()` call fails without it. Added to the GCS service account IAM.

---

## PR History

PRs 1-4 merged to `dev` earlier. PRs 5-9 + integration work merged via `feat/k8s-batch`:

| PR | What | Status |
|----|------|--------|
| 1 | MinIO handler download/list/delete | **Merged** |
| 2 | stageToMinio + stageFromMinio | **Merged** |
| 3 | Remove vestigial `--upload-*` flags | **Merged** |
| 4 | `/runBatch` endpoint | **Merged** |
| 4a | Opt-in stageFromMinio for serverless | **Merged** |
| 4b | Async `/runBatch` with MinIO status | **Merged** |
| 5 | Target profiles + polling interfaces | **Merged** |
| 6 | batchRemote + HttpBatchTarget | **Merged** |
| 7 | Fabric8 + KubernetesTarget + KubernetesPollingStrategy (#395) | **Merged** |
| 8 | Dockerfile.batch + pod_minio_endpoint + Kind CI (#399) | **Merged** |
| 9 | preprocessBatch (#405) | **Merged** |
| 10 | Spot pods + Job TTL (#408) | **Merged** |
| — | Batch image CI (#411, #412) | **Merged** |
| — | GKE integration + poller fixes + OOM detection (#410) | **Merged** |
| — | `--replicate-index` (#413) | **Merged** |
| — | feat/k8s-batch → dev (#414) | **Open** |
