/**
 * Reader for the documentation manifest the docs builder writes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.conformance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader for the documentation manifest the docs builder writes.
 *
 * <p>{@code build/docs/docs-manifest.json} is the contract between the Python harvester and this
 * runner. It names every documentation unit, including models authored under {@code docs/src} that
 * a {@code test_} filename walk cannot see, and it carries the simulation name and seed each unit
 * runs with rather than leaving both to be guessed at the call site.</p>
 *
 * <p>The manifest is optional. A checkout without the docs builder installed still runs the suite
 * from the filesystem, so working on the engine never requires a Python toolchain. A manifest that
 * is present but unreadable is an error rather than a silent fallback: it means the builder is
 * broken, and the whole point of this pipeline is that nothing goes unchecked quietly.</p>
 */
final class DocsManifest {

  /** Where the harvester writes the manifest by default. */
  static final Path DEFAULT_PATH = Paths.get("build", "docs", "docs-manifest.json");

  /** Manifest layout this reader understands, matching {@code joshdocs.SCHEMA_VERSION}. */
  static final int SUPPORTED_SCHEMA_VERSION = 1;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Prevents instantiation of this utility class. */
  private DocsManifest() {}

  /**
   * Reads the units a manifest declares as executable conformance tests.
   *
   * <p>A unit runs when it asserts, is active rather than reserved, and is expected to be valid
   * Josh. Units that only illustrate syntax, and units held back as reserved features, are
   * documented but not executed.</p>
   *
   * @param manifestPath the manifest to read
   * @return the tests it declares, or an empty list when no manifest is present
   * @throws IOException if the manifest exists but cannot be read or understood
   */
  static List<TestInfo> readTests(Path manifestPath) throws IOException {
    if (!Files.isRegularFile(manifestPath)) {
      return List.of();
    }

    JsonNode manifest = MAPPER.readTree(manifestPath.toFile());
    int version = manifest.path("schemaVersion").asInt(0);
    if (version != SUPPORTED_SCHEMA_VERSION) {
      throw new IOException(String.format(
          "%s declares schemaVersion %d but this runner understands %d: re-run './gradlew "
              + "harvestDocs' to regenerate it",
          manifestPath, version, SUPPORTED_SCHEMA_VERSION));
    }

    Path root = Paths.get(text(manifest, "root", "."));
    List<TestInfo> tests = new ArrayList<>();
    for (JsonNode unit : manifest.path("units")) {
      if (isExecutable(unit)) {
        tests.add(toTestInfo(unit, root, manifestPath));
      }
    }
    return tests;
  }

  /**
   * Decides whether a manifest unit should be run as a conformance test.
   *
   * @param unit the manifest entry
   * @return true when the unit asserts and is expected to run cleanly
   */
  private static boolean isExecutable(JsonNode unit) {
    return unit.path("assertions").asBoolean(false)
        && "active".equals(text(unit, "status", "active"))
        && "valid".equals(text(unit, "expect", "valid"));
  }

  /**
   * Converts one manifest unit into a runnable test description.
   *
   * @param unit the manifest entry
   * @param root the directory manifest paths are relative to
   * @param manifestPath the manifest being read, named in errors
   * @return the test description
   * @throws IOException if the unit names no model to run
   */
  private static TestInfo toTestInfo(JsonNode unit, Path root, Path manifestPath)
      throws IOException {
    String id = text(unit, "id", null);

    // The emitted copy carries the export overlay, so it is what actually runs when one exists.
    String relative = text(unit, "runnableFile", text(unit, "source", null));
    if (id == null || relative == null) {
      throw new IOException(
          manifestPath + " has a unit without an id or a source: " + unit);
    }

    return TestInfo.fromManifest(
        id,
        root.resolve(relative).normalize(),
        text(unit, "simulation", null),
        unit.path("seed").asInt(TestInfo.DEFAULT_SEED),
        text(unit, "priority", null));
  }

  /**
   * Reads a string field, treating an absent field and a JSON null the same way.
   *
   * @param node the object to read from
   * @param field the field name
   * @param fallback the value to use when the field is absent or null
   * @return the field's text, or the fallback
   */
  private static String text(JsonNode node, String field, String fallback) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? fallback : value.asText();
  }
}
