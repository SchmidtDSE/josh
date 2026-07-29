/**
 * Tests for how the conformance runner merges manifest units with the filesystem walk.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for how the conformance runner merges manifest units with the filesystem walk.
 *
 * <p>These run against the real {@code josh-tests} tree, because the property worth protecting is
 * that adding the manifest cannot drop or duplicate a test that the walk already found.</p>
 */
class ConformanceDiscoveryTest {

  /** A conformance test that exists in the tree, used as the unit the manifest overrides. */
  private static final String KNOWN_TEST = "test_entity_update_basic";

  @TempDir
  Path tempDir;

  /**
   * Indexes discovered tests by name.
   *
   * @param manifestPath the manifest to discover with
   * @return the discovered tests, keyed by name
   * @throws Exception if discovery fails
   */
  private Map<String, TestInfo> discoverByName(Path manifestPath) throws Exception {
    return JoshConformanceTest.discover(manifestPath)
        .collect(Collectors.toMap(test -> test.name, Function.identity()));
  }

  /**
   * Writes a manifest naming one unit.
   *
   * @param unit a JSON object literal for the unit
   * @return the path the manifest was written to
   * @throws IOException if the file cannot be written
   */
  private Path writeManifest(String unit) throws IOException {
    Path path = tempDir.resolve("docs-manifest.json");
    Files.writeString(
        path,
        "{\"schemaVersion\": 1, \"root\": \".\", \"units\": [" + unit + "]}");
    return path;
  }

  @Test
  void testWalkStillFindsEveryTestWhenNoManifestExists() throws Exception {
    Map<String, TestInfo> found = discoverByName(tempDir.resolve("absent.json"));

    List<Path> onDisk;
    try (var paths = Files.walk(Path.of("josh-tests"))) {
      onDisk = paths
          .filter(p -> p.getFileName().toString().startsWith("test_"))
          .filter(p -> p.toString().endsWith(".josh"))
          .toList();
    }

    assertEquals(onDisk.size(), found.size());
    assertNotNull(found.get(KNOWN_TEST));
    assertEquals(TestInfo.DEFAULT_SEED, found.get(KNOWN_TEST).seed);
  }

  @Test
  void testManifestOverridesTheWalkWithoutChangingTheCount() throws Exception {
    int walkOnly = discoverByName(tempDir.resolve("absent.json")).size();

    Path manifest = writeManifest(String.format("""
        {"id": "%s",
         "source": "josh-tests/conformance/core/entity_overwrite/%s.josh",
         "assertions": true, "simulation": "EntityUpdateBasic", "seed": 99,
         "priority": "critical", "status": "active", "expect": "valid"}
        """, KNOWN_TEST, KNOWN_TEST));

    Map<String, TestInfo> found = discoverByName(manifest);

    // The unit is named by both halves, so it must appear once, carrying the manifest's values.
    assertEquals(walkOnly, found.size());
    assertEquals(99, found.get(KNOWN_TEST).seed);
    assertEquals("EntityUpdateBasic", found.get(KNOWN_TEST).simulationName);
  }

  @Test
  void testManifestAddsUnitsTheWalkCannotSee() throws Exception {
    Path authored = tempDir.resolve("wind-dispersal.josh");
    Files.writeString(authored, "start simulation Main\n");

    int walkOnly = discoverByName(tempDir.resolve("absent.json")).size();
    Path manifest = writeManifest(String.format("""
        {"id": "wind-dispersal", "source": "%s", "assertions": true,
         "simulation": "Main", "seed": 42, "status": "active", "expect": "valid"}
        """, authored.toString().replace("\\", "\\\\")));

    Map<String, TestInfo> found = discoverByName(manifest);

    assertEquals(walkOnly + 1, found.size());
    assertTrue(found.containsKey("wind-dispersal"));
    assertEquals("Main", found.get("wind-dispersal").simulationName);
  }
}
