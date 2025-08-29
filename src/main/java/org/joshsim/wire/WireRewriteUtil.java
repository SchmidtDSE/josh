/**
 * Utility for rewriting and formatting wire responses.
 *
 * <p>This utility class provides centralized methods for generating wire format strings
 * and rewriting response objects with updated replicate numbers and progress counts. It
 * eliminates code duplication across handlers that need to manipulate wire responses.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility for rewriting and formatting wire responses.
 *
 * <p>This class centralizes wire format generation and response rewriting functionality
 * that was previously duplicated across multiple handlers. It provides methods for
 * formatting different types of wire responses and creating modified versions of
 * existing responses.</p>
 */
public class WireRewriteUtil {

  /**
   * Formats a progress response with the specified step count.
   *
   * @param stepCount The current step count
   * @return A wire format progress string ending with newline
   */
  public static String formatProgressResponse(long stepCount) {
    return String.format("[progress %d]\n", stepCount);
  }

  /**
   * Formats a datum response with the specified replicate number and data.
   *
   * @param replicateNumber The replicate number for this data
   * @param dataLine The data payload
   * @return A wire format datum string ending with newline
   */
  public static String formatDatumResponse(int replicateNumber, String dataLine) {
    return String.format("[%d] %s\n", replicateNumber, dataLine);
  }

  /**
   * Formats an end response with the specified replicate number.
   *
   * @param replicateNumber The replicate number that completed
   * @return A wire format end string ending with newline
   */
  public static String formatEndResponse(int replicateNumber) {
    return String.format("[end %d]\n", replicateNumber);
  }

  /**
   * Formats an error response with the specified error message.
   *
   * @param errorMessage The error message
   * @return A wire format error string ending with newline
   */
  public static String formatErrorResponse(String errorMessage) {
    return String.format("[error] %s\n", errorMessage);
  }

  /**
   * Creates a new WireResponse with the specified replicate number.
   *
   * <p>This method creates a copy of the given response with an updated replicate number.
   * The original response is not modified. This is useful when forwarding responses
   * from workers to clients with different replicate numbering.</p>
   *
   * @param response The original response
   * @param newReplicateNumber The new replicate number to use
   * @return A new WireResponse with the updated replicate number
   * @throws IllegalArgumentException if the response type doesn't support replicate numbers
   */
  public static WireResponse rewriteReplicateNumber(WireResponse response, int newReplicateNumber) {
    if (response == null) {
      throw new IllegalArgumentException("Response cannot be null");
    }

    return switch (response.getType()) {
      case DATUM -> new WireResponse(newReplicateNumber, response.getDataLine());
      case END -> new WireResponse(WireResponse.ResponseType.END, newReplicateNumber);
      case PROGRESS -> response; // Progress responses don't have replicate numbers
      case ERROR -> response;    // Error responses don't have replicate numbers
      default -> throw new IllegalArgumentException("Unknown response type: " + response.getType());
    };
  }

  /**
   * Creates a new WireResponse with cumulative progress count.
   *
   * <p>This method creates a PROGRESS response with a cumulative step count calculated
   * by adding the original step count to the cumulative counter. This is useful for
   * combining progress from multiple workers into a single cumulative progress stream.</p>
   *
   * @param response The original progress response
   * @param cumulativeCounter The cumulative counter to update and use
   * @return A new WireResponse with cumulative progress count
   * @throws IllegalArgumentException if the response is not a PROGRESS type
   */
  public static WireResponse rewriteProgressToCumulative(WireResponse response,
                                                        AtomicInteger cumulativeCounter) {
    if (response == null) {
      throw new IllegalArgumentException("Response cannot be null");
    }

    if (response.getType() != WireResponse.ResponseType.PROGRESS) {
      throw new IllegalArgumentException("Can only rewrite PROGRESS responses to cumulative");
    }

    long originalStepCount = response.getStepCount();
    int cumulative = cumulativeCounter.addAndGet((int) originalStepCount);
    return new WireResponse(cumulative);
  }

  /**
   * Formats a wire response to its string representation with newline.
   *
   * <p>This method uses the response's toWireFormat() method and adds a newline
   * for network transmission. This provides a consistent way to format responses
   * for output streams.</p>
   *
   * @param response The response to format
   * @return The wire format string with newline
   */
  public static String formatWireResponse(WireResponse response) {
    if (response == null) {
      throw new IllegalArgumentException("Response cannot be null");
    }
    return response.toWireFormat() + "\n";
  }
}
