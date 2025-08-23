/**
 * Interface for handling worker response callbacks.
 *
 * <p>Implementations of this interface can process streaming responses from workers
 * and determine how to handle different types of responses.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud.pipeline;

import io.undertow.server.HttpServerExchange;
import java.util.concurrent.atomic.AtomicInteger;

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
