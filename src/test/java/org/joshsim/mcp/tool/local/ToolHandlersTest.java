/**
 * Tests for ToolHandlers argument helpers.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp.tool.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.mcp.tool.local.ToolHandlers.MissingArgument;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ToolHandlers#requireString}.
 *
 * <p>Covers the scalar-coercion behavior that lets clients send a bare number (e.g. a GeoTIFF band
 * index) for a string-typed argument.</p>
 */
class ToolHandlersTest {

  @Test
  void returnsStringValue() {
    Map<String, Object> args = new HashMap<>();
    args.put("script", "model.josh");
    assertEquals("model.josh", ToolHandlers.requireString(args, "script"));
  }

  @Test
  void coercesIntegerToString() {
    Map<String, Object> args = new HashMap<>();
    args.put("variable", 0);
    assertEquals("0", ToolHandlers.requireString(args, "variable"));
  }

  @Test
  void coercesDoubleAndBooleanToString() {
    Map<String, Object> args = new HashMap<>();
    args.put("variable", 2.5);
    args.put("flag", true);
    assertEquals("2.5", ToolHandlers.requireString(args, "variable"));
    assertEquals("true", ToolHandlers.requireString(args, "flag"));
  }

  @Test
  void throwsOnMissingArgument() {
    Map<String, Object> args = new HashMap<>();
    MissingArgument ex =
        assertThrows(MissingArgument.class, () -> ToolHandlers.requireString(args, "script"));
    assertEquals("script", ex.getKey());
    assertTrue(ex.getMessage().contains("script"));
  }

  @Test
  void throwsOnBlankString() {
    Map<String, Object> args = new HashMap<>();
    args.put("script", "   ");
    assertThrows(MissingArgument.class, () -> ToolHandlers.requireString(args, "script"));
  }
}
