# GKE Integration Testing Plan

Integration test plan for validating the Josh batch execution system against a real GKE Autopilot cluster. This supplements the existing Kind CI tests (`.github/workflows/test-k8s.yaml`) which validate the mechanics in an isolated environment but can't test real-world GKE behavior (Spot VMs, Autopilot scheduling, GCS as MinIO backend, production-scale workloads).

**Cluster:** `josh-k8s-gke` in `us-west1`, project `dse-nps`
**Deployed via:** SchmidtDSE/fire-recovery-iac#1

---

## Setup

### 1. Cluster credentials

```bash
gcloud container clusters get-credentials josh-k8s-gke --region us-west1 --project dse-nps
```

### 2. MinIO/GCS credentials

```bash
export MINIO_ACCESS_KEY=$(gcloud secrets versions access latest --secret=josh-k8s-minio-access-key --project=dse-nps)
export MINIO_SECRET_KEY=$(gcloud secrets versions access latest --secret=josh-k8s-minio-secret-key --project=dse-nps)
```

### 3. Target profiles

Install the profiles from `examples/test/gke/` into `~/.josh/targets/`:

```bash
mkdir -p ~/.josh/targets
cp examples/test/gke/gke-test.json ~/.josh/targets/gke-test.json
cp examples/test/gke/gke-test-spot.json ~/.josh/targets/gke-test-spot.json
cp examples/test/gke/gke-test-large.json ~/.josh/targets/gke-test-large.json
```

Three profiles are provided:
- **gke-test** — standard on-demand pods (1 CPU, 2Gi)
- **gke-test-spot** — same resources on Spot VMs (60-90% cheaper)
- **gke-test-large** — high-memory Spot pods (4 CPU, 16Gi) for stress tests

### 4. Image

Build and push the batch worker image:

```bash
./gradlew fatJar
docker build -f cloud-img/Dockerfile.batch -t ghcr.io/schmidtdse/josh/joshsim-batch:latest .
docker push ghcr.io/schmidtdse/josh/joshsim-batch:latest
```

The GHCR package is public — no image pull secrets needed.

---

## Test Cases

### T1: Smoke test — single replicate

Validates the basic end-to-end path: stage → dispatch → pod runs → results in GCS.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke/gke_smoke_test.josh GkeSmokeTest \
  --target=gke-test --replicates=1
```

**Verify:**
- [ ] CLI prints staging, dispatch, polling, and completion messages
- [ ] `kubectl get jobs -n joshsim` shows a completed Job
- [ ] Results CSV exists in GCS at `gke-test-results/smoke_0.csv`
- [ ] Job is auto-deleted after TTL expires (~1 hour)

---

### T2: Multi-replicate fan-out

Validates indexed Job with multiple pods running in parallel.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke/gke_smoke_test.josh GkeSmokeTest \
  --target=gke-test --replicates=5
```

**Verify:**
- [ ] `kubectl get pods -n joshsim` shows up to 5 pods running concurrently
- [ ] All 5 pod completions succeed
- [ ] Results CSV has data from all replicates (append mode)

---

### T3: Spot VM scheduling

Validates that pods land on Spot VMs and that preemption is handled.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke/gke_smoke_test.josh GkeSmokeTest \
  --target=gke-test-spot --replicates=3
```

**Verify:**
- [ ] Pod spec has `cloud.google.com/gke-spot` node selector and toleration
- [ ] `kubectl get pods -n joshsim -o wide` shows pods on Spot nodes
- [ ] If a pod is preempted, `backoffLimit` triggers a retry
- [ ] Job completes despite any preemptions

---

### T4: Preprocessing — GeoTIFF to jshd

Validates remote preprocessing with real GeoTIFF data.

```bash
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
- [ ] `java -jar build/libs/joshsim-fat.jar inspectJshd output.jshd 0 0 0 0` returns a value
- [ ] Preprocess K8s Job used `preprocess-entrypoint.sh` (`kubectl describe pod -n joshsim`)

---

### T5: Stress test — larger simulation

Validates 100-timestep simulation with stochastic growth on high-resource Spot pods.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke/gke_stress_test.josh GkeStressTest \
  --target=gke-test-large --replicates=10
```

**Verify:**
- [ ] All 10 replicates complete
- [ ] Each replicate produces a results CSV in GCS at `gke-test-results/stress_<N>.csv`
- [ ] No OOMKill (`kubectl describe pod -n joshsim` — check exit codes)
- [ ] Total wall time is reasonable (parallelism=10 means all run concurrently)

---

### T6: Timeout and failure handling

Validates that `activeDeadlineSeconds` and error detection work on real GKE.

**6a — Timeout:** Create a profile with `"timeoutSeconds": 10` and run the stress test. Verify the Job fails with `DeadlineExceeded` and the CLI reports the error.

**6b — OOMKill:** Create a profile with `"limits": { "memory": "256Mi" }` and run the stress test. Verify `KubernetesPollingStrategy` detects the OOMKill.

**6c — Bad image:** Create a profile with `"image": "ghcr.io/schmidtdse/josh/nonexistent:v999"`. Verify `ImagePullBackOff` is detected.

```bash
# 6c example — create ad-hoc profile
cat > /tmp/gke-bad-image.json << 'EOF'
{
  "type": "kubernetes",
  "kubernetes": {
    "context": "gke_dse-nps_us-west1_josh-k8s-gke",
    "namespace": "joshsim",
    "image": "ghcr.io/schmidtdse/josh/nonexistent:v999",
    "pod_minio_endpoint": "https://storage.googleapis.com",
    "resources": { "requests": { "cpu": "250m", "memory": "512Mi" } },
    "parallelism": 1, "timeoutSeconds": 120
  },
  "minio_endpoint": "https://storage.googleapis.com",
  "minio_bucket": "dse-nps-josh-batch-storage"
}
EOF
cp /tmp/gke-bad-image.json ~/.josh/targets/gke-bad-image.json

java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke/gke_smoke_test.josh GkeSmokeTest \
  --target=gke-bad-image --replicates=1 --timeout=120
# Should fail with image pull error
```

---

### T7: No-wait mode + manual polling

Validates fire-and-forget dispatch.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke/gke_smoke_test.josh GkeSmokeTest \
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

- [ ] `stageToMinio` uploads to GCS bucket: `java -jar build/libs/joshsim-fat.jar stageToMinio --input-dir=examples/test/gke/ --prefix=test-staging/`
- [ ] `stageFromMinio` downloads from GCS: `java -jar build/libs/joshsim-fat.jar stageFromMinio --prefix=test-staging/ --output-dir=/tmp/staged/`
- [ ] Pod-side MinIO access uses the K8s Secret (not host env vars)
- [ ] Pod's `MINIO_ENDPOINT` is `https://storage.googleapis.com`
- [ ] Credentials come from env vars, not profile JSON (profiles have no `minio_access_key`/`minio_secret_key`)

---

### T9: Job TTL cleanup

Validates that completed Jobs are garbage collected.

```bash
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke/gke_smoke_test.josh GkeSmokeTest \
  --target=gke-test --replicates=1
```

**Verify:**
- [ ] Immediately after completion: `kubectl get jobs -n joshsim` shows the Job
- [ ] After TTL expires (profile has `ttlSecondsAfterFinished: 3600`): Job is gone
- [ ] For a faster check: create ad-hoc profile with `"ttlSecondsAfterFinished": 60` and wait

---

### T10: Concurrent jobs

Validates that multiple independent jobs can run simultaneously.

```bash
# Run two jobs in parallel (background the first)
java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke/gke_smoke_test.josh GkeSmokeTest \
  --target=gke-test --replicates=3 &

java -jar build/libs/joshsim-fat.jar batchRemote \
  examples/test/gke/gke_smoke_test.josh GkeSmokeTest \
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
- **GKE Autopilot compute**: billed per-pod-second on requests, $0 when idle
- **GCS storage**: staging + results (minimal for test workloads)
- **Network egress**: pod ↔ GCS in same region = free

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
