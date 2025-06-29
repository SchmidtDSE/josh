/**
 * Exception thrown during simulation execution that provides sanitized error messages.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

/**
 * Custom exception for simulation execution errors that separates user-facing messages
 * from internal error details to prevent information disclosure.
 */
public class SimulationExecutionException extends Exception {

  private final String userMessage;
  private final Throwable originalCause;

  /**
   * Constructs a SimulationExecutionException with a user-safe message and original cause.
   *
   * @param userMessage A sanitized message safe to return to users
   * @param originalCause The original exception that caused this error
   */
  public SimulationExecutionException(String userMessage, Throwable originalCause) {
    super(userMessage, originalCause);
    this.userMessage = userMessage;
    this.originalCause = originalCause;
  }

  /**
   * Gets the sanitized message suitable for returning to users.
   *
   * @return The user-safe error message
   */
  public String getUserMessage() {
    return userMessage;
  }

  /**
   * Gets the original exception that caused this error for internal logging.
   *
   * @return The original exception
   */
  public Throwable getOriginalCause() {
    return originalCause;
  }
}