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

**Credential Secret lifecycle:** each dispatch creates a `josh-creds-<jobId>` Secret, then adds an
`ownerReferences` entry pointing at the Job it created (`KubernetesJobSecret`). K8s garbage collection
deletes the Secret when the Job goes away. The Secret is written *before* the Job so no pod can start
against a missing `secretKeyRef`; the owner reference is added afterwards because it needs the Job's
server-assigned UID.

Set `ttlSecondsAfterFinished` on any target you care about — it is what actually triggers the
cascade. Without it the Job (and therefore the Secret) survives until deleted by hand.

### Server endpoints

- `POST /runBatch` — async simulation execution, returns 202 + statusPath
- `POST /preprocessBatch` — async preprocessing, returns 202 + statusPath, uploads .jshd to GCS

### Preprocess time axis

`preprocessBatch` accepts the same declared time axis as the `preprocess` command: `--time-type`,
`--time-start`, `--time-unit`, `--time-count`, `--time-increment`, `--time-interval`,
`--time-instant`. Field names are declared once in `TimeAxisParams.Field` and derived per transport,
so all three surfaces stay in step:

| Transport | Encoding |
|---|---|
| CLI | `--time-start=2015` |
| HTTP form (`/preprocessBatch`) | `timeStart=2015` |
| K8s pod env | one `JOSH_TIME_OPTS` holding the whole flag string |
| MCP (`preprocess_data`) | `"timeStart": "2015"` |

Blank values stay off the wire, and omitting them all writes a timeless .jshd.

`--time-count` must equal the number of slices the job writes. A job dispatched with `--timestep`
writes exactly one, so per-timestep fan-out must declare `--time-instant` rather than a
`--time-start`/`--time-count` range, and combining those results with `--amend` requires ascending,
contiguous timesteps.

### Batch worker image

`ghcr.io/schmidtdse/josh/joshsim-batch:latest` — single image with both entrypoints:
- `/app/run-entrypoint.sh` — stages from GCS, runs simulation at `--replicate-index`
- `/app/preprocess-entrypoint.sh` — stages from GCS, preprocesses, uploads result .jshd

Both entrypoints wait for DNS on the host parsed out of `$MINIO_ENDPOINT` before staging, since a
pod's resolver can be briefly unusable right after start and a failed `stageFromMinio` wastes the
full JVM startup. The probe follows the configured endpoint rather than a fixed hostname so it stays
meaningful off GCP.

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

### Nautilus / NRP

Profile: `examples/test/nautilus/nautilus.json`. Two values need substituting before use:
`kubernetes.namespace` and `minio_bucket`. Validated verbatim otherwise — see results below.

- **Kubeconfig:** download from the [NRP portal](https://nrp.ai); the context is `nautilus`.
  Fabric8 reads it through the same `Config.autoConfigure(context)` path as GKE.
- **Auth needs a plugin, and it is interactive.** NRP authenticates via OIDC against authentik with
  no static credential in the kubeconfig. The kubeconfig's `exec` block calls
  `kubectl oidc-login` ([kubelogin](https://github.com/int128/kubelogin)), which must be installed as
  `kubectl-oidc_login` on `PATH` — Fabric8 shells out to the same plugin, so josh needs it just as
  much as kubectl does. First use opens a browser flow; `offline_access` then yields a refresh token
  so renewals are silent. In a headless container, kubelogin listens on `--listen-address` and prints
  a URL: complete it in a browser and, if the redirect cannot reach the container, replay the
  `?code=...&state=...` callback against the listener with `curl` from inside it.

  This is the one place NRP is materially worse than GKE for automation: unattended dispatch requires
  a pre-warmed token cache plus the plugin binary. For CI or scheduled runs, prefer a ServiceAccount
  token in the namespace, which removes both the plugin and the interactive step.
- **Object storage:** NRP's Ceph RGW is S3-compatible, so the MinIO client works unchanged. Get keys
  from the portal's User → S3 Tokens page and export them as `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`.
  The token can create its own buckets — `stageToMinio --ensure-bucket-exists` is enough, no admin
  step. Note `stageToMinio` takes no `--target`, so it reads endpoint and bucket from the
  environment, while `batchRemote` reads them from the profile; the `minio://` export path in the
  `.josh` script has to name the same bucket as both.
- **The endpoint split matters here.** The client uses the public gateway
  `https://s3-west.nrp-nautilus.io`; pods use the in-cluster RGW service
  `http://rook-ceph-rgw-nautiluss3.rook`, which talks to the OSDs directly for higher bandwidth.
  That is exactly what `pod_minio_endpoint` is for.
- **Resource policy:** NRP requires `limits` within 20% of `requests`, and calls out
  `ephemeral-storage` specifically — staging writes inputs to the pod's ephemeral disk, so a job with
  large `.jshd` inputs is evicted without a request for it. The profile sets requests == limits on
  cpu/memory/ephemeral-storage to stay inside the policy.
- **Do not set `"spot": true`.** `applySpotConfig` emits a `cloud.google.com/gke-spot` selector and
  toleration, which is GKE-only and will make pods unschedulable on NRP.

Verified on namespace `schmidtdse` (2026-08-05):

| Test | What it validates | Result |
|------|-------------------|--------|
| N1 | Smoke test (single replicate), 40s end to end | **PASS** |
| N2 | Multi-replicate fan-out — 3 pods at indices 0/1/2, three distinct `{replicate}` CSVs | **PASS** |
| N3 | Preprocessing round-trip (GeoTIFF → 16MB .jshd downloaded) | **PASS** |
| N4 | Credential Secret GC'd with the Job — `ownerReferences` set on both the run and preprocess paths; deleting the Job removed the Secret | **PASS** |
| N5 | Pod admission — namespace carries no `pod-security.kubernetes.io/*` label, so the root-running batch image is admitted. No `securityContext` needed. | **PASS** |

Environment notes from that run:

- **Pods scheduled across institutions** — the three N2 replicates landed on nodes at SDSC, SDSC-HaoSu
  and UC Santa Cruz, all writing back to the west Ceph pool through the in-cluster RGW service. The
  in-cluster endpoint resolves from every site, so `pod_minio_endpoint` needs no per-site variation.
- **Quotas are not a constraint at this scale:** namespace cap is 200 pods; GPU quotas are 0 (no GPU
  access) and the `high-priority-ban` / `low-priority-ban` quotas mean pods must stay at default
  priority, which josh does since it never sets `priorityClassName`.
- **LimitRanges are permissive** — Pod max memory 1Ti, container defaults (cpu 100m / memory 1Gi)
  apply only when unset, and `ephemeral-storage` defaults to a 50Gi limit. The profile's
  2 cpu / 4Gi / 16Gi ephemeral sits well inside all of it.
- **RBAC check before dispatching** — `kubectl auth can-i` on `create/get/delete jobs.batch`,
  `create/get/patch secrets`, `list pods`, `get pods/log`. `patch secrets` matters specifically: it is
  what the `ownerReferences` binding needs, and without it dispatch still succeeds but warns and
  leaves the Secret behind.
- **Pods are not labelled `app=joshsim`** — josh sets that label on the Job only, so ad-hoc pod
  queries need `-l job-name=<job>`, which is also what `KubernetesPollingStrategy` uses.

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
