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

package org.joshsim.command;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.joshsim.cloud.ParallelWorkerHandler;
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
    context.getOutputOptions().printInfo("Using local leader mode - "
        + "coordinating " + context.getMaxConcurrentWorkers() 
        + " concurrent workers locally");

    // Convert the endpoint URI to point to worker handler instead of leader
    URI workerUri = convertToWorkerEndpoint(context.getEndpointUri());
    context.getOutputOptions().printInfo("Worker endpoint: " + workerUri);

    // Set up parallel worker handler
    AtomicInteger cumulativeStepCount = new AtomicInteger(0);
    ParallelWorkerHandler parallelHandler = new ParallelWorkerHandler(
        workerUri.toString(),
        context.getMaxConcurrentWorkers(),
        cumulativeStepCount
    );

    // Initialize export system
    InputOutputLayer ioLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(context.getReplicateNumber())
        .build();
    ExportFacadeFactory exportFactory = ioLayer.getExportFacadeFactory();

    // Create shared response handler for processing worker responses
    RemoteResponseHandler responseHandler = new RemoteResponseHandler(
        context, exportFactory, true); // useCumulativeProgress = true for local coordination

    // Create wire response handler that delegates to shared handler
    LocalLeaderWireResponseHandler wireResponseHandler = 
        new LocalLeaderWireResponseHandler(responseHandler, cumulativeStepCount);

    try {
      // Create worker tasks - currently supporting single replicate but structured for multiple
      List<ParallelWorkerHandler.WorkerTask> tasks = createWorkerTasks(context);
      
      context.getOutputOptions().printInfo("Executing " + tasks.size() 
          + " worker tasks in parallel");

      // Execute tasks using ParallelWorkerHandler with wire response handling
      parallelHandler.executeInParallelWire(tasks, null, wireResponseHandler);

      context.getOutputOptions().printInfo("All worker tasks completed successfully");

    } finally {
      // Ensure all export facades are properly closed using shared handler
      responseHandler.closeExportFacades();
    }

    context.getOutputOptions().printInfo("Results saved locally via export facade");
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
   * Creates worker tasks for parallel execution.
   *
   * <p>Currently creates a single task for single replicate execution, but structured
   * to support multiple replicates in the future. Each task contains all the parameters
   * needed for a worker to execute one replicate.</p>
   *
   * @param context The execution context
   * @return List of worker tasks to execute
   */
  private List<ParallelWorkerHandler.WorkerTask> createWorkerTasks(RunRemoteContext context) {
    List<ParallelWorkerHandler.WorkerTask> tasks = new ArrayList<>();
    
    // Currently supporting single replicate - can be extended for multiple replicates
    ParallelWorkerHandler.WorkerTask task = new ParallelWorkerHandler.WorkerTask(
        context.getJoshCode(),
        context.getSimulation(),
        context.getApiKey(),
        context.getExternalDataSerialized(),
        !context.isUseFloat64(), // favorBigDecimal is inverse of useFloat64
        context.getReplicateNumber()
    );
    
    tasks.add(task);
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
      implements ParallelWorkerHandler.WireResponseHandler {
    
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