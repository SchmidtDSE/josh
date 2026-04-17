# Integration Testing Plan: `/runBatch` and `batchRemote` HTTP Target

Tests for the HTTP target path — client stages to GCS, POSTs to Cloud Run `/runBatch`, server runs simulation, results land in GCS. This complements the K8s integration tests in `K8S_INTEGRATION_TESTING.md`.

**Cloud Run dev instance:** `https://josh-executor-dev-1007495489273.us-west1.run.app`

---

## Previous `/runBatch` endpoint tests (2026-04-12)

Direct `curl` POST tests against the endpoint, validating request validation and error handling.

| Test | Result | Notes |
|------|--------|-------|
| Test 1: Happy path (CSV export) | **PASS** | stage → run → results in MinIO |
| Test 2: Missing minioPrefix | **PASS** | 400 with correct error |
| Test 3: Missing required fields | **PASS** | 400 with correct error |
| Test 4: Invalid workDir | **PASS** | 400 with correct error |
| Test 5: Bad MinIO prefix | **PASS** | 500 with `"No objects found"` |
| Test 6: Parse error (async) | **RETEST** | Now 202 + error in status.json |
| Test 7: Wrong simulation name (async) | **RETEST** | Now 202 + error in status.json |
| Test 8: Existing endpoints | **PASS** | `/health` 200, `/parse` 200 |

---

## Setup

### Credentials

```bash
# GCS HMAC credentials (same bucket as K8s tests)
export MINIO_ACCESS_KEY=$(gcloud secrets versions access latest --secret=josh-k8s-minio-access-key --project=dse-nps)
export MINIO_SECRET_KEY=$(gcloud secrets versions access latest --secret=josh-k8s-minio-secret-key --project=dse-nps)
export MINIO_ENDPOINT=https://storage.googleapis.com
export MINIO_BUCKET=dse-nps-josh-batch-storage

# API key for Cloud Run (JOSH_API_KEYS is enforced)
source .devcontainer/.env
export API_KEY=$JOSH_API_KEY
```

### HTTP target profile

Create `~/.josh/targets/cloudrun-dev.json`:

```json
{
  "type": "http",
  "http": {
    "endpoint": "https://josh-executor-dev-1007495489273.us-west1.run.app",
    "apiKey": "<JOSH_API_KEY>"
  },
  "minio_endpoint": "https://storage.googleapis.com",
  "minio_bucket": "dse-nps-josh-batch-storage"
}
```

Note: The API key must be in the profile (not env var) because `HttpBatchTarget` reads it from `HttpTargetConfig`. MinIO credentials can come from env vars via `HierarchyConfig`.

### Fat jar

```bash
./gradlew fatJar
```

---

## Test Cases

### H1: Smoke test — batchRemote with HTTP target

Validates the full `batchRemote` CLI → HTTP target path: stage → POST `/runBatch` → poll MinIO status → verify results.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke-smoke/gke_smoke_test.josh GkeSmokeTest \
  --target=cloudrun-dev --replicates=1 --poll-interval=5 --timeout=120
```

**Verify:**
- [ ] CLI stages files to GCS
- [ ] `HttpBatchTarget` POSTs to `/runBatch` with `stageFromMinio=true`
- [ ] Server returns 202 Accepted
- [ ] `MinioPollingStrategy` polls `batch-status/<jobId>/status.json` until `complete`
- [ ] Results CSV exists in GCS at `gke-test-results/smoke_0.csv`

**Key difference from K8s target:** Polling uses `MinioPollingStrategy` (reads status.json from GCS), not `KubernetesPollingStrategy` (reads Job API). The server writes status.json — the K8s entrypoint does not.

---

### H2: Multi-replicate via HTTP target

Cloud Run runs all replicates in-process (no indexed Jobs). The server loops replicates sequentially.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke-smoke/gke_smoke_test.josh GkeSmokeTest \
  --target=cloudrun-dev --replicates=3 --poll-interval=5 --timeout=120
```

**Verify:**
- [ ] Server runs 3 replicates sequentially
- [ ] Results CSV has data from all 3 replicates (replicate column 0, 1, 2)
- [ ] Status transitions: `running` → `complete`

**Note:** Unlike K8s where each replicate is a separate pod, HTTP target runs all replicates in one Cloud Run container. `{replicate}` template works because the server's replicate loop increments the index.

---

### H3: preprocessBatch with HTTP target

Validates the `/preprocessBatch` endpoint via `HttpPreprocessTarget`.

```bash
mkdir -p /tmp/preprocess-http-test
cp examples/test/k8s-preprocess/k8s_preprocess_test.josh /tmp/preprocess-http-test/
cp josh-tests/test-data/spatial/grid_10x10_constant.tiff /tmp/preprocess-http-test/

java -jar build/libs/joshsim-fat.jar preprocessBatch \
  /tmp/preprocess-http-test/k8s_preprocess_test.josh K8sPreprocessTest \
  grid_10x10_constant.tiff 0 count output.jshd \
  --target=cloudrun-dev --poll-interval=5 --timeout=120
```

**Verify:**
- [ ] CLI stages josh script + GeoTIFF to GCS
- [ ] `HttpPreprocessTarget` POSTs to `/preprocessBatch`
- [ ] Server preprocesses and uploads result .jshd to GCS
- [ ] CLI downloads `output.jshd`
- [ ] `inspectJshd output.jshd data 0 0 0` returns 42

---

### H4: Async error handling — parse error

```bash
mkdir -p /tmp/bad-script-http
echo "this is not valid josh code" > /tmp/bad-script-http/bad.josh

java -jar build/libs/joshsim-fat.jar batchRemote \
  /tmp/bad-script-http/bad.josh BadSim \
  --target=cloudrun-dev --replicates=1 --poll-interval=5 --timeout=60
```

**Verify:**
- [ ] CLI stages files, dispatches (202 returned)
- [ ] Polling reads status.json from GCS with `{"status":"error","message":"..."}`
- [ ] CLI reports failure with parse error message

---

### H5: Async error handling — wrong simulation name

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke-smoke/gke_smoke_test.josh WrongSimName \
  --target=cloudrun-dev --replicates=1 --poll-interval=5 --timeout=60
```

**Verify:**
- [ ] Dispatches successfully (202)
- [ ] status.json shows `{"status":"error","message":"Simulation not found: WrongSimName"}`
- [ ] CLI reports failure

---

### H6: No-wait mode with HTTP target

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke-smoke/gke_smoke_test.josh GkeSmokeTest \
  --target=cloudrun-dev --replicates=1 --no-wait
```

**Verify:**
- [ ] CLI exits immediately with jobId and statusPath
- [ ] Manual check: `gsutil cat gs://dse-nps-josh-batch-storage/batch-status/<jobId>/status.json` shows `complete` after a few seconds

**Key difference from K8s no-wait:** HTTP target writes status.json to GCS, so manual polling via `gsutil` or `stageFromMinio` works. K8s targets don't write status.json — you'd use `kubectl get jobs`.

---

### H7: Cloud Run timeout behavior

Cloud Run dev has a 60-minute request timeout. For simulations approaching this limit, the server should write an error status before the container is killed.

Not easily testable without a very long simulation. Document as a known constraint.

---

## Environment notes

- Cloud Run env vars must include `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` for the `/runBatch` and `/preprocessBatch` handlers
- The dev instance enforces `JOSH_API_KEYS` — API key required in the profile
- Cloud Run runs replicates sequentially in-process (no parallelism)
- The bucket `dse-nps-josh-batch-storage` is shared between K8s and HTTP targets — same GCS credentials work for both
- `roles/storage.bucketViewer` required on the GCS service account (for `bucketExists()` check)

---

## Checklist Summary

| Test | What it validates | Status |
|------|-------------------|--------|
| H1 | batchRemote smoke test via HTTP | [ ] |
| H2 | Multi-replicate via HTTP | [ ] |
| H3 | preprocessBatch via HTTP | [ ] |
| H4 | Parse error (async status) | [ ] |
| H5 | Wrong simulation name (async status) | [ ] |
| H6 | No-wait mode via HTTP | [ ] |
| H7 | Cloud Run timeout | Known constraint |
