/**
 * Classpath loader for MCP tool input schemas.
 *
 * <p>Each tool's JSON Schema lives as a sibling resource under
 * {@code org/joshsim/mcp/tool/local/<tool_name>.schema.json}. This helper loads them as raw
 * strings for the MCP SDK's {@code Tool.Builder.inputSchema(McpJsonMapper, String)} method.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp.tool.local;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads MCP tool input schemas from classpath resources.
 *
 * <p>Schemas are colocated with the tool classes so the resource path mirrors the package path.
 * Keeping the JSON in dedicated files (rather than concatenated Java string literals) makes the
 * schemas readable, diff-friendly, and lets editors lint them as JSON.</p>
 */
public final class ToolSchemas {

  private static final String RESOURCE_PREFIX = "org/joshsim/mcp/tool/local/";

  private ToolSchemas() {
    // Static utility class
  }

  /**
   * Loads the input schema JSON for a tool by its MCP tool name.
   *
   * @param toolName the tool's MCP name (e.g. {@code "run_simulation"}); used as the base of the
   *     resource filename {@code <toolName>.schema.json}
   * @return the raw JSON Schema as a UTF-8 string
   * @throws IllegalStateException if the schema resource cannot be found or read
   */
  public static String load(String toolName) {
    String resourcePath = RESOURCE_PREFIX + toolName + ".schema.json";
    ClassLoader classLoader = ToolSchemas.class.getClassLoader();
    try (InputStream stream = classLoader.getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new IllegalStateException("Missing MCP tool schema resource: " + resourcePath);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read MCP tool schema: " + resourcePath, e);
    }
  }

}
