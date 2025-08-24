/**
 * Handler for executing tasks in parallel across multiple JoshSimWorker instances.
 *
 * <p>This class extracts the parallel execution logic from JoshSimLeaderHandler and provides
 * a reusable component for coordinating multiple worker requests. It manages thread pools,
 * HTTP connections, and response processing in a centralized way.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud.pipeline;

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
  public ParallelWorkerHandler(
      String workerUrl,
      int maxParallelRequests,
      AtomicInteger cumulativeStepCount) {
    this.workerUrl = workerUrl;
    this.maxParallelRequests = maxParallelRequests;
    this.cumulativeStepCount = cumulativeStepCount;
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
  public void executeInParallel(
      List<WorkerTask> tasks,
      HttpServerExchange clientExchange,
      WorkerResponseHandler responseHandler) {
    TaskExecutor taskExecutor = task ->
        executeWorkerTask(task, clientExchange, cumulativeStepCount, responseHandler);
    executeInParallelCommon(tasks, clientExchange, taskExecutor);
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
  public void executeInParallelWire(
      List<WorkerTask> tasks,
      HttpServerExchange clientExchange,
      WireResponseHandler wireResponseHandler) {
    TaskExecutor taskExecutor = task ->
        executeWorkerTaskWire(task, clientExchange, cumulativeStepCount, wireResponseHandler);
    executeInParallelCommon(tasks, clientExchange, taskExecutor);
  }

  /**
   * Common implementation for parallel task execution.
   *
   * @param tasks The list of tasks to execute
   * @param clientExchange The client exchange for sending consolidated responses
   * @param taskExecutor Function that executes a single task
   */
  private void executeInParallelCommon(
      List<WorkerTask> tasks,
      HttpServerExchange clientExchange,
      TaskExecutor taskExecutor) {
    if (tasks.isEmpty()) {
      return;
    }

    int effectiveThreadCount = Math.min(tasks.size(), maxParallelRequests);
    ExecutorService executor = Executors.newFixedThreadPool(effectiveThreadCount);

    // Reset cumulative step counter for this execution
    cumulativeStepCount.set(0);

    try {
      // Execute tasks with streaming approach
      List<Future<?>> futures = new ArrayList<>();
      for (WorkerTask task : tasks) {
        Runnable taskRunnable = () -> taskExecutor.execute(task);
        futures.add(
            executor.submit(taskRunnable)
        );
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
   * Functional interface for task execution strategies.
   */
  @FunctionalInterface
  private interface TaskExecutor {
    void execute(WorkerTask task);
  }

  /**
   * Executes a single worker task via HTTP streaming.
   *
   * @param task The task to execute
   * @param clientExchange The client exchange for sending responses
   * @param cumulativeStepCount Shared cumulative step counter
   * @param responseHandler Handler for processing worker responses
   */
  private void executeWorkerTask(
      WorkerTask task,
      HttpServerExchange clientExchange,
      AtomicInteger cumulativeStepCount,
      WorkerResponseHandler responseHandler) {
    HttpResponse<Stream<String>> response = sendWorkerRequest(task);

    if (response.statusCode() == 200) {
      try {
        int replicateNum = task.getReplicateNumber();
        response.body().forEach(line ->
            responseHandler.handleResponseLine(
                line,
                replicateNum,
                clientExchange,
                cumulativeStepCount));
      } catch (Exception e) {
        throw new RuntimeException("Error processing response stream", e);
      }
    } else {
      handleWorkerError(task, response.statusCode());
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
   * @param cumulativeStepCount Shared cumulative step counter
   * @param wireResponseHandler Handler for processing parsed wire responses
   */
  private void executeWorkerTaskWire(
      WorkerTask task,
      HttpServerExchange clientExchange,
      AtomicInteger cumulativeStepCount,
      WireResponseHandler wireResponseHandler) {
    HttpResponse<Stream<String>> response = sendWorkerRequest(task);

    if (response.statusCode() == 200) {
      try {
        int replicateNum = task.getReplicateNumber();
        response.body().forEach(line -> {
          Optional<WireResponse> parsedResponse =
              WireResponseParser.parseEngineResponse(line.trim());
          if (parsedResponse.isPresent()) {
            WireResponse wireResponse = parsedResponse.get();
            wireResponseHandler.handleWireResponse(
                wireResponse,
                replicateNum,
                clientExchange,
                cumulativeStepCount);
          }
        });
      } catch (Exception e) {
        throw new RuntimeException("Error processing wire response stream", e);
      }
    } else {
      handleWorkerError(task, response.statusCode());
    }
  }

  /**
   * Sends an HTTP request to the worker for the given task.
   *
   * @param task The task to execute
   * @return The HTTP response from the worker
   */
  private HttpResponse<Stream<String>> sendWorkerRequest(WorkerTask task) {
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

    try {
      return client.send(request, HttpResponse.BodyHandlers.ofLines());
    } catch (IOException | InterruptedException e) {
      System.err.println("Worker connection failed for replicate " + task.getReplicateNumber()
          + " to " + workerUrl + ": " + e.getMessage());
      throw new RuntimeException("Encountered issue in worker thread: " + e);
    }
  }

  /**
   * Handles worker error responses.
   *
   * @param task The task that failed
   * @param statusCode The HTTP status code from the worker
   */
  private void handleWorkerError(WorkerTask task, int statusCode) {
    String errorMessage = String.format("Replicate %d failed: HTTP %d from %s",
        task.getReplicateNumber(), statusCode, workerUrl);
    throw new RuntimeException(errorMessage);
  }
}
