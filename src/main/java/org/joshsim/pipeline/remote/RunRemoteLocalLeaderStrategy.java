/**
 * Strategy that manages leader execution locally with parallel worker calls.
 *
 * <p>This strategy implements the new behavior where the local client acts as the leader,
 * coordinating multiple parallel calls to remote JoshSimWorkerHandler instances.
 * This approach provides better control over parallelism and can avoid timeout issues
 * with long-running simulations.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.joshsim.command.RemoteResponseHandler;
import org.joshsim.lang.io.ExportFacadeFactory;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.wire.WireResponse;

/**
 * Remote execution strategy that manages leadership locally with parallel workers.
 *
 * <p>This strategy replicates the coordination logic of JoshSimLeaderHandler locally,
 * making direct calls to remote JoshSimWorkerHandler instances in parallel. This approach
 * provides better control over concurrent execution and can handle long-running simulations
 * without remote server timeouts.</p>
 */
public class RunRemoteLocalLeaderStrategy implements RunRemoteStrategy {

  /**
   * Executes remote simulation using local leadership with parallel workers.
   *
   * <p>This method creates multiple parallel worker tasks and coordinates their execution
   * using the ParallelWorkerHandler. Each worker handles a single replicate, and this
   * local leader aggregates responses and manages progress reporting.</p>
   *
   * @param context The execution context containing all necessary parameters
   * @throws IOException if network communication fails
   * @throws InterruptedException if the operation is interrupted
   * @throws RuntimeException if execution fails for other reasons
   */
  @Override
  public void execute(RunRemoteContext context) throws IOException, InterruptedException {
    // Extract configuration names for display
    String configNames = getConfigNames(context.getJob());

    context.getOutputOptions().printInfo("");
    if (!configNames.isEmpty()) {
      context.getOutputOptions().printInfo("  Running: " + configNames);
    }

    // Convert the endpoint URI to point to worker handler instead of leader
    URI workerUri = convertToWorkerEndpoint(context.getEndpointUri());

    // Set up parallel worker handler
    AtomicInteger cumulativeStepCount = new AtomicInteger(0);
    ParallelWorkerHandler parallelHandler = new ParallelWorkerHandler(
        workerUri.toString(),
        context.getMaxConcurrentWorkers(),
        cumulativeStepCount
    );

    // Initialize export system with MinIO options for remote export targets
    InputOutputLayer ioLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(context.getReplicateNumber())
        .withMinioOptions(context.getMinioOptions())
        .withCustomTags(context.getJob().getCustomParameters())
        .build();
    ExportFacadeFactory exportFactory = ioLayer.getExportFacadeFactory();

    // Create shared response handler for processing worker responses
    // Disable step-by-step progress for parallel workers to avoid confusion
    // (only show replicate completion messages)
    RemoteResponseHandler responseHandler = new RemoteResponseHandler(
        context, exportFactory, false, false);

    // Create wire response handler that delegates to shared handler
    LocalLeaderWireResponseHandler wireResponseHandler =
        new LocalLeaderWireResponseHandler(responseHandler, cumulativeStepCount);

    try {
      // Create worker tasks based on replicate count
      List<WorkerTask> tasks = createWorkerTasks(context);

      // Execute tasks using ParallelWorkerHandler with wire response handling
      parallelHandler.executeInParallelWire(tasks, null, wireResponseHandler);

      context.getOutputOptions().printInfo("");

    } finally {
      // Ensure all export facades are properly closed using shared handler
      responseHandler.closeExportFacades();
    }
  }

  /**
   * Converts the leader endpoint URI to point to the worker endpoint.
   *
   * <p>Changes the endpoint path from /runReplicates (leader) to /runReplicate (worker).
   * This allows the local leader to communicate directly with worker handlers.</p>
   *
   * @param leaderUri The original leader endpoint URI
   * @return The worker endpoint URI
   * @throws RuntimeException if URI conversion fails
   */
  private URI convertToWorkerEndpoint(URI leaderUri) {
    try {
      String path = leaderUri.getPath();
      String workerPath = path.replace("/runReplicates", "/runReplicate");

      return new URI(
          leaderUri.getScheme(),
          leaderUri.getUserInfo(),
          leaderUri.getHost(),
          leaderUri.getPort(),
          workerPath,
          leaderUri.getQuery(),
          leaderUri.getFragment()
      );
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert leader URI to worker URI", e);
    }
  }

  /**
   * Extracts configuration names from the job for display purposes.
   *
   * <p>This method extracts .jshc config file names from the job's file mappings
   * to provide context about what simulation configuration is running. If there are
   * multiple .jshc files, they are comma-separated. Data files (.jshd) are only shown
   * when there are many (>3) or when there are no config files.</p>
   *
   * @param job The JoshJob containing file mappings
   * @return Description of config/data files, or empty string if none
   */
  private String getConfigNames(org.joshsim.pipeline.job.JoshJob job) {
    if (job == null || job.getFileInfos().isEmpty()) {
      return "";
    }

    List<String> configNames = new ArrayList<>();
    List<String> dataNames = new ArrayList<>();

    for (String filename : job.getFileInfos().keySet()) {
      if (filename.endsWith(".jshc")) {
        // Extract just the base name without extension
        String baseName = filename.substring(0, filename.length() - 5);
        configNames.add(baseName);
      } else if (filename.endsWith(".jshd")) {
        // Track data files separately
        String baseName = filename.substring(0, filename.length() - 5);
        dataNames.add(baseName);
      }
    }

    // Build display string
    StringBuilder display = new StringBuilder();

    if (!configNames.isEmpty()) {
      display.append(String.join(", ", configNames));
    }

    // Only show data file count if there are multiple data files
    // (showing individual names would be too verbose)
    if (dataNames.size() > 3) {
      if (display.length() > 0) {
        display.append(" (+ ").append(dataNames.size()).append(" data files)");
      }
    } else if (dataNames.size() > 0 && configNames.isEmpty()) {
      // If there are no configs but some data files, show the data file names
      display.append(String.join(", ", dataNames));
    }

    return display.toString();
  }

  /**
   * Creates worker tasks for parallel execution.
   *
   * <p>Creates multiple tasks based on the replicate count specified in the context.
   * Each task contains all the parameters needed for a worker to execute one replicate
   * with proper replicate numbering using the offset from replicateNumber.</p>
   *
   * @param context The execution context
   * @return List of worker tasks to execute
   */
  private List<WorkerTask> createWorkerTasks(RunRemoteContext context) {
    List<WorkerTask> tasks = new ArrayList<>();
    boolean favorBigDecimal = !context.isUseFloat64();

    for (int i = 0; i < context.getReplicates(); i++) {
      WorkerTask task = new WorkerTask(
          context.getJoshCode(),
          context.getSimulation(),
          context.getApiKey(),
          context.getExternalDataSerialized(),
          favorBigDecimal,
          context.getReplicateNumber() + i,  // Offset by replicateNumber
          ""  // No output filtering for local leader strategy (export all steps)
      );
      tasks.add(task);
    }

    return tasks;
  }

  /**
   * Wire response handler that delegates to the shared RemoteResponseHandler.
   *
   * <p>This class implements the WireResponseHandler interface and delegates
   * to the shared RemoteResponseHandler for consistent response processing.
   * It adapts the WireResponseHandler interface to work with the shared handler.</p>
   */
  private static class LocalLeaderWireResponseHandler
      implements WireResponseHandler {

    private final RemoteResponseHandler sharedHandler;
    private final AtomicInteger cumulativeStepCount;

    /**
     * Creates a new LocalLeaderWireResponseHandler.
     *
     * @param sharedHandler The shared response handler to delegate to
     * @param cumulativeStepCount Shared cumulative step counter for progress coordination
     */
    public LocalLeaderWireResponseHandler(RemoteResponseHandler sharedHandler,
                                         AtomicInteger cumulativeStepCount) {
      this.sharedHandler = sharedHandler;
      this.cumulativeStepCount = cumulativeStepCount;
    }

    /**
     * Handles a parsed wire response from a worker.
     *
     * <p>Delegates to the shared RemoteResponseHandler for consistent processing
     * while passing the cumulative step counter for progress coordination.</p>
     *
     * @param response The parsed wire response from the worker
     * @param replicateNumber The replicate number for this response
     * @param clientExchange The client exchange (unused in this implementation)
     * @param cumulativeStepCount Shared cumulative step counter (unused - we use our own)
     */
    @Override
    public void handleWireResponse(WireResponse response, int replicateNumber,
                                  io.undertow.server.HttpServerExchange clientExchange,
                                  AtomicInteger cumulativeStepCount) {
      // Convert WireResponse back to string format for shared handler
      // This avoids duplicating all the processing logic
      String wireFormatLine = response.toWireFormat();

      // Delegate to shared handler with our cumulative step count
      sharedHandler.processResponseLine(wireFormatLine, replicateNumber, this.cumulativeStepCount);
    }
  }
}
