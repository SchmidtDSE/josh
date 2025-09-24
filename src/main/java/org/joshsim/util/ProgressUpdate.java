/**
 * Container class for progress update information.
 *
 * <p>This class encapsulates the result of a progress calculation, including
 * whether the update should be reported and the formatted message to display.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

/**
 * Container class for progress update information.
 *
 * <p>This class encapsulates the result of a progress calculation, including
 * whether the update should be reported and the formatted message to display.</p>
 */
public class ProgressUpdate {
  private final boolean shouldReport;
  private final double percentage;
  private final String message;

  /**
   * Creates a new ProgressUpdate.
   *
   * @param shouldReport Whether this progress update should be reported to the user
   * @param percentage The current progress percentage (0-100)
   * @param message The formatted progress message, or null if not reporting
   */
  public ProgressUpdate(boolean shouldReport, double percentage, String message) {
    this.shouldReport = shouldReport;
    this.percentage = percentage;
    this.message = message;
  }

  /**
   * Returns whether this progress update should be reported.
   *
   * @return true if the update should be displayed to the user
   */
  public boolean shouldReport() {
    return shouldReport;
  }

  /**
   * Returns the current progress percentage.
   *
   * @return Progress percentage (0-100)
   */
  public double getPercentage() {
    return percentage;
  }

  /**
   * Returns the formatted progress message.
   *
   * @return Formatted message string, or null if not reporting
   */
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return String.format(
        "ProgressUpdate{shouldReport=%s, percentage=%.1f, message='%s'}",
        shouldReport,
        percentage,
        message);
  }
}
