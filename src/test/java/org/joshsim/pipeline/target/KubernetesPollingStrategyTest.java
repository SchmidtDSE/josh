/**
 * Tests for KubernetesPollingStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.ContainerStateBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodConditionBuilder;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobConditionBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link KubernetesPollingStrategy}.
 */
@SuppressWarnings("unchecked")
class KubernetesPollingStrategyTest {

  private static final String NAMESPACE = "joshsim-lab";
  private static final String JOB_ID = "test-job-123";
  private static final String JOB_NAME = "josh-" + JOB_ID;

  private KubernetesClient mockClient;
  private io.fabric8.kubernetes.client.dsl.BatchAPIGroupDSL
      mockBatch;
  private io.fabric8.kubernetes.client.dsl.V1BatchAPIGroupDSL
      mockV1;
  private MixedOperation mockJobs;
  private NonNamespaceOperation mockNsJobs;
  private ScalableResource mockJobResource;

  private MixedOperation mockPods;
  private MixedOperation mockNsPods;
  private MixedOperation mockLabeledPods;

  private KubernetesPollingStrategy strategy;

  @BeforeEach
  void setUp() {
    mockClient = mock(KubernetesClient.class);
    mockBatch =
        mock(
            io.fabric8.kubernetes.client.dsl
                .BatchAPIGroupDSL.class
        );
    mockV1 =
        mock(
            io.fabric8.kubernetes.client.dsl
                .V1BatchAPIGroupDSL.class
        );
    mockJobs = mock(MixedOperation.class);
    mockNsJobs = mock(NonNamespaceOperation.class);
    mockJobResource = mock(ScalableResource.class);

    when(mockClient.batch()).thenReturn(mockBatch);
    when(mockBatch.v1()).thenReturn(mockV1);
    when(mockV1.jobs()).thenReturn(mockJobs);
    when(mockJobs.inNamespace(NAMESPACE)).thenReturn(mockNsJobs);
    when(mockNsJobs.withName(JOB_NAME))
        .thenReturn(mockJobResource);

    mockPods = mock(MixedOperation.class);
    mockNsPods = mock(MixedOperation.class);
    mockLabeledPods = mock(MixedOperation.class);
    when(mockClient.pods()).thenReturn(mockPods);
    when(mockPods.inNamespace(NAMESPACE)).thenReturn(mockNsPods);
    when(mockNsPods.withLabel("job-name", JOB_NAME))
        .thenReturn(mockLabeledPods);
    when(mockLabeledPods.list())
        .thenReturn(new PodListBuilder().build());

    strategy = new KubernetesPollingStrategy(
        mockClient, NAMESPACE
    );
  }

  @Test
  void pollReturnsPendingWhenJobNotFound() throws Exception {
    when(mockJobResource.get()).thenReturn(null);

    JobStatus status = strategy.poll(JOB_ID);

    assertEquals(JobStatus.State.PENDING, status.getState());
  }

  @Test
  void pollReturnsRunningWhenPodsActive() throws Exception {
    when(mockJobResource.get()).thenReturn(
        new JobBuilder()
            .withNewMetadata().withName(JOB_NAME).endMetadata()
            .withNewSpec().endSpec()
            .withStatus(new JobStatusBuilder()
                .withActive(3)
                .build())
            .build()
    );

    JobStatus status = strategy.poll(JOB_ID);

    assertEquals(JobStatus.State.RUNNING, status.getState());
  }

  @Test
  void pollReturnsCompleteOnCompleteCondition() throws Exception {
    when(mockJobResource.get()).thenReturn(
        new JobBuilder()
            .withNewMetadata().withName(JOB_NAME).endMetadata()
            .withNewSpec().endSpec()
            .withStatus(new JobStatusBuilder()
                .withConditions(new JobConditionBuilder()
                    .withType("Complete")
                    .withStatus("True")
                    .withLastTransitionTime("2026-04-14T12:00:00Z")
                    .build())
                .build())
            .build()
    );

    JobStatus status = strategy.poll(JOB_ID);

    assertEquals(JobStatus.State.COMPLETE, status.getState());
    assertTrue(status.getTimestamp().isPresent());
  }

  @Test
  void pollReturnsErrorOnFailedCondition() throws Exception {
    when(mockJobResource.get()).thenReturn(
        new JobBuilder()
            .withNewMetadata().withName(JOB_NAME).endMetadata()
            .withNewSpec().endSpec()
            .withStatus(new JobStatusBuilder()
                .withConditions(new JobConditionBuilder()
                    .withType("Failed")
                    .withStatus("True")
                    .withMessage("something went wrong")
                    .build())
                .build())
            .build()
    );

    JobStatus status = strategy.poll(JOB_ID);

    assertEquals(JobStatus.State.ERROR, status.getState());
    assertTrue(status.getMessage().isPresent());
  }

  @Test
  void pollReportsDeadlineExceeded() throws Exception {
    when(mockJobResource.get()).thenReturn(
        new JobBuilder()
            .withNewMetadata().withName(JOB_NAME).endMetadata()
            .withNewSpec()
                .withActiveDeadlineSeconds(3600L)
            .endSpec()
            .withStatus(new JobStatusBuilder()
                .withConditions(new JobConditionBuilder()
                    .withType("Failed")
                    .withStatus("True")
                    .withReason("DeadlineExceeded")
                    .build())
                .build())
            .build()
    );

    JobStatus status = strategy.poll(JOB_ID);

    assertEquals(JobStatus.State.ERROR, status.getState());
    assertTrue(
        status.getMessage().get().contains("timeout")
    );
    assertTrue(
        status.getMessage().get().contains("3600")
    );
  }

  @Test
  void pollReportsBackoffLimitExceeded() throws Exception {
    when(mockJobResource.get()).thenReturn(
        new JobBuilder()
            .withNewMetadata().withName(JOB_NAME).endMetadata()
            .withNewSpec().endSpec()
            .withStatus(new JobStatusBuilder()
                .withConditions(new JobConditionBuilder()
                    .withType("Failed")
                    .withStatus("True")
                    .withReason("BackoffLimitExceeded")
                    .build())
                .build())
            .build()
    );

    JobStatus status = strategy.poll(JOB_ID);

    assertEquals(JobStatus.State.ERROR, status.getState());
    assertTrue(
        status.getMessage().get().contains("retry limit")
    );
  }

  @Test
  void pollDetectsOomKilled() throws Exception {
    when(mockJobResource.get()).thenReturn(
        new JobBuilder()
            .withNewMetadata().withName(JOB_NAME).endMetadata()
            .withNewSpec().endSpec()
            .withStatus(new JobStatusBuilder()
                .withConditions(new JobConditionBuilder()
                    .withType("Failed")
                    .withStatus("True")
                    .withReason("BackoffLimitExceeded")
                    .build())
                .build())
            .build()
    );

    when(mockLabeledPods.list()).thenReturn(
        new PodListBuilder()
            .withItems(new PodBuilder()
                .withNewMetadata()
                    .withName("josh-pod-0")
                .endMetadata()
                .withNewStatus()
                    .withContainerStatuses(
                        new ContainerStatusBuilder()
                            .withName("joshsim")
                            .withState(new ContainerStateBuilder()
                                .withNewTerminated()
                                    .withReason("OOMKilled")
                                    .withExitCode(137)
                                .endTerminated()
                                .build())
                            .build())
                .endStatus()
                .build())
            .build()
    );

    JobStatus status = strategy.poll(JOB_ID);

    assertEquals(JobStatus.State.ERROR, status.getState());
    assertTrue(
        status.getMessage().get().contains("OOMKilled")
    );
  }

  @Test
  void pollDetectsImagePullError() throws Exception {
    when(mockJobResource.get()).thenReturn(
        new JobBuilder()
            .withNewMetadata().withName(JOB_NAME).endMetadata()
            .withNewSpec().endSpec()
            .withStatus(new JobStatusBuilder()
                .withConditions(new JobConditionBuilder()
                    .withType("Failed")
                    .withStatus("True")
                    .withReason("BackoffLimitExceeded")
                    .build())
                .build())
            .build()
    );

    when(mockLabeledPods.list()).thenReturn(
        new PodListBuilder()
            .withItems(new PodBuilder()
                .withNewMetadata()
                    .withName("josh-pod-0")
                .endMetadata()
                .withNewStatus()
                    .withContainerStatuses(
                        new ContainerStatusBuilder()
                            .withName("joshsim")
                            .withState(new ContainerStateBuilder()
                                .withNewWaiting()
                                    .withReason("ImagePullBackOff")
                                    .withMessage(
                                        "image not found"
                                    )
                                .endWaiting()
                                .build())
                            .build())
                .endStatus()
                .build())
            .build()
    );

    JobStatus status = strategy.poll(JOB_ID);

    assertEquals(JobStatus.State.ERROR, status.getState());
    assertTrue(
        status.getMessage().get().contains("ImagePullBackOff")
    );
  }

  @Test
  void pollDetectsSchedulingFailure() throws Exception {
    when(mockJobResource.get()).thenReturn(
        new JobBuilder()
            .withNewMetadata().withName(JOB_NAME).endMetadata()
            .withNewSpec().endSpec()
            .withStatus(new JobStatusBuilder()
                .withConditions(new JobConditionBuilder()
                    .withType("Failed")
                    .withStatus("True")
                    .withReason("BackoffLimitExceeded")
                    .build())
                .build())
            .build()
    );

    when(mockLabeledPods.list()).thenReturn(
        new PodListBuilder()
            .withItems(new PodBuilder()
                .withNewMetadata()
                    .withName("josh-pod-0")
                .endMetadata()
                .withNewStatus()
                    .withConditions(new PodConditionBuilder()
                        .withType("PodScheduled")
                        .withStatus("False")
                        .withMessage(
                            "0/3 nodes available:"
                            + " insufficient memory"
                        )
                        .build())
                .endStatus()
                .build())
            .build()
    );

    JobStatus status = strategy.poll(JOB_ID);

    assertEquals(JobStatus.State.ERROR, status.getState());
    assertTrue(
        status.getMessage().get().contains("Scheduling failed")
    );
    assertTrue(
        status.getMessage().get().contains("insufficient memory")
    );
  }
}
