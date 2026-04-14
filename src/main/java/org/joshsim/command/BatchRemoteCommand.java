/**
 * Command for dispatching batch simulations to remote compute targets.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.util.concurrent.Callable;
import org.joshsim.pipeline.target.BatchJobStrategy;
import org.joshsim.pipeline.target.BatchPollingStrategy;
import org.joshsim.pipeline.target.HttpBatchTarget;
import org.joshsim.pipeline.target.JobStatus;
import org.joshsim.pipeline.target.KubernetesPollingStrategy;
import org.joshsim.pipeline.target.KubernetesTarget;
import org.joshsim.pipeline.target.MinioPollingStrategy;
import org.joshsim.pipeline.target.RemoteBatchTarget;
import org.joshsim.pipeline.target.TargetProfile;
import org.joshsim.pipeline.target.TargetProfileLoader;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Dispatches a batch simulation to a remote compute target.
 *
 * <p>Stages local simulation files to MinIO, dispatches execution to the configured
 * target, and optionally polls for completion. The target profile determines how the
 * job runs — an HTTP target POSTs to a joshsim server, a Kubernetes target creates
 * an indexed Job, etc.</p>
 */
@Command(
    name = "batchRemote",
    description = "Run a simulation on a remote batch target via MinIO staging"
)
public class BatchRemoteCommand implements Callable<Integer> {

  private static final int TARGET_ERROR_CODE = 100;
  private static final int DISPATCH_ERROR_CODE = 101;

  @Parameters(index = "0", description = "Path to Josh simulation file or input directory")
  private File input;

  @Parameters(index = "1", description = "Simulation name to execute")
  private String simulation;

  @Option(
      names = "--target",
      description = "Target profile name (loads ~/.josh/targets/<name>.json)",
      required = true
  )
  private String targetName;

  @Option(
      names = "--replicates",
      description = "Number of replicates to execute (passed to target)",
      defaultValue = "1"
  )
  private int replicates = 1;

  @Option(
      names = "--no-wait",
      description = "Dispatch and exit without polling for completion",
      defaultValue = "false"
  )
  private boolean noWait = false;

  @Option(
      names = "--poll-interval",
      description = "Seconds between status polls (default: 5)",
      defaultValue = "5"
  )
  private int pollIntervalSeconds = 5;

  @Option(
      names = "--timeout",
      description = "Maximum seconds to wait for completion (default: 3600)",
      defaultValue = "3600"
  )
  private int timeoutSeconds = 3600;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    try {
      output.printInfo("Loading target profile: " + targetName);
      TargetProfileLoader loader = new TargetProfileLoader();
      TargetProfile profile = loader.load(targetName);

      RemoteBatchTarget target = buildTarget(profile);
      MinioHandler minioHandler = profile.buildMinioHandler(output);
      BatchPollingStrategy poller = buildPoller(
          profile, target, minioHandler
      );

      BatchJobStrategy strategy = new BatchJobStrategy(
          target, poller, minioHandler, output,
          pollIntervalSeconds * 1000L, timeoutSeconds * 1000L
      );

      File inputDir = resolveInputDir();

      if (noWait) {
        String jobId = strategy.executeNoWait(inputDir, simulation, replicates);
        output.printInfo("Job dispatched: " + jobId);
        output.printInfo("Poll manually: batch-status/" + jobId + "/status.json");
        return 0;
      }

      JobStatus finalStatus = strategy.execute(inputDir, simulation, replicates);

      if (finalStatus.getState() == JobStatus.State.COMPLETE) {
        output.printInfo("Batch job completed successfully.");
        return 0;
      } else {
        output.printError("Batch job failed: "
            + finalStatus.getMessage().orElse("unknown error"));
        return DISPATCH_ERROR_CODE;
      }

    } catch (Exception e) {
      output.printError("batchRemote failed: " + e.getMessage());
      return TARGET_ERROR_CODE;
    }
  }

  private BatchPollingStrategy buildPoller(
      TargetProfile profile,
      RemoteBatchTarget target,
      MinioHandler minioHandler
  ) {
    if ("kubernetes".equals(profile.getType())
        && target instanceof KubernetesTarget k8sTarget) {
      return new KubernetesPollingStrategy(
          k8sTarget.getClient(),
          k8sTarget.getConfig().getNamespace()
      );
    }
    return new MinioPollingStrategy(minioHandler);
  }

  private RemoteBatchTarget buildTarget(TargetProfile profile) {
    String type = profile.getType();
    if ("http".equals(type)) {
      return new HttpBatchTarget(profile.getHttpConfig());
    }
    if ("kubernetes".equals(type)) {
      return new KubernetesTarget(
          profile.getKubernetesConfig(),
          profile.getMinioEndpoint(),
          profile.getMinioAccessKey(),
          profile.getMinioSecretKey(),
          profile.getMinioBucket()
      );
    }
    throw new IllegalArgumentException(
        "Unsupported target type: " + type
        + ". Supported types: http, kubernetes"
    );
  }

  /**
   * Resolves the input to a directory. If the input is a file (e.g., simulation.josh),
   * uses its parent directory. If it's already a directory, uses it directly.
   */
  private File resolveInputDir() {
    if (input.isDirectory()) {
      return input;
    }
    return input.getParentFile() != null ? input.getParentFile() : new File(".");
  }
}
