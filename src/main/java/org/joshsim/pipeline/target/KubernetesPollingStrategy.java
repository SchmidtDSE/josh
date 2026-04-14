/**
 * Polls Kubernetes Job status for batch job lifecycle tracking.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.List;


/**
 * Polls batch job status via the Kubernetes Job API.
 *
 * <p>Provides richer error information than {@link MinioPollingStrategy}
 * by inspecting K8s Job conditions and pod statuses. Detects
 * infrastructure failures (OOMKill, scheduling failures, image pull
 * errors) that never reach the MinIO status file because the
 * container process is killed or never starts.</p>
 *
 * <p>Pod-level inspection is performed only when the Job has a
 * {@code Failed} condition, minimizing K8s API calls during normal
 * execution.</p>
 */
public class KubernetesPollingStrategy implements BatchPollingStrategy {

  private static final String JOB_NAME_PREFIX = "josh-";

  private final KubernetesClient client;
  private final String namespace;

  /**
   * Constructs a polling strategy for the given cluster and namespace.
   *
   * @param client The Fabric8 Kubernetes client (shared with
   *     {@link KubernetesTarget}).
   * @param namespace The namespace where jobs are created.
   */
  public KubernetesPollingStrategy(
      KubernetesClient client,
      String namespace
  ) {
    this.client = client;
    this.namespace = namespace;
  }

  @Override
  public JobStatus poll(String jobId) throws Exception {
    String jobName = JOB_NAME_PREFIX + jobId;

    Job job = client.batch().v1().jobs()
        .inNamespace(namespace)
        .withName(jobName)
        .get();

    if (job == null || job.getStatus() == null) {
      return new JobStatus(JobStatus.State.PENDING);
    }

    JobCondition failed = findCondition(
        job.getStatus().getConditions(), "Failed"
    );
    if (failed != null && "True".equals(failed.getStatus())) {
      String message = extractFailureReason(job, failed);
      return new JobStatus(
          JobStatus.State.ERROR,
          message,
          failed.getLastTransitionTime()
      );
    }

    JobCondition complete = findCondition(
        job.getStatus().getConditions(), "Complete"
    );
    if (complete != null && "True".equals(complete.getStatus())) {
      return new JobStatus(
          JobStatus.State.COMPLETE,
          null,
          complete.getLastTransitionTime()
      );
    }

    Integer active = job.getStatus().getActive();
    if (active != null && active > 0) {
      return new JobStatus(JobStatus.State.RUNNING);
    }

    return new JobStatus(JobStatus.State.PENDING);
  }

  private JobCondition findCondition(
      List<JobCondition> conditions,
      String type
  ) {
    if (conditions == null) {
      return null;
    }
    return conditions.stream()
        .filter(c -> type.equals(c.getType()))
        .findFirst()
        .orElse(null);
  }

  private String extractFailureReason(
      Job job,
      JobCondition failed
  ) {
    String reason = failed.getReason();

    if ("DeadlineExceeded".equals(reason)) {
      return "Job exceeded timeout ("
          + job.getSpec().getActiveDeadlineSeconds() + "s)";
    }

    if ("BackoffLimitExceeded".equals(reason)) {
      String podDetail = getPodFailureDetail(job);
      if (podDetail != null) {
        return "Job exceeded retry limit: " + podDetail;
      }
      return "Job exceeded retry limit";
    }

    String podDetail = getPodFailureDetail(job);
    if (podDetail != null) {
      return podDetail;
    }

    if (failed.getMessage() != null) {
      return failed.getMessage();
    }

    return "Job failed" + (reason != null ? ": " + reason : "");
  }

  private String getPodFailureDetail(Job job) {
    String jobName = job.getMetadata().getName();

    List<Pod> pods = client.pods()
        .inNamespace(namespace)
        .withLabel("job-name", jobName)
        .list()
        .getItems();

    for (Pod pod : pods) {
      String detail = inspectPod(pod);
      if (detail != null) {
        return detail;
      }
    }
    return null;
  }

  private String inspectPod(Pod pod) {
    if (pod.getStatus() == null) {
      return null;
    }

    String scheduling = checkSchedulingFailure(pod);
    if (scheduling != null) {
      return scheduling;
    }

    List<ContainerStatus> statuses =
        pod.getStatus().getContainerStatuses();
    if (statuses == null) {
      return null;
    }

    for (ContainerStatus cs : statuses) {
      String detail = inspectContainerStatus(cs);
      if (detail != null) {
        return detail;
      }
    }
    return null;
  }

  private String checkSchedulingFailure(Pod pod) {
    List<PodCondition> conditions =
        pod.getStatus().getConditions();
    if (conditions == null) {
      return null;
    }
    for (PodCondition cond : conditions) {
      if ("PodScheduled".equals(cond.getType())
          && "False".equals(cond.getStatus())) {
        String msg = cond.getMessage();
        return "Scheduling failed"
            + (msg != null ? ": " + msg : "");
      }
    }
    return null;
  }

  private String inspectContainerStatus(ContainerStatus cs) {
    ContainerState state = cs.getState();
    if (state == null) {
      return null;
    }

    ContainerStateTerminated terminated = state.getTerminated();
    if (terminated != null) {
      String reason = terminated.getReason();
      if ("OOMKilled".equals(reason)) {
        return "OOMKilled: container ran out of memory";
      }
      if (reason != null && terminated.getExitCode() != null
          && terminated.getExitCode() != 0) {
        return "Container exited with " + reason
            + " (exit code " + terminated.getExitCode() + ")";
      }
    }

    ContainerStateWaiting waiting = state.getWaiting();
    if (waiting != null) {
      String reason = waiting.getReason();
      if ("ImagePullBackOff".equals(reason)
          || "ErrImagePull".equals(reason)) {
        String msg = waiting.getMessage();
        return "Image pull failed: " + reason
            + (msg != null ? " - " + msg : "");
      }
    }
    return null;
  }
}
