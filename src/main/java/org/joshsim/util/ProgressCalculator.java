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
 * <p>This class tracks simulation progress with per-replicate progress tracking (0-100%
 * within each replicate) and provides percentage-based progress calculations with intelligent
 * filtering to reduce verbose output. It formats progress messages consistently with the
 * HTML editor experience and supports progress resets between replicates.</p>
 */
public class ProgressCalculator {

  // Progress reporting thresholds
  private static final double PROGRESS_REPORT_THRESHOLD = 5.0; // Report every 5%
  private static final long MIN_STEP_INTERVAL = 10; // Minimum steps between reports
  private static final double MIN_PERCENTAGE_INTERVAL = 1.0; // Minimum percentage between reports

  private final long totalStepsPerReplicate;
  private final int totalReplicates;

  // Tracking state
  private long lastReportedStep = -1;
  private double lastReportedPercentage = -1.0;
  private int currentReplicate = 1;

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
    // Calculate per-replicate progress (0-100% within current replicate)
    double currentPercentage = (double) currentStepInReplicate / totalStepsPerReplicate * 100.0;

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
    // Per-replicate completion is always 100%
    double currentPercentage = 100.0;

    // Generate completion message
    String message = String.format(
        "Replicate %d/%d completed",
        completedReplicateNumber,
        totalReplicates);

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
    boolean isFirstUpdate = lastReportedPercentage < 0;
    if (isFirstUpdate) {
      return true;
    }

    // Report on significant percentage milestones (every 5%)
    double deltaPercentFromLastReport = currentPercentage - lastReportedPercentage;
    boolean hasMilestone = deltaPercentFromLastReport >= PROGRESS_REPORT_THRESHOLD;
    if (hasMilestone) {
      return true;
    }

    // For long-running simulations, report on step intervals with minimum percentage change
    long deltaStepFromLastReport = currentStep - lastReportedStep;
    boolean aboveMinStepReport = deltaStepFromLastReport >= MIN_STEP_INTERVAL;
    boolean aboveMinPercentReport = deltaPercentFromLastReport >= MIN_PERCENTAGE_INTERVAL;
    if (aboveMinStepReport && aboveMinPercentReport) {
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
      return String.format(
          "Progress: %.1f%% (step %d/%d)",
          percentage,
          currentStepInReplicate,
          totalStepsPerReplicate);
    } else {
      // Multiple replicates: "Replicate 2/10: Progress: 45.2% (step 123/500)"
      return String.format(
          "Replicate %d/%d: Progress: %.1f%% (step %d/%d)",
          currentReplicate,
          totalReplicates,
          percentage,
          currentStepInReplicate,
          totalStepsPerReplicate);
    }
  }

  /**
   * Resets progress tracking for the next replicate.
   *
   * <p>This method resets the internal progress tracking state to prepare for a new replicate.
   * It updates the current replicate number and resets the reporting thresholds, ensuring
   * that progress starts fresh at 0% for the new replicate.</p>
   *
   * @param nextReplicateNumber The replicate number starting next (1-based)
   * @throws IllegalArgumentException if nextReplicateNumber is invalid
   */
  public void resetForNextReplicate(int nextReplicateNumber) {
    if (nextReplicateNumber < 1 || nextReplicateNumber > totalReplicates) {
      throw new IllegalArgumentException("Invalid replicate number: " + nextReplicateNumber
          + " (must be between 1 and " + totalReplicates + ")");
    }

    this.currentReplicate = nextReplicateNumber;
    this.lastReportedStep = -1;
    this.lastReportedPercentage = -1.0;
  }

}
