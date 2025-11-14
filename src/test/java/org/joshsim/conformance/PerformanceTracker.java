/**
 * JUnit 5 extension that tracks test execution times and detects performance regressions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.conformance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


/**
 * JUnit 5 extension that tracks test execution times and detects performance regressions.
 *
 * <p>Records all test durations to performance-history.csv and compares against baseline.
 * Warns (but doesn't fail) if performance degrades significantly.</p>
 */
public class PerformanceTracker implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

  private static final Path HISTORY_FILE =
      Paths.get("build/test-results/performance-history.csv");
  private static final int BASELINE_WINDOW = 10;
  private static final double REGRESSION_THRESHOLD = 0.30;
  private static final double IMPROVEMENT_THRESHOLD = 0.20;
  private static final double Z_SCORE_THRESHOLD = 2.0;

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    context.getStore(ExtensionContext.Namespace.GLOBAL)
        .put("startTime", System.currentTimeMillis());
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    Long startTime = context.getStore(ExtensionContext.Namespace.GLOBAL)
        .get("startTime", Long.class);

    long durationMs = (startTime != null) ? System.currentTimeMillis() - startTime : 0;

    String testName = context.getDisplayName();
    boolean passed = !context.getExecutionException().isPresent();

    recordPerformance(testName, durationMs, passed);

    if (passed && durationMs > 0) {
      checkPerformanceRegression(testName, durationMs);
    }
  }

  /**
   * Records test performance data to the CSV file.
   *
   * @param testName the name of the test
   * @param durationMs the duration in milliseconds
   * @param passed whether the test passed
   * @throws IOException if writing to the file fails
   */
  private void recordPerformance(String testName, long durationMs, boolean passed)
      throws IOException {
    Files.createDirectories(HISTORY_FILE.getParent());

    if (!Files.exists(HISTORY_FILE)) {
      Files.writeString(HISTORY_FILE,
          "test_name,timestamp,duration_ms,passed\n",
          StandardOpenOption.CREATE);
    }

    String record = String.format("%s,%s,%d,%b\n",
        testName,
        Instant.now().toString(),
        durationMs,
        passed
    );

    Files.writeString(HISTORY_FILE, record,
        StandardOpenOption.APPEND);
  }

  /**
   * Checks for performance regression by comparing current execution against baseline.
   *
   * @param testName the name of the test
   * @param currentMs the current execution time in milliseconds
   * @throws IOException if reading the history file fails
   */
  private void checkPerformanceRegression(String testName, long currentMs)
      throws IOException {
    if (!Files.exists(HISTORY_FILE)) {
      return;
    }

    List<Long> historicalDurations = Files.lines(HISTORY_FILE)
        .skip(1)
        .map(line -> line.split(","))
        .filter(parts -> parts[0].equals(testName))
        .filter(parts -> Boolean.parseBoolean(parts[3]))
        .map(parts -> Long.parseLong(parts[2]))
        .collect(Collectors.toList());

    if (historicalDurations.size() < BASELINE_WINDOW) {
      return;
    }

    List<Long> recentRuns = historicalDurations.subList(
        Math.max(0, historicalDurations.size() - BASELINE_WINDOW),
        historicalDurations.size()
    );

    double baselineMean = recentRuns.stream()
        .mapToLong(Long::longValue)
        .average()
        .orElse(0);

    double variance = recentRuns.stream()
        .mapToDouble(d -> Math.pow(d - baselineMean, 2))
        .average()
        .orElse(0);
    double stdDev = Math.sqrt(variance);

    double slowdown = (currentMs - baselineMean) / baselineMean;
    double zscore = stdDev > 0 ? (currentMs - baselineMean) / stdDev : 0;

    if (slowdown > REGRESSION_THRESHOLD || zscore > Z_SCORE_THRESHOLD) {
      System.err.println(String.format(
          "\n⚠️  PERFORMANCE WARNING: %s\n"
          + "   Current: %dms\n"
          + "   Baseline: %.0fms (±%.0fms)\n"
          + "   Slowdown: %.1f%% (threshold: %.0f%%)\n"
          + "   Z-score: %.2f (threshold: %.1f)\n",
          testName, currentMs, baselineMean, stdDev,
          slowdown * 100, REGRESSION_THRESHOLD * 100, zscore, Z_SCORE_THRESHOLD
      ));
    }

    if (slowdown < -IMPROVEMENT_THRESHOLD && recentRuns.size() >= BASELINE_WINDOW) {
      System.out.println(String.format(
          "\n✨ PERFORMANCE IMPROVEMENT: %s\n"
          + "   Current: %dms\n"
          + "   Baseline: %.0fms\n"
          + "   Speedup: %.1f%%\n",
          testName, currentMs, baselineMean, Math.abs(slowdown) * 100
      ));
    }
  }
}
