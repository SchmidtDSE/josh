/**
 * Polling strategy that reads job status from MinIO.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.joshsim.util.MinioHandler;


/**
 * Polls batch job status by reading {@code batch-status/<jobId>/status.json} from MinIO.
 *
 * <p>This is the default polling strategy — it works for all target types because the
 * {@code /runBatch} server handler writes status files to MinIO at job lifecycle
 * boundaries (running, complete, error). See {@code JoshSimBatchHandler} for the
 * writer side.</p>
 *
 * <p>If the status file does not yet exist (job was just dispatched), this returns
 * {@link JobStatus.State#PENDING}. If the MinIO read fails for other reasons,
 * the exception propagates to the caller.</p>
 */
public class MinioPollingStrategy implements BatchPollingStrategy {

  private static final String STATUS_PREFIX = "batch-status/";
  private static final String STATUS_SUFFIX = "/status.json";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final MinioHandler minioHandler;

  /**
   * Constructs a polling strategy that reads from the given MinIO handler.
   *
   * @param minioHandler The MinIO handler configured with the target's bucket and credentials.
   */
  public MinioPollingStrategy(MinioHandler minioHandler) {
    this.minioHandler = minioHandler;
  }

  @Override
  public JobStatus poll(String jobId) throws Exception {
    String statusPath = STATUS_PREFIX + jobId + STATUS_SUFFIX;

    String json;
    try (InputStream stream = minioHandler.downloadStream(statusPath)) {
      json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      if (isNotFoundError(e)) {
        return new JobStatus(JobStatus.State.PENDING);
      }
      throw e;
    }

    return parseStatusJson(json);
  }

  /**
   * Parses a status JSON string into a JobStatus.
   *
   * <p>Package-visible for testing.</p>
   *
   * @param json The raw JSON string from the status file.
   * @return The parsed job status.
   */
  JobStatus parseStatusJson(String json) throws Exception {
    JsonNode root = MAPPER.readTree(json);

    String status = root.has("status") ? root.get("status").asText() : null;
    String message = root.has("message") ? root.get("message").asText() : null;

    String timestamp = null;
    if (root.has("startedAt")) {
      timestamp = root.get("startedAt").asText();
    } else if (root.has("completedAt")) {
      timestamp = root.get("completedAt").asText();
    } else if (root.has("failedAt")) {
      timestamp = root.get("failedAt").asText();
    }

    JobStatus.State state = mapStatusToState(status);
    return new JobStatus(state, message, timestamp);
  }

  private JobStatus.State mapStatusToState(String status) {
    if (status == null) {
      return JobStatus.State.PENDING;
    }
    return switch (status) {
      case "running" -> JobStatus.State.RUNNING;
      case "complete" -> JobStatus.State.COMPLETE;
      case "error" -> JobStatus.State.ERROR;
      default -> JobStatus.State.PENDING;
    };
  }

  private boolean isNotFoundError(Exception exception) {
    String message = exception.getMessage();
    if (message == null) {
      return false;
    }
    // MinIO SDK throws ErrorResponseException with "NoSuchKey" for missing objects
    return message.contains("NoSuchKey")
        || message.contains("The specified key does not exist")
        || message.contains("Object does not exist");
  }
}
