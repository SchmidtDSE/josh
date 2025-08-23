/**
 * Interface for handling parsed wire responses from workers.
 *
 * <p>This interface provides a higher-level abstraction for handling worker responses
 * using parsed WireResponse objects instead of raw string lines. This avoids the need
 * for repeated parsing and enables better response manipulation.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud.pipeline;

import io.undertow.server.HttpServerExchange;
import java.util.concurrent.atomic.AtomicInteger;
import org.joshsim.wire.WireResponse;

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
  void handleWireResponse(WireResponse response, int replicateNumber,
                         HttpServerExchange clientExchange, AtomicInteger cumulativeStepCount);
}
