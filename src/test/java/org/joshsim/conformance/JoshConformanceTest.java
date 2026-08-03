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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 *
 * <p>Discovery reads the documentation manifest first, so a model authored under {@code docs/src}
 * with {@code assert: true} runs here too, with the simulation and seed it declares. The
 * {@code josh-tests} walk stays as a fallback and fills in anything the manifest does not name, so
 * the suite is unchanged on a checkout that has never run the docs builder.</p>
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
   * Discovers every conformance test, from the manifest and from the filesystem.
   *
   * <p>The manifest wins on a name collision, since it is the half that knows about declared
   * simulations, seeds, and export overlays.</p>
   *
   * @return stream of test info objects, ordered by name
   * @throws Exception if the manifest cannot be read or the directory cannot be traversed
   */
  static Stream<TestInfo> discoverAllTests() throws Exception {
    return discover(DocsManifest.DEFAULT_PATH);
  }

  /**
   * Discovers tests from a named manifest, merged with the filesystem walk.
   *
   * @param manifestPath the manifest to read, which need not exist
   * @return stream of test info objects, ordered by name
   * @throws Exception if the manifest cannot be read or the directory cannot be traversed
   */
  static Stream<TestInfo> discover(Path manifestPath) throws Exception {
    Map<String, TestInfo> byName = new LinkedHashMap<>();
    for (TestInfo test : DocsManifest.readTests(manifestPath)) {
      byName.put(test.name, test);
    }
    for (TestInfo test : walkTestRoot()) {
      byName.putIfAbsent(test.name, test);
    }
    return byName.values().stream().sorted(Comparator.comparing(test -> test.name));
  }

  /**
   * Discovers only tests tagged as critical priority.
   *
   * @return stream of critical test info objects
   * @throws Exception if directory cannot be traversed
   */
  static Stream<TestInfo> discoverCriticalTests() throws Exception {
    return discoverAllTests()
        .filter(t -> "critical".equals(t.priority));
  }

  /**
   * Walks the test root for Josh files named by the conformance convention.
   *
   * @return the tests found, or an empty list when the root is absent
   * @throws Exception if directory cannot be traversed
   */
  private static List<TestInfo> walkTestRoot() throws Exception {
    if (!Files.exists(TEST_ROOT)) {
      return List.of();
    }

    try (Stream<Path> paths = Files.walk(TEST_ROOT)) {
      return paths
          .filter(p -> p.getFileName().toString().startsWith("test_"))
          .filter(p -> p.toString().endsWith(".josh"))
          .sorted()
          .map(TestInfo::fromPath)
          .toList();
    }
  }

  /**
   * Runs a single Josh test by invoking the jar.
   *
   * @param test the test to run
   * @throws Exception if test execution fails
   */
  private void runJoshTest(TestInfo test) throws Exception {
    List<String> args = List.of(
        "java", "-jar", JOSH_JAR,
        "run", test.path.toString(), test.simulationName,
        "--seed", String.valueOf(test.seed)
    );
    ProcessBuilder pb = new ProcessBuilder(args);

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
}
