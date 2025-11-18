/**
 * Parameterized JUnit test that discovers and runs all Josh conformance tests.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.conformance;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * Parameterized JUnit test that discovers and runs all Josh conformance tests.
 *
 * <p>Each .josh file with assertions is a self-validating test.
 * This runner simply executes them and checks the exit code.</p>
 */
@ExtendWith(PerformanceTracker.class)
class JoshConformanceTest {

  private static final String JOSH_JAR = "build/libs/joshsim-fat.jar";
  private static final Path TEST_ROOT = Paths.get("josh-tests");

  /**
   * Runs a conformance test for all discovered Josh test files.
   *
   * @param test the test info containing path and metadata
   * @throws Exception if test execution fails
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("discoverAllTests")
  void runConformanceTest(TestInfo test) throws Exception {
    runJoshTest(test);
  }

  /**
   * Runs a conformance test for critical-priority Josh test files only.
   *
   * @param test the test info containing path and metadata
   * @throws Exception if test execution fails
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("discoverCriticalTests")
  @Tag("critical")
  void runCriticalTest(TestInfo test) throws Exception {
    runJoshTest(test);
  }

  /**
   * Discovers all Josh test files in josh-tests directory.
   *
   * @return stream of test info objects
   * @throws Exception if directory cannot be traversed
   */
  static Stream<TestInfo> discoverAllTests() throws Exception {
    if (!Files.exists(TEST_ROOT)) {
      return Stream.empty();
    }
    
    return Files.walk(TEST_ROOT)
        .filter(p -> p.getFileName().toString().startsWith("test_"))
        .filter(p -> p.toString().endsWith(".josh"))
        .sorted()
        .map(TestInfo::fromPath);
  }

  /**
   * Discovers only tests tagged as critical priority.
   *
   * @return stream of critical test info objects
   * @throws Exception if directory cannot be traversed
   */
  static Stream<TestInfo> discoverCriticalTests() throws Exception {
    return discoverAllTests()
        .filter(t -> "critical".equals(t.metadata.priority));
  }

  /**
   * Runs a single Josh test by invoking the jar.
   *
   * @param test the test to run
   * @throws Exception if test execution fails
   */
  private void runJoshTest(TestInfo test) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(
        "java", "-jar", JOSH_JAR,
        "run", test.path.toString(), test.simulationName,
        "--seed", "42"
    );

    pb.redirectErrorStream(true);
    Process process = pb.start();

    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }
    }

    int exitCode = process.waitFor();

    if (exitCode != 0) {
      fail(String.format(
          "Test failed: %s\n\nOutput:\n%s",
          test.path.getFileName(),
          output.toString()
      ));
    }
  }

  /**
   * Holds test metadata and path information.
   */
  static class TestInfo {
    final Path path;
    final String simulationName;
    final TestMetadata metadata;

    /**
     * Constructs a TestInfo instance.
     *
     * @param path the path to the test file
     * @param simulationName the simulation name to run
     * @param metadata the parsed test metadata
     */
    TestInfo(Path path, String simulationName, TestMetadata metadata) {
      this.path = path;
      this.simulationName = simulationName;
      this.metadata = metadata;
    }

    /**
     * Creates a TestInfo from a file path.
     *
     * @param path the path to parse
     * @return the TestInfo instance
     */
    static TestInfo fromPath(Path path) {
      try {
        String simulationName = extractSimulationName(path);
        TestMetadata metadata = TestMetadata.parse(path);
        return new TestInfo(path, simulationName, metadata);
      } catch (Exception e) {
        throw new RuntimeException("Failed to parse test: " + path, e);
      }
    }

    @Override
    public String toString() {
      return path.getFileName().toString().replace(".josh", "");
    }

    /**
     * Extracts the simulation name from a Josh file.
     *
     * @param file the file to parse
     * @return the simulation name
     * @throws Exception if no simulation is found
     */
    private static String extractSimulationName(Path file) throws Exception {
      return Files.lines(file)
          .filter(line -> line.trim().startsWith("start simulation"))
          .findFirst()
          .map(line -> line.split("\\s+")[2])
          .orElseThrow(() -> new IllegalStateException(
              "No simulation found in " + file));
    }
  }
}
