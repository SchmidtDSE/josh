/**
 * Command for polling the status of a dispatched batch job.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.Callable;
import org.joshsim.pipeline.target.BatchPollingStrategy;
import org.joshsim.pipeline.target.JobStatus;
import org.joshsim.pipeline.target.TargetProfile;
import org.joshsim.pipeline.target.TargetProfileLoader;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Polls the status of a previously dispatched batch job.
 *
 * <p>Single-shot status check — loads the target profile, queries the
 * appropriate status source (MinIO status file for HTTP targets, K8s
 * Job API for Kubernetes targets), and outputs JSON to stdout. The
 * caller (joshpy) handles polling intervals and retry logic.</p>
 *
 * <p>Exit codes: 0 = complete, 1 = error, 2 = running/pending.</p>
 */
@Command(
    name = "pollBatch",
    description = "Check the status of a dispatched batch job"
)
public class PollBatchCommand implements Callable<Integer> {

  static final int EXIT_COMPLETE = 0;
  static final int EXIT_ERROR = 1;
  static final int EXIT_RUNNING = 2;
  private static final int EXIT_POLL_FAILURE = 100;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Parameters(
      index = "0",
      description = "Job ID returned by batchRemote --no-wait"
  )
  private String jobId;

  @Option(
      names = "--target",
      description = "Target profile name "
          + "(loads ~/.josh/targets/<name>.json)",
      required = true
  )
  private String targetName;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    try {
      TargetProfileLoader loader = new TargetProfileLoader();
      TargetProfile profile = loader.load(targetName);
      BatchPollingStrategy poller =
          profile.buildPollingStrategy(output);

      JobStatus status = poller.poll(jobId);

      System.out.println(formatJson(status));
      return exitCodeFor(status.getState());

    } catch (Exception e) {
      output.printError(
          "pollBatch failed: " + e.getMessage()
      );
      return EXIT_POLL_FAILURE;
    }
  }

  String formatJson(JobStatus status) {
    ObjectNode node = MAPPER.createObjectNode();
    node.put("status", status.getState().name().toLowerCase());
    node.put("jobId", jobId);

    status.getMessage().ifPresent(
        msg -> node.put("message", msg)
    );

    status.getTimestamp().ifPresent(ts -> {
      String key = timestampKeyFor(status.getState());
      node.put(key, ts);
    });

    return node.toString();
  }

  static int exitCodeFor(JobStatus.State state) {
    return switch (state) {
      case COMPLETE -> EXIT_COMPLETE;
      case ERROR -> EXIT_ERROR;
      case RUNNING, PENDING -> EXIT_RUNNING;
    };
  }

  private static String timestampKeyFor(JobStatus.State state) {
    return switch (state) {
      case RUNNING, PENDING -> "startedAt";
      case COMPLETE -> "completedAt";
      case ERROR -> "failedAt";
    };
  }
}
