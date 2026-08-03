/**
 * Description of a single conformance test run.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.conformance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Description of a single conformance test run.
 *
 * <p>A test is discovered either by walking {@code josh-tests} or by reading the documentation
 * manifest, so this record is shared by {@link JoshConformanceTest} and {@link DocsManifest} rather
 * than nested inside the runner.</p>
 */
class TestInfo {

  /** Seed used when nothing declares one, matching what the runner passed before the manifest. */
  static final int DEFAULT_SEED = 42;

  final String name;
  final Path path;
  final String simulationName;
  final int seed;
  final String priority;

  /**
   * Constructs a TestInfo instance.
   *
   * @param name the test identifier, which is what lands in the JUnit XML
   * @param path the path to the model to run
   * @param simulationName the simulation name to run
   * @param seed the random seed to run with
   * @param priority the declared priority, or null when the file declares none
   */
  TestInfo(String name, Path path, String simulationName, int seed, String priority) {
    this.name = name;
    this.path = path;
    this.simulationName = simulationName;
    this.seed = seed;
    this.priority = priority;
  }

  /**
   * Creates a TestInfo from a file path, reading its metadata header.
   *
   * @param path the path to parse
   * @return the TestInfo instance
   */
  static TestInfo fromPath(Path path) {
    try {
      TestMetadata metadata = TestMetadata.parse(path);
      String name = path.getFileName().toString().replace(".josh", "");
      return new TestInfo(
          name, path, extractSimulationName(path), DEFAULT_SEED, metadata.priority);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse test: " + path, e);
    }
  }

  /**
   * Creates a TestInfo from a manifest entry.
   *
   * <p>The manifest leaves {@code simulation} null unless an author pinned one, because only the
   * engine knows what a file declares. That lookup happens here, so a manifest-driven run and a
   * filesystem-driven run resolve the name the same way.</p>
   *
   * @param name the unit id, which the harvester keeps equal to the filename stem for tests
   * @param path the path to the model to run
   * @param simulationName the declared simulation name, or null to read it from the file
   * @param seed the random seed to run with
   * @param priority the declared priority, or null when the unit declares none
   * @return the TestInfo instance
   */
  static TestInfo fromManifest(
      String name, Path path, String simulationName, int seed, String priority) {
    try {
      String resolved =
          simulationName == null ? extractSimulationName(path) : simulationName;
      return new TestInfo(name, path, resolved, seed, priority);
    } catch (Exception e) {
      throw new RuntimeException("Failed to resolve simulation for manifest unit: " + name, e);
    }
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Extracts the simulation name from a Josh file.
   *
   * @param file the file to parse
   * @return the simulation name
   * @throws Exception if no simulation is found
   */
  private static String extractSimulationName(Path file) throws Exception {
    try (Stream<String> lines = Files.lines(file)) {
      return lines
          .filter(line -> line.trim().startsWith("start simulation"))
          .findFirst()
          .map(line -> line.split("\\s+")[2])
          .orElseThrow(() -> new IllegalStateException(
              "No simulation found in " + file));
    }
  }
}
