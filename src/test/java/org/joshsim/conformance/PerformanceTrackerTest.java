/**
 * Tests for PerformanceTracker.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.mockito.Mockito;


/**
 * Tests for the PerformanceTracker JUnit 5 extension.
 */
class PerformanceTrackerTest {

  private static final Path HISTORY_FILE =
      Paths.get("build/test-results/performance-history.csv");

  private PerformanceTracker tracker;
  private ExtensionContext mockContext;
  private Store mockStore;
  private ByteArrayOutputStream stdout;
  private ByteArrayOutputStream stderr;
  private PrintStream originalStdout;
  private PrintStream originalStderr;

  @BeforeEach
  void setUp() throws IOException {
    tracker = new PerformanceTracker();

    mockContext = Mockito.mock(ExtensionContext.class);
    mockStore = Mockito.mock(Store.class);

    Mockito.when(mockContext.getStore(Namespace.GLOBAL)).thenReturn(mockStore);
    Mockito.when(mockContext.getDisplayName()).thenReturn("test_method");
    Mockito.when(mockContext.getExecutionException()).thenReturn(Optional.empty());

    if (Files.exists(HISTORY_FILE)) {
      Files.delete(HISTORY_FILE);
    }

    originalStdout = System.out;
    originalStderr = System.err;
    stdout = new ByteArrayOutputStream();
    stderr = new ByteArrayOutputStream();
    System.setOut(new PrintStream(stdout));
    System.setErr(new PrintStream(stderr));
  }

  @AfterEach
  void tearDown() throws IOException {
    System.setOut(originalStdout);
    System.setErr(originalStderr);

    if (Files.exists(HISTORY_FILE)) {
      Files.delete(HISTORY_FILE);
    }
  }

  @Test
  void testCsvFileCreation() throws Exception {
    Mockito.when(mockStore.get("startTime", Long.class))
        .thenReturn(System.currentTimeMillis() - 100);

    tracker.beforeTestExecution(mockContext);
    tracker.afterTestExecution(mockContext);

    assertTrue(Files.exists(HISTORY_FILE));

    List<String> lines = Files.readAllLines(HISTORY_FILE);
    assertEquals(2, lines.size());
    assertEquals("test_name,timestamp,duration_ms,passed", lines.get(0));
    assertTrue(lines.get(1).startsWith("test_method,"));
    assertTrue(lines.get(1).contains(",true"));
  }

  @Test
  void testMultipleTestRecordings() throws Exception {
    for (int i = 0; i < 3; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 50);
      Mockito.when(mockContext.getDisplayName()).thenReturn("test_" + i);

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);
    }

    List<String> lines = Files.readAllLines(HISTORY_FILE);
    assertEquals(4, lines.size());
  }

  @Test
  void testRecordsFailedTests() throws Exception {
    Mockito.when(mockStore.get("startTime", Long.class))
        .thenReturn(System.currentTimeMillis() - 100);
    Mockito.when(mockContext.getExecutionException())
        .thenReturn(Optional.of(new RuntimeException("Test failed")));

    tracker.beforeTestExecution(mockContext);
    tracker.afterTestExecution(mockContext);

    List<String> lines = Files.readAllLines(HISTORY_FILE);
    assertTrue(lines.get(1).endsWith(",false"));
  }

  @Test
  void testNoRegressionWithInsufficientData() throws Exception {
    for (int i = 0; i < 5; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 100);

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);
    }

    String stderrOutput = stderr.toString();
    assertFalse(stderrOutput.contains("PERFORMANCE WARNING"));
  }

  @Test
  void testRegressionDetectionByPercentage() throws Exception {
    for (int i = 0; i < 10; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 100);

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);
    }

    stderr.reset();

    Mockito.when(mockStore.get("startTime", Long.class))
        .thenReturn(System.currentTimeMillis() - 200);

    tracker.beforeTestExecution(mockContext);
    tracker.afterTestExecution(mockContext);

    String stderrOutput = stderr.toString();
    assertTrue(stderrOutput.contains("PERFORMANCE WARNING"));
    assertTrue(stderrOutput.contains("test_method"));
    assertTrue(stderrOutput.contains("Slowdown:"));
  }

  @Test
  void testRegressionDetectionByZscore() throws Exception {
    for (int i = 0; i < 10; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 100);

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);
    }

    stderr.reset();

    Mockito.when(mockStore.get("startTime", Long.class))
        .thenReturn(System.currentTimeMillis() - 500);

    tracker.beforeTestExecution(mockContext);
    tracker.afterTestExecution(mockContext);

    String stderrOutput = stderr.toString();
    assertTrue(stderrOutput.contains("PERFORMANCE WARNING"));
    assertTrue(stderrOutput.contains("Z-score:"));
  }

  @Test
  void testImprovementDetection() throws Exception {
    for (int i = 0; i < 10; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 200);

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);
    }

    stdout.reset();

    Mockito.when(mockStore.get("startTime", Long.class))
        .thenReturn(System.currentTimeMillis() - 100);

    tracker.beforeTestExecution(mockContext);
    tracker.afterTestExecution(mockContext);

    String stdoutOutput = stdout.toString();
    assertTrue(stdoutOutput.contains("PERFORMANCE IMPROVEMENT"));
    assertTrue(stdoutOutput.contains("test_method"));
    assertTrue(stdoutOutput.contains("Speedup:"));
  }

  @Test
  void testOnlyPassedTestsInBaseline() throws Exception {
    for (int i = 0; i < 5; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 100);
      Mockito.when(mockContext.getExecutionException())
          .thenReturn(Optional.empty());

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);
    }

    for (int i = 0; i < 5; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 1000);
      Mockito.when(mockContext.getExecutionException())
          .thenReturn(Optional.of(new RuntimeException("Fail")));

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);
    }

    for (int i = 0; i < 5; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 100);
      Mockito.when(mockContext.getExecutionException())
          .thenReturn(Optional.empty());

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);
    }

    stderr.reset();

    Mockito.when(mockStore.get("startTime", Long.class))
        .thenReturn(System.currentTimeMillis() - 200);
    Mockito.when(mockContext.getExecutionException())
        .thenReturn(Optional.empty());

    tracker.beforeTestExecution(mockContext);
    tracker.afterTestExecution(mockContext);

    String stderrOutput = stderr.toString();
    assertTrue(stderrOutput.contains("PERFORMANCE WARNING"));
  }

  @Test
  void testHandlesZeroStdDevGracefully() throws Exception {
    for (int i = 0; i < 10; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 100);

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);

      Thread.sleep(1);
    }

    stderr.reset();

    Mockito.when(mockStore.get("startTime", Long.class))
        .thenReturn(System.currentTimeMillis() - 101);

    tracker.beforeTestExecution(mockContext);
    tracker.afterTestExecution(mockContext);

    String stderrOutput = stderr.toString();
    assertFalse(stderrOutput.contains("NaN"));
    assertFalse(stderrOutput.contains("Infinity"));
  }

  @Test
  void testDifferentTestsTrackedSeparately() throws Exception {
    for (int i = 0; i < 10; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 100);
      Mockito.when(mockContext.getDisplayName()).thenReturn("test_fast");

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);
    }

    for (int i = 0; i < 10; i++) {
      Mockito.when(mockStore.get("startTime", Long.class))
          .thenReturn(System.currentTimeMillis() - 500);
      Mockito.when(mockContext.getDisplayName()).thenReturn("test_slow");

      tracker.beforeTestExecution(mockContext);
      tracker.afterTestExecution(mockContext);
    }

    stderr.reset();

    Mockito.when(mockStore.get("startTime", Long.class))
        .thenReturn(System.currentTimeMillis() - 200);
    Mockito.when(mockContext.getDisplayName()).thenReturn("test_fast");

    tracker.beforeTestExecution(mockContext);
    tracker.afterTestExecution(mockContext);

    String stderrOutput = stderr.toString();
    assertTrue(stderrOutput.contains("test_fast"));
    assertFalse(stderrOutput.contains("test_slow"));
  }

  @Test
  void testCsvFormatCorrect() throws Exception {
    Mockito.when(mockStore.get("startTime", Long.class))
        .thenReturn(System.currentTimeMillis() - 100);

    tracker.beforeTestExecution(mockContext);
    tracker.afterTestExecution(mockContext);

    List<String> lines = Files.readAllLines(HISTORY_FILE);
    String dataLine = lines.get(1);
    String[] parts = dataLine.split(",");

    assertEquals(4, parts.length);
    assertEquals("test_method", parts[0]);
    assertTrue(parts[1].contains("T"));
    assertTrue(Long.parseLong(parts[2]) >= 0);
    assertEquals("true", parts[3]);
  }
}
