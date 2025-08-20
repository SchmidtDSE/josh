/**
 * Utility for calculating and formatting progress updates during simulation execution.
 *
 * <p>This class provides functionality to calculate percentage-based progress and format
 * informative progress messages during remote simulation execution. It implements intelligent
 * filtering to reduce verbose output while maintaining meaningful progress feedback.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;


/**
 * Calculator for simulation progress with intelligent message filtering.
 *
 * <p>This class tracks simulation progress across multiple replicates and provides
 * percentage-based progress calculations with intelligent filtering to reduce verbose
 * output. It formats progress messages consistently with the HTML editor experience.</p>
 */
public class ProgressCalculator {

  // Progress reporting thresholds
  private static final double PROGRESS_REPORT_THRESHOLD = 5.0; // Report every 5%
  private static final long MIN_STEP_INTERVAL = 10; // Minimum steps between reports
  private static final double MIN_PERCENTAGE_INTERVAL = 1.0; // Minimum percentage between reports

  private final long totalStepsPerReplicate;
  private final int totalReplicates;
  private final long totalStepsAcrossReplicates;

  // Tracking state
  private long lastReportedStep = -1;
  private double lastReportedPercentage = -1.0;
  private int currentReplicate = 1;
  private long totalCompletedSteps = 0;

  /**
   * Creates a new ProgressCalculator for tracking simulation progress.
   *
   * @param totalStepsPerReplicate The total number of steps per replicate
   * @param totalReplicates The total number of replicates to execute
   */
  public ProgressCalculator(long totalStepsPerReplicate, int totalReplicates) {
    if (totalStepsPerReplicate <= 0) {
      throw new IllegalArgumentException("Total steps per replicate must be positive");
    }
    if (totalReplicates <= 0) {
      throw new IllegalArgumentException("Total replicates must be positive");
    }

    this.totalStepsPerReplicate = totalStepsPerReplicate;
    this.totalReplicates = totalReplicates;
    this.totalStepsAcrossReplicates = totalStepsPerReplicate * totalReplicates;
  }

  /**
   * Updates progress based on a step update from the current replicate.
   *
   * <p>This method calculates the current progress percentage and determines
   * whether a progress update should be reported based on filtering thresholds.</p>
   *
   * @param currentStepInReplicate The current step within the active replicate (0-based)
   * @return ProgressUpdate indicating whether to report and what message to show
   */
  public ProgressUpdate updateStep(long currentStepInReplicate) {
    // Calculate cumulative progress across all replicates
    long stepsFromCompletedReplicates = (currentReplicate - 1) * totalStepsPerReplicate;
    long totalCurrentSteps = stepsFromCompletedReplicates + currentStepInReplicate;
    
    // Calculate percentage (0-100%)
    double currentPercentage = (double) totalCurrentSteps / totalStepsAcrossReplicates * 100.0;

    // Determine if we should report this progress update
    boolean shouldReport = shouldReportProgress(currentPercentage, currentStepInReplicate);

    if (shouldReport) {
      String message = formatProgressMessage(currentPercentage, currentStepInReplicate, 
          currentReplicate, false);
      lastReportedStep = currentStepInReplicate;
      lastReportedPercentage = currentPercentage;
      return new ProgressUpdate(shouldReport, currentPercentage, message);
    } else {
      return new ProgressUpdate(shouldReport, currentPercentage, null);
    }
  }

  /**
   * Updates progress when a replicate is completed.
   *
   * <p>This method handles replicate completion events and updates internal tracking
   * to prepare for the next replicate. It always generates a report for replicate
   * completion milestones.</p>
   *
   * @param completedReplicateNumber The replicate number that just completed
   * @return ProgressUpdate with a completion message
   */
  public ProgressUpdate updateReplicateCompleted(int completedReplicateNumber) {
    // Update tracking for the completed replicate
    totalCompletedSteps = completedReplicateNumber * totalStepsPerReplicate;
    currentReplicate = completedReplicateNumber + 1;

    // Calculate percentage at completion of this replicate
    double currentPercentage = (double) totalCompletedSteps / totalStepsAcrossReplicates * 100.0;

    // Generate completion message
    String message;
    if (totalReplicates == 1) {
      message = String.format("Progress: %.1f%% - Simulation completed", currentPercentage);
    } else {
      message = String.format("Progress: %.1f%% - Replicate %d/%d completed", 
          currentPercentage, completedReplicateNumber, totalReplicates);
    }

    // Reset progress tracking for next replicate
    lastReportedStep = -1;
    lastReportedPercentage = currentPercentage;

    return new ProgressUpdate(true, currentPercentage, message);
  }

  /**
   * Determines whether progress should be reported based on filtering thresholds.
   *
   * <p>This method implements intelligent filtering to reduce verbose output while
   * ensuring important progress milestones are reported. It considers both percentage
   * thresholds and step intervals.</p>
   *
   * @param currentPercentage The current progress percentage
   * @param currentStep The current step within the replicate
   * @return true if progress should be reported, false otherwise
   */
  private boolean shouldReportProgress(double currentPercentage, long currentStep) {
    // Always report the first progress update
    if (lastReportedPercentage < 0) {
      return true;
    }

    // Report on significant percentage milestones (every 5%)
    if (currentPercentage - lastReportedPercentage >= PROGRESS_REPORT_THRESHOLD) {
      return true;
    }

    // For long-running simulations, report on step intervals with minimum percentage change
    if (currentStep - lastReportedStep >= MIN_STEP_INTERVAL 
        && currentPercentage - lastReportedPercentage >= MIN_PERCENTAGE_INTERVAL) {
      return true;
    }

    return false;
  }

  /**
   * Formats a progress message consistent with the HTML editor experience.
   *
   * <p>This method creates informative progress messages that include percentage,
   * current step, total steps, and replicate information when applicable.</p>
   *
   * @param percentage The current progress percentage
   * @param currentStepInReplicate The current step within the replicate
   * @param currentReplicate The current replicate number
   * @param isReplicateComplete Whether this is a replicate completion message
   * @return Formatted progress message string
   */
  private String formatProgressMessage(double percentage, long currentStepInReplicate, 
                                      int currentReplicate, boolean isReplicateComplete) {
    if (totalReplicates == 1) {
      // Single replicate: "Progress: 45.2% (step 123/500)"
      return String.format("Progress: %.1f%% (step %d/%d)", 
          percentage, currentStepInReplicate, totalStepsPerReplicate);
    } else {
      // Multiple replicates: "Progress: 45.2% (step 123/500, replicate 2/10)"
      String base = String.format("Progress: %.1f%% (step %d/%d, replicate %d/%d)",
          percentage, currentStepInReplicate, totalStepsPerReplicate, 
          currentReplicate, totalReplicates);
      
      if (isReplicateComplete) {
        base += " - Replicate " + currentReplicate + " completed";
      }
      
      return base;
    }
  }

  /**
   * Container class for progress update information.
   *
   * <p>This class encapsulates the result of a progress calculation, including
   * whether the update should be reported and the formatted message to display.</p>
   */
  public static class ProgressUpdate {
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
      return String.format("ProgressUpdate{shouldReport=%s, percentage=%.1f, message='%s'}",
          shouldReport, percentage, message);
    }
  }
}