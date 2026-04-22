/**
 * Command for dispatching batch simulations to remote compute targets.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
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
import org.joshsim.util.MinioHandler.StagedState;
import org.joshsim.util.MinioHandler.StagedStatus;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Dispatches a batch simulation to a remote compute target against a MinIO prefix.
 *
 * <p>Three modes:</p>
 * <ol>
 *   <li><b>Stage-and-dispatch</b> ({@code --stage-from-local-dir}): upload the local directory
 *   to {@code --minio-prefix} (writing a {@code .josh-staged.json} sentinel), then dispatch.
 *   </li>
 *   <li><b>Trust-the-prefix</b> (no staging flag): read the sentinel; warn if absent (manual
 *   upload case), fail if in-progress or errored, proceed if complete.</li>
 *   <li><b>Strict</b> ({@code --require-prestaged}): fail fast unless the sentinel exists and
 *   reports {@code complete}. Intended for shared/multi-dispatch workflows where staging is
 *   a separate upstream step.</li>
 * </ol>
 *
 * <p>Supports both HTTP and Kubernetes target types; the target profile determines
 * how the dispatched job runs.</p>
 */
@Command(
    name = "batchRemote",
    description = "Dispatch a simulation to a remote batch target against a MinIO prefix"
)
public class BatchRemoteCommand implements Callable<Integer> {

  private static final int TARGET_ERROR_CODE = 100;
  private static final int DISPATCH_ERROR_CODE = 101;
  private static final int USAGE_ERROR_CODE = 102;
  private static final int STAGING_ERROR_CODE = 103;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Parameters(index = "0", description = "Simulation name to execute")
  private String simulation;

  @Option(
      names = "--target",
      description = "Target profile name (loads ~/.josh/targets/<name>.json)",
      required = true
  )
  private String targetName;

  @Option(
      names = "--minio-prefix",
      description = "MinIO object prefix where inputs live (e.g. batch-jobs/my-run/inputs/).",
      required = true
  )
  private String minioPrefix;

  @Option(
      names = "--stage-from-local-dir",
      description = "Upload this local directory to --minio-prefix before dispatching. "
          + "Writes a .josh-staged.json sentinel. Mutually exclusive with --require-prestaged."
  )
  private File stageFromLocalDir;

  @Option(
      names = "--require-prestaged",
      description = "Fail fast unless .josh-staged.json at --minio-prefix reports 'complete'. "
          + "Mutually exclusive with --stage-from-local-dir.",
      defaultValue = "false"
  )
  private boolean requirePrestaged = false;

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
    if (stageFromLocalDir != null && requirePrestaged) {
      output.printError("--stage-from-local-dir and --require-prestaged are mutually exclusive.");
      return USAGE_ERROR_CODE;
    }

    try {
      output.printInfo("Loading target profile: " + targetName);
      TargetProfileLoader loader = new TargetProfileLoader();
      TargetProfile profile = loader.load(targetName);

      MinioHandler minioHandler = profile.buildMinioHandler(output);

      RemoteBatchTarget target;
      BatchPollingStrategy poller;
      String type = profile.getType();

      if ("kubernetes".equals(type)) {
        KubernetesTarget k8sTarget = buildKubernetesTarget(profile);
        target = k8sTarget;
        poller = buildPoller(k8sTarget);
      } else if ("http".equals(type)) {
        target = buildHttpTarget(profile);
        poller = buildPoller(minioHandler);
      } else {
        throw new IllegalArgumentException(
            "Unsupported target type: " + type
            + ". Supported types: http, kubernetes"
        );
      }

      String normalizedPrefix = MinioHandler.normalizePrefix(minioPrefix);

      if (stageFromLocalDir != null) {
        Integer stagingResult = stageLocalDir(minioHandler, normalizedPrefix);
        if (stagingResult != null) {
          return stagingResult;
        }
      } else {
        Integer preflightResult = checkSentinel(minioHandler, normalizedPrefix);
        if (preflightResult != null) {
          return preflightResult;
        }
      }

      BatchJobStrategy strategy = new BatchJobStrategy(
          target, poller, output,
          pollIntervalSeconds * 1000L, timeoutSeconds * 1000L
      );

      if (noWait) {
        String jobId = strategy.executeNoWait(normalizedPrefix, simulation, replicates);
        ObjectNode node = MAPPER.createObjectNode();
        node.put("jobId", jobId);
        node.put("target", targetName);
        node.put("statusPath", "batch-status/" + jobId + "/status.json");
        System.out.println(node.toString());
        return 0;
      }

      JobStatus finalStatus = strategy.execute(normalizedPrefix, simulation, replicates);

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

  private Integer stageLocalDir(MinioHandler minioHandler, String normalizedPrefix) {
    if (!stageFromLocalDir.exists()) {
      output.printError("--stage-from-local-dir not found: " + stageFromLocalDir.getPath());
      return USAGE_ERROR_CODE;
    }
    if (!stageFromLocalDir.isDirectory()) {
      output.printError("--stage-from-local-dir is not a directory: "
          + stageFromLocalDir.getPath());
      return USAGE_ERROR_CODE;
    }

    output.printInfo("Staging " + stageFromLocalDir.getName() + " to " + normalizedPrefix + "...");
    try {
      minioHandler.writeStagedSentinel(normalizedPrefix, StagedState.STAGING, null);
      int uploaded;
      try {
        uploaded = minioHandler.uploadDirectory(stageFromLocalDir, normalizedPrefix);
      } catch (IOException uploadError) {
        try {
          minioHandler.writeStagedSentinel(
              normalizedPrefix, StagedState.ERROR, uploadError.getMessage());
        } catch (IOException sentinelError) {
          output.printError("Additionally failed to write error sentinel: "
              + sentinelError.getMessage());
        }
        throw uploadError;
      }
      minioHandler.writeStagedSentinel(normalizedPrefix, StagedState.COMPLETE, null);
      output.printInfo("Staged " + uploaded + " file(s).");
    } catch (IOException e) {
      output.printError("Staging failed: " + e.getMessage());
      return STAGING_ERROR_CODE;
    }
    return null;
  }

  private Integer checkSentinel(MinioHandler minioHandler, String normalizedPrefix) {
    Optional<StagedStatus> sentinel;
    try {
      sentinel = minioHandler.readStagedSentinel(normalizedPrefix);
    } catch (IOException e) {
      output.printError("Failed to read staging sentinel at " + normalizedPrefix + ": "
          + e.getMessage());
      return STAGING_ERROR_CODE;
    }

    if (sentinel.isEmpty()) {
      if (requirePrestaged) {
        output.printError("--require-prestaged: no .josh-staged.json under " + normalizedPrefix
            + ". Run stageToMinio or use --stage-from-local-dir first.");
        return STAGING_ERROR_CODE;
      }
      output.printInfo("WARNING: Josh did not detect .josh-staged.json under "
          + normalizedPrefix + " — expected if you uploaded manually; "
          + "dispatch will fail if required files are missing.");
      return null;
    }

    StagedState state = sentinel.get().state();
    if (state == StagedState.COMPLETE) {
      return null;
    }
    if (requirePrestaged) {
      output.printError("--require-prestaged: sentinel reports state=" + describeState(state)
          + describeMessage(sentinel.get()));
      return STAGING_ERROR_CODE;
    }
    if (state == StagedState.STAGING) {
      output.printError("Sentinel at " + normalizedPrefix
          + " reports staging still in progress. Retry once staging completes.");
      return STAGING_ERROR_CODE;
    }
    output.printError("Sentinel at " + normalizedPrefix + " reports prior staging failed"
        + describeMessage(sentinel.get()));
    return STAGING_ERROR_CODE;
  }

  private static String describeState(StagedState state) {
    return state == null ? "unknown" : state.name().toLowerCase();
  }

  private static String describeMessage(StagedStatus status) {
    return status.message() != null ? ": " + status.message() : ".";
  }

  private HttpBatchTarget buildHttpTarget(TargetProfile profile) {
    return new HttpBatchTarget(profile.getHttpConfig());
  }

  private KubernetesTarget buildKubernetesTarget(TargetProfile profile) {
    return new KubernetesTarget(
        profile.getKubernetesConfig(),
        profile.buildMinioOptions()
    );
  }

  private BatchPollingStrategy buildPoller(MinioHandler minioHandler) {
    return new MinioPollingStrategy(minioHandler);
  }

  private BatchPollingStrategy buildPoller(KubernetesTarget k8sTarget) {
    return new KubernetesPollingStrategy(
        k8sTarget.getClient(),
        k8sTarget.getConfig().getNamespace()
    );
  }
}
