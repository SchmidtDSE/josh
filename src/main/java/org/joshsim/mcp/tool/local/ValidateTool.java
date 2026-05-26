/**
 * MCP tool for validating Josh simulation scripts.
 *
 * <p>Registers the {@code validate_simulation} MCP tool which parses and interprets a
 * {@code .josh} file to confirm it is syntactically and semantically valid.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp.tool.local;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.nio.file.Path;
import org.joshsim.mcp.Backend;
import org.joshsim.mcp.JoshPaths;
import org.joshsim.mcp.tool.local.ToolHandlers.MissingArgument;

/**
 * Registers the {@code validate_simulation} MCP tool.
 *
 * <p>The tool accepts a single {@code script} argument pointing to a {@code .josh} file
 * and returns a success or error result describing any parse failures.</p>
 */
public final class ValidateTool {

  private static final String TOOL_NAME = "validate_simulation";

  private ValidateTool() {
    // Static utility
  }

  /**
   * Registers the {@code validate_simulation} tool on the given server.
   *
   * @param server     the MCP sync server to register the tool on
   * @param backend    the backend that will execute validation
   * @param jsonMapper the JSON mapper used for schema parsing
   */
  public static void register(McpSyncServer server, Backend backend, McpJsonMapper jsonMapper) {
    Tool tool = Tool.builder()
        .name(TOOL_NAME)
        .description(
            "Validates a Josh simulation script (.josh file) for syntax and semantic errors. "
            + "Use this before running a simulation to catch parse errors early. "
            + "Returns a success message if the script is valid, or a list of error details "
            + "describing exactly where each problem was found (line number and message). "
            + "Josh scripts define simulations using 'start simulation ... end simulation' blocks "
            + "containing grid configuration and step ranges."
        )
        .inputSchema(jsonMapper, ToolSchemas.load(TOOL_NAME))
        .build();

    SyncToolSpecification spec = SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, request) -> handle(request, backend))
        .build();

    server.addTool(spec);
  }

  private static CallToolResult handle(CallToolRequest request, Backend backend) {
    String scriptArg;
    try {
      scriptArg = ToolHandlers.requireString(request.arguments(), "script");
    } catch (MissingArgument e) {
      return ToolHandlers.errorResult(e.getMessage());
    }
    Path script = JoshPaths.resolve(scriptArg);
    Backend.ValidateResult result = backend.validate(script);
    return CallToolResult.builder()
        .addTextContent(result.getMessage())
        .isError(!result.isSuccess())
        .build();
  }

}
