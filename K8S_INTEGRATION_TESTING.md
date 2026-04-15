# GKE Integration Testing Plan

Integration test plan for validating the Josh batch execution system against a real GKE Autopilot cluster. This supplements the existing Kind CI tests (`.github/workflows/test-k8s.yaml`) which validate the mechanics in an isolated environment but can't test real-world GKE behavior (Spot VMs, Autopilot scheduling, GCS as MinIO backend, production-scale workloads).

**Prerequisite:** GKE Autopilot cluster deployed per SchmidtDSE/fire-recovery-iac#1.

---

## Setup

### 1. Target profiles

Create profiles for each test scenario in `~/.josh/targets/`:

**gke-test.json** — standard on-demand pods:
```json
{
  "type": "kubernetes",
  "kubernetes": {
    "context": "gke_<project>_<region>_<cluster>",
    "namespace": "joshsim",
    "image": "us-docker.pkg.dev/<project>/josh/joshsim-batch:latest",
    "pod_minio_endpoint": "https://storage.googleapis.com",
    "resources": {
      "requests": { "cpu": "1", "memory": "2Gi" },
      "limits": { "memory": "4Gi" }
    },
    "parallelism": 5,
    "timeoutSeconds": 600,
    "ttlSecondsAfterFinished": 3600
  },
  "minio_endpoint": "https://storage.googleapis.com",
  "minio_bucket": "<gcs-bucket>"
}
```

**gke-test-spot.json** — same but with `"spot": true`.

**gke-test-large.json** — high-memory for stress testing:
```json
{
  "kubernetes": {
    "resources": {
      "requests": { "cpu": "4", "memory": "16Gi" },
      "limits": { "memory": "32Gi" }
    },
    "parallelism": 10,
    "timeoutSeconds": 1800,
    "spot": true,
    "ttlSecondsAfterFinished": 3600,
    ...
  },
  ...
}
```

### 2. Environment

MinIO credentials for GCS:
```bash
export MINIO_ACCESS_KEY=<gcs-hmac-access-key>
export MINIO_SECRET_KEY=<gcs-hmac-secret-key>
```

### 3. Image

Build and push the batch worker image:
```bash
./gradlew fatJar
docker build -f cloud-img/Dockerfile.batch -t us-docker.pkg.dev/<project>/josh/joshsim-batch:latest .
docker push us-docker.pkg.dev/<project>/josh/joshsim-batch:latest
```

---

## Test Cases

### T1: Smoke test — single replicate

Validates the basic end-to-end path: stage → dispatch → pod runs → results in GCS.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/k8s/k8s_test.josh K8sTest \
  --target=gke-test --replicates=1
```

**Verify:**
- [ ] CLI prints staging, dispatch, polling, and completion messages
- [ ] `kubectl get jobs -n joshsim` shows a completed Job
- [ ] Results CSV exists in GCS bucket at `k8s-e2e-results/results.csv`
- [ ] Job is auto-deleted after TTL expires

---

### T2: Multi-replicate fan-out

Validates indexed Job with multiple pods running in parallel.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/k8s/k8s_test.josh K8sTest \
  --target=gke-test --replicates=5
```

**Verify:**
- [ ] `kubectl get pods -n joshsim` shows up to `parallelism` pods running concurrently
- [ ] All 5 pod completions succeed
- [ ] Results CSV has data from all replicates (append mode)

---

### T3: Spot VM scheduling

Validates that pods land on Spot VMs and that preemption is handled.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/k8s/k8s_test.josh K8sTest \
  --target=gke-test-spot --replicates=3
```

**Verify:**
- [ ] Pods are scheduled on Spot nodes: `kubectl get pods -n joshsim -o wide` shows Spot node pool
- [ ] Pod spec has `cloud.google.com/gke-spot` node selector and toleration
- [ ] If a pod is preempted during the test, `backoffLimit` triggers a retry (check pod restart count)
- [ ] Job completes despite any preemptions

---

### T4: Preprocessing — GeoTIFF to jshd

Validates remote preprocessing with real GeoTIFF data.

```bash
# Copy test data into a staging directory
mkdir -p /tmp/preprocess-test
cp examples/test/k8s-preprocess/k8s_preprocess_test.josh /tmp/preprocess-test/
cp josh-tests/test-data/spatial/grid_10x10_constant.tiff /tmp/preprocess-test/

java -jar build/libs/joshsim-fat.jar preprocessBatch \
  /tmp/preprocess-test/k8s_preprocess_test.josh K8sPreprocessTest \
  grid_10x10_constant.tiff 0 count output.jshd \
  --target=gke-test
```

**Verify:**
- [ ] CLI stages files, dispatches, polls, downloads result
- [ ] `output.jshd` is a valid non-empty file
- [ ] `inspectJshd output.jshd 0 0 0 0` returns a value without error
- [ ] Preprocess K8s Job used `preprocess-entrypoint.sh` (check pod command in `kubectl describe`)

---

### T5: Stress test — larger simulation

Validates that non-trivial simulations complete on GKE with higher resource requests. Uses the existing `stress.josh` simulation (100 timesteps, stochastic growth) but with minio:// export paths.

Create a modified stress simulation:
```bash
cp examples/simulations/stress.josh /tmp/stress-gke.josh
# Edit exportFiles.patch to use minio:// path:
#   exportFiles.patch = "minio://<bucket>/stress-results/results_{replicate}.csv"
```

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  /tmp/stress-gke.josh TestSimpleSimulation \
  --target=gke-test-large --replicates=10
```

**Verify:**
- [ ] All 10 replicates complete
- [ ] Each replicate produces a results CSV in GCS
- [ ] No OOMKill (check `kubectl describe pod` for exit codes)
- [ ] Total wall time is reasonable (parallelism=10 means all run concurrently)

---

### T6: Timeout and failure handling

Validates that `activeDeadlineSeconds` and error detection work on real GKE.

**6a — Timeout:** Create a profile with `"timeoutSeconds": 10` and run a simulation that takes longer. Verify the Job fails with `DeadlineExceeded` and the CLI reports the error.

**6b — OOMKill:** Create a profile with `"limits": { "memory": "256Mi" }` and run a simulation that uses more memory. Verify `KubernetesPollingStrategy` detects the OOMKill and reports it.

**6c — Bad image:** Use a profile pointing to a non-existent image. Verify `ImagePullBackOff` is detected within the poll timeout.

```bash
# 6c example
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/k8s/k8s_test.josh K8sTest \
  --target=gke-test-bad-image --replicates=1 --timeout=120
# Should fail with image pull error
```

---

### T7: No-wait mode + manual polling

Validates fire-and-forget dispatch.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/k8s/k8s_test.josh K8sTest \
  --target=gke-test --replicates=2 --no-wait
```

**Verify:**
- [ ] CLI exits immediately after dispatch with jobId
- [ ] `kubectl get jobs -n joshsim` shows the job running/pending
- [ ] Status file appears in GCS at `batch-status/<jobId>/status.json`
- [ ] Status transitions: `running` → `complete`

---

### T8: GCS as MinIO backend — credential resolution

Validates that GCS HMAC credentials work correctly through the full pipeline.

- [ ] `stageToMinio` uploads to GCS bucket successfully
- [ ] `stageFromMinio` downloads from GCS bucket
- [ ] Pod-side MinIO access uses the K8s Secret (not host env vars)
- [ ] Pod's `MINIO_ENDPOINT` resolves to `https://storage.googleapis.com`
- [ ] Credentials from env vars (not in profile JSON) — verify profile has no `minio_access_key`/`minio_secret_key` fields

---

### T9: Job TTL cleanup

Validates that completed Jobs are garbage collected.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/k8s/k8s_test.josh K8sTest \
  --target=gke-test --replicates=1
```

**Verify:**
- [ ] Immediately after completion: `kubectl get jobs -n joshsim` shows the Job
- [ ] After TTL expires (profile has `ttlSecondsAfterFinished: 3600`): Job is gone
- [ ] For a faster check: create a profile with `"ttlSecondsAfterFinished": 60` and wait

---

### T10: Concurrent jobs

Validates that multiple independent jobs can run simultaneously.

```bash
# Run two jobs in parallel (background the first)
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/k8s/k8s_test.josh K8sTest \
  --target=gke-test --replicates=3 &

java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/k8s/k8s_test.josh K8sTest \
  --target=gke-test --replicates=3

wait
```

**Verify:**
- [ ] Both jobs appear in `kubectl get jobs -n joshsim`
- [ ] Each has its own K8s Secret (`josh-creds-<jobId>`)
- [ ] Both complete without interfering with each other
- [ ] Autopilot scales nodes to accommodate both job sets

---

## Cost Monitoring

During testing, track costs in the GCP console:
- **GKE Autopilot compute**: should only bill during active pod time
- **GCS storage**: staging + results (should be minimal for test workloads)
- **Network egress**: pod ↔ GCS traffic (same-region should be free)

After all tests, verify:
- [ ] No lingering Jobs or pods: `kubectl get jobs,pods -n joshsim`
- [ ] No lingering Secrets: `kubectl get secrets -n joshsim -l app=joshsim`
- [ ] GKE console shows $0 control plane cost (free Autopilot tier)
- [ ] Total test cost is < $5 (expected: ~$1-2 for all tests combined)

---

## Checklist Summary

| Test | What it validates | Status |
|------|-------------------|--------|
| T1 | Basic e2e path | [ ] |
| T2 | Multi-replicate fan-out | [ ] |
| T3 | Spot VM scheduling | [ ] |
| T4 | Preprocessing (GeoTIFF → jshd) | [ ] |
| T5 | Stress test (100 steps, 10 replicates) | [ ] |
| T6 | Timeout + OOMKill + bad image | [ ] |
| T7 | No-wait mode | [ ] |
| T8 | GCS credential resolution | [ ] |
| T9 | Job TTL cleanup | [ ] |
| T10 | Concurrent jobs | [ ] |
