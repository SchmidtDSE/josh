/**
 * Handler for executing tasks in parallel across multiple JoshSimWorker instances.
 *
 * <p>This class extracts the parallel execution logic from JoshSimLeaderHandler and provides
 * a reusable component for coordinating multiple worker requests. It manages thread pools,
 * HTTP connections, and response processing in a centralized way.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import io.undertow.server.HttpServerExchange;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.joshsim.wire.WireResponse;
import org.joshsim.wire.WireResponseParser;

/**
 * Handler for executing tasks in parallel across multiple JoshSimWorker instances.
 *
 * <p>This class manages the execution of multiple replicate tasks by distributing them
 * across worker instances. It handles thread management, HTTP communication, and
 * provides callback mechanisms for processing worker responses.</p>
 */
public class ParallelWorkerHandler {

  private final String workerUrl;
  private final int maxParallelRequests;
  private final AtomicInteger cumulativeStepCount;

  /**
   * Creates a new ParallelWorkerHandler.
   *
   * @param workerUrl The URL of the worker endpoint
   * @param maxParallelRequests Maximum number of concurrent requests
   * @param cumulativeStepCount Shared counter for cumulative progress tracking
   */
  public ParallelWorkerHandler(String workerUrl, int maxParallelRequests, 
                              AtomicInteger cumulativeStepCount) {
    this.workerUrl = workerUrl;
    this.maxParallelRequests = maxParallelRequests;
    this.cumulativeStepCount = cumulativeStepCount;
  }

  /**
   * Task configuration for worker execution.
   *
   * <p>This class encapsulates all the parameters needed to execute a single
   * replicate task on a worker instance.</p>
   */
  public static class WorkerTask {
    private final String code;
    private final String simulationName;
    private final String apiKey;
    private final String externalData;
    private final boolean favorBigDecimal;
    private final int replicateNumber;

    /**
     * Creates a new WorkerTask.
     *
     * @param code The Josh simulation code
     * @param simulationName The name of the simulation
     * @param apiKey The API key for authentication
     * @param externalData External data for the simulation
     * @param favorBigDecimal Whether to favor BigDecimal precision
     * @param replicateNumber The replicate number for this task
     */
    public WorkerTask(String code, String simulationName, String apiKey, 
                     String externalData, boolean favorBigDecimal, int replicateNumber) {
      this.code = code;
      this.simulationName = simulationName;
      this.apiKey = apiKey;
      this.externalData = externalData;
      this.favorBigDecimal = favorBigDecimal;
      this.replicateNumber = replicateNumber;
    }

    public String getCode() {
      return code;
    }

    public String getSimulationName() {
      return simulationName;
    }

    public String getApiKey() {
      return apiKey;
    }

    public String getExternalData() {
      return externalData;
    }

    public boolean isFavorBigDecimal() {
      return favorBigDecimal;
    }

    public int getReplicateNumber() {
      return replicateNumber;
    }
  }

  /**
   * Interface for handling worker response callbacks.
   *
   * <p>Implementations of this interface can process streaming responses from workers
   * and determine how to handle different types of responses.</p>
   */
  public interface WorkerResponseHandler {
    /**
     * Handles a line of streaming response from a worker.
     *
     * @param line The response line from the worker
     * @param replicateNumber The replicate number for this response
     * @param clientExchange The client exchange for sending responses
     * @param cumulativeStepCount Shared cumulative step counter
     */
    void handleResponseLine(String line, int replicateNumber, 
                           HttpServerExchange clientExchange, AtomicInteger cumulativeStepCount);
  }

  /**
   * Interface for handling parsed wire responses from workers.
   *
   * <p>This interface provides a higher-level abstraction for handling worker responses
   * using parsed WireResponse objects instead of raw string lines. This avoids the need
   * for repeated parsing and enables better response manipulation.</p>
   */
  public interface WireResponseHandler {
    /**
     * Handles a parsed wire response from a worker.
     *
     * @param response The parsed wire response from the worker
     * @param replicateNumber The replicate number for this response
     * @param clientExchange The client exchange for sending responses
     * @param cumulativeStepCount Shared cumulative step counter
     */
    void handleWireResponse(org.joshsim.wire.WireResponse response, int replicateNumber,
                           HttpServerExchange clientExchange, AtomicInteger cumulativeStepCount);
  }

  /**
   * Executes tasks in parallel across multiple worker instances.
   *
   * <p>This method creates a thread pool and distributes the provided tasks across
   * worker instances. Each task is executed as a separate HTTP request to the
   * worker URL, with responses processed through the provided response handler.</p>
   *
   * @param tasks The list of tasks to execute
   * @param clientExchange The client exchange for sending consolidated responses
   * @param responseHandler Handler for processing worker responses
   * @throws RuntimeException if any worker execution fails
   */
  public void executeInParallel(List<WorkerTask> tasks, HttpServerExchange clientExchange,
                               WorkerResponseHandler responseHandler) {
    int effectiveThreadCount = Math.min(tasks.size(), maxParallelRequests);
    ExecutorService executor = Executors.newFixedThreadPool(effectiveThreadCount);

    // Reset cumulative step counter for this execution
    cumulativeStepCount.set(0);

    try {
      // Execute tasks with streaming approach
      List<Future<?>> futures = new ArrayList<>();
      for (WorkerTask task : tasks) {
        futures.add(executor.submit(() -> 
            executeWorkerTask(task, clientExchange, responseHandler)));
      }

      // Wait for all tasks to complete
      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (Exception e) {
          System.err.println("Exception in worker task execution: "
              + e.getClass().getSimpleName() + ": " + e.getMessage());
          throw new RuntimeException("Worker task execution failed", e);
        }
      }
    } finally {
      executor.shutdown();
    }
  }

  /**
   * Executes tasks in parallel and returns a stream of parsed WireResponse objects.
   *
   * <p>This method provides a higher-level interface that returns parsed WireResponse
   * objects instead of raw string lines. This avoids the need for callers to repeatedly
   * parse responses and enables better response manipulation.</p>
   *
   * @param tasks The list of tasks to execute
   * @param clientExchange The client exchange for sending consolidated responses
   * @param wireResponseHandler Handler for processing parsed wire responses
   * @throws RuntimeException if any worker execution fails
   */
  public void executeInParallelWire(List<WorkerTask> tasks, HttpServerExchange clientExchange,
                                   WireResponseHandler wireResponseHandler) {
    int effectiveThreadCount = Math.min(tasks.size(), maxParallelRequests);
    ExecutorService executor = Executors.newFixedThreadPool(effectiveThreadCount);

    // Reset cumulative step counter for this execution
    cumulativeStepCount.set(0);

    try {
      // Execute tasks with wire response streaming approach
      List<Future<?>> futures = new ArrayList<>();
      for (WorkerTask task : tasks) {
        futures.add(executor.submit(() -> 
            executeWorkerTaskWire(task, clientExchange, wireResponseHandler)));
      }

      // Wait for all tasks to complete
      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (Exception e) {
          System.err.println("Exception in worker task execution: "
              + e.getClass().getSimpleName() + ": " + e.getMessage());
          throw new RuntimeException("Worker task execution failed", e);
        }
      }
    } finally {
      executor.shutdown();
    }
  }

  /**
   * Executes a single worker task via HTTP streaming.
   *
   * @param task The task to execute
   * @param clientExchange The client exchange for sending responses
   * @param responseHandler Handler for processing worker responses
   */
  private void executeWorkerTask(WorkerTask task, HttpServerExchange clientExchange,
                                WorkerResponseHandler responseHandler) {
    String bodyString = String.format(
        "code=%s&name=%s&apiKey=%s&externalData=%s&favorBigDecimal=%s",
        URLEncoder.encode(task.getCode(), StandardCharsets.UTF_8),
        URLEncoder.encode(task.getSimulationName(), StandardCharsets.UTF_8),
        URLEncoder.encode(task.getApiKey(), StandardCharsets.UTF_8),
        URLEncoder.encode(task.getExternalData(), StandardCharsets.UTF_8),
        URLEncoder.encode(task.isFavorBigDecimal() ? "true" : "false", StandardCharsets.UTF_8)
    );
    HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(bodyString);

    HttpClient client = HttpClient.newBuilder().build();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(workerUrl))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(body)
        .build();

    HttpResponse<Stream<String>> response = null;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofLines());
    } catch (IOException | InterruptedException e) {
      System.err.println("Worker connection failed for replicate " + task.getReplicateNumber()
          + " to " + workerUrl + ": " + e.getMessage());
      throw new RuntimeException("Encountered issue in worker thread: " + e);
    }

    if (response.statusCode() == 200) {
      try {
        response.body().forEach(line -> 
            responseHandler.handleResponseLine(line, task.getReplicateNumber(), 
                                             clientExchange, cumulativeStepCount));
      } catch (Exception e) {
        throw new RuntimeException("Error processing response stream", e);
      }
    } else {
      // Handle different error status codes from worker
      String errorMessage = String.format("Replicate %d failed: HTTP %d from %s",
          task.getReplicateNumber(), response.statusCode(), workerUrl);
      throw new RuntimeException(errorMessage);
    }
  }

  /**
   * Executes a single worker task via HTTP streaming with wire response parsing.
   *
   * <p>This method executes a worker task and provides parsed WireResponse objects
   * to the response handler. This avoids the need for the handler to parse wire
   * format strings and enables better response manipulation.</p>
   *
   * @param task The task to execute
   * @param clientExchange The client exchange for sending responses
   * @param wireResponseHandler Handler for processing parsed wire responses
   */
  private void executeWorkerTaskWire(WorkerTask task, HttpServerExchange clientExchange,
                                    WireResponseHandler wireResponseHandler) {
    String bodyString = String.format(
        "code=%s&name=%s&apiKey=%s&externalData=%s&favorBigDecimal=%s",
        URLEncoder.encode(task.getCode(), StandardCharsets.UTF_8),
        URLEncoder.encode(task.getSimulationName(), StandardCharsets.UTF_8),
        URLEncoder.encode(task.getApiKey(), StandardCharsets.UTF_8),
        URLEncoder.encode(task.getExternalData(), StandardCharsets.UTF_8),
        URLEncoder.encode(task.isFavorBigDecimal() ? "true" : "false", StandardCharsets.UTF_8)
    );
    HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(bodyString);

    HttpClient client = HttpClient.newBuilder().build();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(workerUrl))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(body)
        .build();

    HttpResponse<Stream<String>> response = null;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofLines());
    } catch (IOException | InterruptedException e) {
      System.err.println("Worker connection failed for replicate " + task.getReplicateNumber()
          + " to " + workerUrl + ": " + e.getMessage());
      throw new RuntimeException("Encountered issue in worker thread: " + e);
    }

    if (response.statusCode() == 200) {
      try {
        response.body().forEach(line -> {
          Optional<WireResponse> parsedResponse = 
              WireResponseParser.parseEngineResponse(line.trim());
          if (parsedResponse.isPresent()) {
            wireResponseHandler.handleWireResponse(parsedResponse.get(), 
                task.getReplicateNumber(), clientExchange, cumulativeStepCount);
          }
        });
      } catch (Exception e) {
        throw new RuntimeException("Error processing wire response stream", e);
      }
    } else {
      // Handle different error status codes from worker
      String errorMessage = String.format("Replicate %d failed: HTTP %d from %s",
          task.getReplicateNumber(), response.statusCode(), workerUrl);
      throw new RuntimeException(errorMessage);
    }
  }
}