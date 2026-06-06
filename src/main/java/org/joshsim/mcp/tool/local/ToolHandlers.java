/**
 * Shared helpers for MCP tool call handlers.
 *
 * <p>Provides small reusable building blocks (error result construction, required-string argument
 * extraction) so each tool's call handler can focus on its tool-specific logic.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp.tool.local;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.Map;

/**
 * Static helpers used by MCP tool call handlers.
 */
public final class ToolHandlers {

  private ToolHandlers() {
    // Static utility class
  }

  /**
   * Builds a {@link CallToolResult} representing a recoverable, LLM-visible error.
   *
   * <p>Used for missing or malformed arguments and for backend failures that the LLM client can
   * sensibly react to. Genuinely unexpected exceptions should propagate so the SDK maps them to
   * JSON-RPC errors instead.</p>
   *
   * @param message human-readable error description
   * @return a {@link CallToolResult} with the given message and {@code isError} set to true
   */
  public static CallToolResult errorResult(String message) {
    return CallToolResult.builder()
        .addTextContent(message)
        .isError(Boolean.TRUE)
        .build();
  }

  /**
   * Extracts a required non-blank string argument from a tool call request's argument map.
   *
   * <p>Returns the string value when present and non-blank. Numeric and boolean scalars are
   * coerced to their string form: although the tool schemas declare these arguments as strings,
   * clients (and the MCP Inspector CLI) sometimes send a bare number for a naturally numeric value
   * such as a GeoTIFF band index, and rejecting that would be needlessly brittle. Throws
   * {@link MissingArgument} (an unchecked sentinel) when the argument is absent, blank, or a
   * non-scalar type, so callers can wrap the call in a try/catch and convert to an error result
   * without nesting deep argument validation logic.</p>
   *
   * @param arguments the tool call's argument map (from {@code request.arguments()})
   * @param key       the argument key to extract
   * @return the string value
   * @throws MissingArgument if the argument is absent, blank, or not a string/number/boolean
   */
  public static String requireString(Map<String, Object> arguments, String key) {
    Object value = arguments.get(key);
    String stringValue;
    if (value instanceof String s) {
      stringValue = s;
    } else if (value instanceof Number || value instanceof Boolean) {
      stringValue = String.valueOf(value);
    } else {
      throw new MissingArgument(key);
    }
    if (stringValue.isBlank()) {
      throw new MissingArgument(key);
    }
    return stringValue;
  }

  /**
   * Sentinel exception thrown by {@link #requireString(Map, String)} when a required argument is
   * missing or blank. Caught by tool handlers and translated to a {@link CallToolResult} via
   * {@link #errorResult(String)}.
   */
  public static final class MissingArgument extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String key;

    /**
     * Constructs a MissingArgument exception for the given argument key.
     *
     * @param key the name of the missing required argument
     */
    public MissingArgument(String key) {
      super("Missing required argument: " + key);
      this.key = key;
    }

    /**
     * Returns the name of the missing required argument.
     *
     * @return the argument key
     */
    public String getKey() {
      return key;
    }
  }

}
