/**
 * Command for dispatching batch simulations to remote compute targets.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
 * <p>Two modes:</p>
 * <ol>
 *   <li><b>Trust-the-prefix</b> (default): read the {@code .josh-staged.json} sentinel at
 *   {@code --minio-prefix}; warn if absent (manual upload case), fail if in-progress or errored,
 *   proceed if complete.</li>
 *   <li><b>Strict</b> ({@code --require-prestaged}): fail fast unless the sentinel exists and
 *   reports {@code complete}. Intended for shared/multi-dispatch workflows where staging is
 *   a separate upstream step (via {@code stageToMinio}).</li>
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
      names = "--require-prestaged",
      description = "Fail fast unless .josh-staged.json at --minio-prefix reports 'complete'.",
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
      names = "--replicate-start",
      description = "Starting replicate index (default: 0). Combined with --replicates this "
          + "selects the half-open range [start, start+count). Used for pool/resume "
          + "workflows where indices need to be stable across re-dispatch.",
      defaultValue = "0"
  )
  private int replicateStart = 0;

  @Option(
      names = "--custom-tag",
      description = "Custom template parameters (format: name=value). Can be specified "
          + "multiple times. Resolvable as {name} in exportFiles paths."
  )
  private String[] customTags = new String[0];

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
      if (replicateStart < 0) {
        output.printError("--replicate-start must be >= 0");
        return DISPATCH_ERROR_CODE;
      }
      final Map<String, String> parsedCustomTags = parseCustomTags();

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

      Integer preflightResult = checkSentinel(minioHandler, normalizedPrefix);
      if (preflightResult != null) {
        return preflightResult;
      }

      BatchJobStrategy strategy = new BatchJobStrategy(
          target, poller, output,
          pollIntervalSeconds * 1000L, timeoutSeconds * 1000L
      );

      if (noWait) {
        String jobId = strategy.executeNoWait(
            normalizedPrefix, simulation, replicates, parsedCustomTags, replicateStart
        );
        ObjectNode node = MAPPER.createObjectNode();
        node.put("jobId", jobId);
        node.put("target", targetName);
        node.put("statusPath", "batch-status/" + jobId + "/status.json");
        System.out.println(node.toString());
        return 0;
      }

      JobStatus finalStatus = strategy.execute(
          normalizedPrefix, simulation, replicates, parsedCustomTags, replicateStart
      );

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
            + ". Run stageToMinio first.");
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

  /**
   * Parses {@code --custom-tag name=value} options into a map.
   *
   * <p>Mirrors {@code RunCommand.parseCustomParameters} so the same {@code .josh} script
   * resolves the same template variables on either run path. Reserved names ({@code replicate},
   * {@code step}, {@code variable}) are rejected.</p>
   */
  private Map<String, String> parseCustomTags() {
    Map<String, String> parsed = new HashMap<>();
    for (String customTag : customTags) {
      int equalsIndex = customTag.indexOf('=');
      if (equalsIndex <= 0 || equalsIndex == customTag.length() - 1) {
        throw new IllegalArgumentException("Invalid custom-tag format: " + customTag
            + ". Expected format: name=value");
      }
      String name = customTag.substring(0, equalsIndex).trim();
      String value = customTag.substring(equalsIndex + 1);

      if ("replicate".equals(name) || "step".equals(name) || "variable".equals(name)) {
        throw new IllegalArgumentException("Custom parameter name '" + name
            + "' conflicts with reserved template variable");
      }

      parsed.put(name, value);
    }
    return parsed;
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
