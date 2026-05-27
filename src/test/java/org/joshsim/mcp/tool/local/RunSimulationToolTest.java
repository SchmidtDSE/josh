/**
 * Tests for RunSimulationTool's data-argument parsing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp.tool.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RunSimulationTool#parseDataFiles}.
 *
 * <p>Covers the optional {@code data} object: resolution to absolute paths and the structural
 * validation that turns malformed input into an MCP error result.</p>
 */
class RunSimulationToolTest {

  @Test
  void absentDataYieldsEmptyMap() {
    assertTrue(RunSimulationTool.parseDataFiles(null).isEmpty());
  }

  @Test
  void resolvesValuesToAbsolutePaths() {
    Map<String, Object> data = new HashMap<>();
    data.put("temperature.jshd", "data/t.jshd");
    Map<String, Path> resolved = RunSimulationTool.parseDataFiles(data);

    assertEquals(1, resolved.size());
    Path path = resolved.get("temperature.jshd");
    assertTrue(path.isAbsolute(), "value should be resolved to an absolute path");
    assertTrue(path.endsWith(Path.of("data", "t.jshd")),
        "resolved path should preserve the supplied relative path tail, got: " + path);
  }

  @Test
  void rejectsNonObjectData() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> RunSimulationTool.parseDataFiles("not-an-object"));
    assertTrue(ex.getMessage().contains("data"));
  }

  @Test
  void rejectsBlankName() {
    Map<String, Object> data = new HashMap<>();
    data.put("   ", "data/t.jshd");
    assertThrows(IllegalArgumentException.class, () -> RunSimulationTool.parseDataFiles(data));
  }

  @Test
  void rejectsNonStringPath() {
    Map<String, Object> data = new HashMap<>();
    data.put("temperature.jshd", 42);
    assertThrows(IllegalArgumentException.class, () -> RunSimulationTool.parseDataFiles(data));
  }

  @Test
  void rejectsBlankPath() {
    Map<String, Object> data = new HashMap<>();
    data.put("temperature.jshd", "  ");
    assertThrows(IllegalArgumentException.class, () -> RunSimulationTool.parseDataFiles(data));
  }
}
