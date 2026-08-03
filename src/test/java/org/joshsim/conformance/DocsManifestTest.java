/**
 * Tests for DocsManifest.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the reader that turns docs-manifest.json into conformance test descriptions.
 */
class DocsManifestTest {

  @TempDir
  Path tempDir;

  /**
   * Writes a manifest containing the given unit bodies.
   *
   * @param units JSON object literals, already comma-free
   * @return the path the manifest was written to
   * @throws IOException if the file cannot be written
   */
  private Path writeManifest(String... units) throws IOException {
    String manifest = String.format(
        "{\"generator\": \"joshdocs\", \"schemaVersion\": 1, \"root\": \"%s\", \"units\": [%s]}",
        tempDir.toString().replace("\\", "\\\\"),
        String.join(",", units));
    Path path = tempDir.resolve("docs-manifest.json");
    Files.writeString(path, manifest);
    return path;
  }

  /**
   * Writes a minimal model declaring the given simulation.
   *
   * @param name the file name to write
   * @param simulation the simulation the file declares
   * @throws IOException if the file cannot be written
   */
  private void writeModel(String name, String simulation) throws IOException {
    Files.writeString(tempDir.resolve(name), "start simulation " + simulation + "\n");
  }

  @Test
  void testNoManifestYieldsNoTests() throws Exception {
    assertTrue(DocsManifest.readTests(tempDir.resolve("absent.json")).isEmpty());
  }

  @Test
  void testReadsAnAssertingUnit() throws Exception {
    writeModel("wind.josh", "Main");
    Path manifest = writeManifest("""
        {"id": "wind-dispersal", "source": "wind.josh", "assertions": true,
         "simulation": "Main", "seed": 7, "priority": "critical",
         "status": "active", "expect": "valid"}
        """);

    List<TestInfo> tests = DocsManifest.readTests(manifest);

    assertEquals(1, tests.size());
    assertEquals("wind-dispersal", tests.get(0).name);
    assertEquals("Main", tests.get(0).simulationName);
    assertEquals(7, tests.get(0).seed);
    assertEquals("critical", tests.get(0).priority);
    assertEquals(tempDir.resolve("wind.josh"), tests.get(0).path);
  }

  @Test
  void testResolvesTheSimulationWhenTheManifestLeavesItNull() throws Exception {
    writeModel("test_thing.josh", "ThingSimulation");
    Path manifest = writeManifest("""
        {"id": "test_thing", "source": "test_thing.josh", "assertions": true,
         "simulation": null, "seed": 42, "status": "active", "expect": "valid"}
        """);

    assertEquals("ThingSimulation", DocsManifest.readTests(manifest).get(0).simulationName);
  }

  @Test
  void testPrefersTheEmittedRunnableOverTheAuthoredSource() throws Exception {
    writeModel("authored.josh", "Main");
    writeModel("emitted.josh", "Main");
    Path manifest = writeManifest("""
        {"id": "overlaid", "source": "authored.josh", "runnableFile": "emitted.josh",
         "assertions": true, "simulation": "Main", "seed": 42,
         "status": "active", "expect": "valid"}
        """);

    assertEquals(tempDir.resolve("emitted.josh"), DocsManifest.readTests(manifest).get(0).path);
  }

  @Test
  void testSkipsUnitsThatDoNotAssert() throws Exception {
    writeModel("prose.josh", "Main");
    Path manifest = writeManifest("""
        {"id": "prose-only", "source": "prose.josh", "assertions": false,
         "seed": 42, "status": "active", "expect": "valid"}
        """);

    assertTrue(DocsManifest.readTests(manifest).isEmpty());
  }

  @Test
  void testSkipsReservedUnits() throws Exception {
    writeModel("reserved.josh", "Main");
    Path manifest = writeManifest("""
        {"id": "reserved-feature", "source": "reserved.josh", "assertions": true,
         "seed": 42, "status": "reserved", "reason": "not implemented", "expect": "valid"}
        """);

    assertTrue(DocsManifest.readTests(manifest).isEmpty());
  }

  @Test
  void testSkipsUnitsExpectedToFailParsing() throws Exception {
    writeModel("broken.josh", "Main");
    Path manifest = writeManifest("""
        {"id": "syntax-error", "source": "broken.josh", "assertions": true,
         "seed": 42, "status": "active", "expect": "parse-error"}
        """);

    assertTrue(DocsManifest.readTests(manifest).isEmpty());
  }

  @Test
  void testRejectsManifestFromDifferentSchema() throws Exception {
    Path path = tempDir.resolve("docs-manifest.json");
    Files.writeString(path, "{\"schemaVersion\": 99, \"units\": []}");

    IOException failure = assertThrows(IOException.class, () -> DocsManifest.readTests(path));
    assertTrue(failure.getMessage().contains("harvestDocs"));
  }
}
