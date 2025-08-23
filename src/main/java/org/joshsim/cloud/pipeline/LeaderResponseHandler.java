/**
 * Response handler for processing worker responses in the leader context.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud.pipeline;

import io.undertow.server.HttpServerExchange;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.joshsim.cloud.ParallelWorkerHandler;
import org.joshsim.wire.WireResponse;
import org.joshsim.wire.WireResponseParser;
import org.joshsim.wire.WireRewriteUtil;

/**
 * Response handler for processing worker responses in the leader context.
 * 
 * <p>This class handles streaming responses from worker nodes, parsing wire format messages
 * and rewriting them with appropriate replicate numbers and cumulative progress counts
 * for the leader's client connection.</p>
 */
public class LeaderResponseHandler implements ParallelWorkerHandler.WorkerResponseHandler {

  @Override
  public void handleResponseLine(String line, int replicateNumber,
                                HttpServerExchange clientExchange,
                                AtomicInteger cumulativeStepCount) {
    try {
      // Parse worker response using existing wire format parser
      Optional<WireResponse> parsed = WireResponseParser.parseEngineResponse(line);
      if (parsed.isEmpty()) {
        return; // Skip empty or ignored lines
      }
      
      WireResponse parsedResponse = parsed.get();
      String outputLine = generateOutputLine(parsedResponse, replicateNumber, cumulativeStepCount);
      
      // Thread-safe write to client
      synchronized (clientExchange.getOutputStream()) {
        clientExchange.getOutputStream().write(outputLine.getBytes());
        clientExchange.getOutputStream().flush();
      }
    } catch (IllegalArgumentException e) {
      handleLeaderException(e, line, replicateNumber, clientExchange);
    } catch (IOException e) {
      throw new RuntimeException("Error streaming to client", e);
    }
  }
  
  /**
   * Generate the appropriate output line based on the parsed wire response.
   *
   * @param parsedResponse The parsed wire response from the worker.
   * @param replicateNumber The replicate number to use for rewriting.
   * @param cumulativeStepCount The cumulative step count for progress tracking.
   * @return The formatted output line to send to the client.
   * @throws IllegalArgumentException If the response type is unknown.
   */
  private String generateOutputLine(WireResponse parsedResponse, int replicateNumber, 
                                   AtomicInteger cumulativeStepCount) {
    return switch (parsedResponse.getType()) {
      case PROGRESS -> {
        // Convert per-replicate progress to cumulative
        WireResponse cumulativeResponse = 
            WireRewriteUtil.rewriteProgressToCumulative(parsedResponse, cumulativeStepCount);
        yield WireRewriteUtil.formatWireResponse(cumulativeResponse);
      }
      case DATUM -> {
        // Rewrite replicate number from worker's 0 to actual replicate number
        WireResponse rewrittenDatum = 
            WireRewriteUtil.rewriteReplicateNumber(parsedResponse, replicateNumber);
        yield WireRewriteUtil.formatWireResponse(rewrittenDatum);
      }
      case END -> {
        // Rewrite end marker with correct replicate number
        WireResponse rewrittenEnd = 
            WireRewriteUtil.rewriteReplicateNumber(parsedResponse, replicateNumber);
        yield WireRewriteUtil.formatWireResponse(rewrittenEnd);
      }
      case ERROR -> {
        // Forward error messages unchanged
        yield WireRewriteUtil.formatWireResponse(parsedResponse);
      }
      default -> throw new IllegalArgumentException(
          "Unknown wire response type: " + parsedResponse.getType());
    };
  }
  
  /**
   * Handle exceptions that occur during leader response processing.
   *
   * @param e The exception that occurred.
   * @param line The original line that caused the exception.
   * @param replicateNumber The replicate number for fallback formatting.
   * @param clientExchange The client exchange for writing fallback output.
   */
  private void handleLeaderException(Exception e, String line, int replicateNumber,
                                     HttpServerExchange clientExchange) {
    // Handle lines that don't match wire format - pass through as-is with replicate prefix
    String fallbackOutput = WireRewriteUtil.formatDatumResponse(replicateNumber, line);
    try {
      synchronized (clientExchange.getOutputStream()) {
        clientExchange.getOutputStream().write(fallbackOutput.getBytes());
        clientExchange.getOutputStream().flush();
      }
    } catch (IOException ioException) {
      throw new RuntimeException("Error streaming fallback to client", ioException);
    }
  }
}