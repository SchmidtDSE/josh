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
 * <p>Pod-level inspection is performed when the Job has a
 * {@code Failed} condition, and also when the Job is active —
 * if all active pods are stuck (ImagePullBackOff, CrashLoopBackOff,
 * Unschedulable), reports an error immediately rather than waiting
 * for the deadline to expire.</p>
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
      String stuckReason = checkAllPodsStuck(job);
      if (stuckReason != null) {
        return new JobStatus(
            JobStatus.State.ERROR, stuckReason, null
        );
      }
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
    String podDetail = getPodFailureDetail(job);
    if (podDetail != null) {
      return podDetail;
    }

    if (failed.getReason() != null) {
      return failed.getReason();
    }

    if (failed.getMessage() != null) {
      return failed.getMessage();
    }

    return "Job failed";
  }

  private String checkAllPodsStuck(Job job) {
    String jobName = job.getMetadata().getName();

    List<Pod> pods = client.pods()
        .inNamespace(namespace)
        .withLabel("job-name", jobName)
        .list()
        .getItems();

    if (pods.isEmpty()) {
      return null;
    }

    String firstReason = null;
    for (Pod pod : pods) {
      String reason = inspectPod(pod);
      if (reason == null) {
        return null;
      }
      if (firstReason == null) {
        firstReason = reason;
      }
    }
    return firstReason;
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
    // Scheduling failures (PodScheduled: False) are not treated as terminal.
    // On any autoscaling cluster (GKE Autopilot, Nautilus, EKS+Karpenter),
    // pods sit unschedulable while nodes are provisioned. The Job's
    // activeDeadlineSeconds handles the case where scheduling truly can't
    // succeed — K8s marks the Job Failed with DeadlineExceeded.
    return null;
  }

  private String inspectContainerStatus(ContainerStatus cs) {
    ContainerState state = cs.getState();
    if (state == null) {
      return null;
    }

    ContainerStateTerminated terminated = state.getTerminated();
    if (terminated != null && terminated.getExitCode() != null
        && terminated.getExitCode() != 0) {
      // Exit code 3: JVM ExitOnOutOfMemoryError — deterministic OOM signal.
      if (terminated.getExitCode() == 3) {
        return "OutOfMemoryError (JVM heap exhausted — increase memory limits)";
      }
      // Exit code 137: kernel OOMKill (SIGKILL).
      if (terminated.getExitCode() == 137) {
        return "OOMKilled (container exceeded memory limit)";
      }
      if (terminated.getReason() != null) {
        return terminated.getReason();
      }
      return "Container exited with code " + terminated.getExitCode();
    }

    ContainerStateWaiting waiting = state.getWaiting();
    if (waiting != null && waiting.getReason() != null) {
      String reason = waiting.getReason();
      // Only report actual failure waiting states. Transient states like
      // ContainerCreating and PodInitializing are normal during startup.
      if (reason.contains("BackOff") || reason.contains("Err")
          || reason.contains("Invalid") || reason.contains("Crash")) {
        return reason;
      }
    }
    return null;
  }
}
