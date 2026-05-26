/**
 * MCP tool for discovering configuration variables in Josh simulation scripts.
 *
 * <p>Registers the {@code discover_config} MCP tool which analyses a {@code .josh} file
 * and returns the set of configuration variables referenced via {@code config} expressions.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp.tool.local;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.nio.file.Path;
import org.joshsim.mcp.Backend;
import org.joshsim.mcp.JoshPaths;

/**
 * Registers the {@code discover_config} MCP tool.
 *
 * <p>Config variables in Josh are referenced using the {@code config} keyword in simulation
 * expressions. This tool lists all such variables found in a script, which helps LLM clients
 * understand what configuration must be supplied before running the simulation.</p>
 */
public final class DiscoverConfigTool {

  private static final String INPUT_SCHEMA = "{"
      + "\"type\": \"object\","
      + "\"properties\": {"
      + "  \"script\": {"
      + "    \"type\": \"string\","
      + "    \"description\": \"Path to the .josh simulation script file.\""
      + "  }"
      + "},"
      + "\"required\": [\"script\"]"
      + "}";

  private DiscoverConfigTool() {
    // Static utility
  }

  /**
   * Registers the {@code discover_config} tool on the given server.
   *
   * @param server     the MCP sync server to register the tool on
   * @param backend    the backend that will execute config discovery
   * @param jsonMapper the JSON mapper used for schema parsing
   */
  public static void register(McpSyncServer server, Backend backend, McpJsonMapper jsonMapper) {
    Tool tool = Tool.builder()
        .name("discover_config")
        .description(
            "Discovers all configuration variables referenced in a Josh simulation script. "
            + "Josh scripts can reference external configuration values using the 'config' "
            + "keyword — for example, 'config.growthRate' or 'config.maxAge'. "
            + "This tool returns the names and expected types of all such variables found in "
            + "the script, which tells you what must be provided in a .jshc configuration file "
            + "before the simulation can be run. "
            + "Returns '[No variables found]' if the script uses no configuration variables."
        )
        .inputSchema(jsonMapper, INPUT_SCHEMA)
        .build();

    SyncToolSpecification spec = SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, request) -> {
          String scriptArg = (String) request.arguments().get("script");
          if (scriptArg == null || scriptArg.isBlank()) {
            return CallToolResult.builder()
                .addTextContent("Missing required argument: script")
                .isError(Boolean.TRUE)
                .build();
          }
          Path script = JoshPaths.resolve(scriptArg);
          Backend.DiscoverConfigResult result = backend.discoverConfig(script);
          return CallToolResult.builder()
              .addTextContent(result.getOutput())
              .isError(!result.isSuccess())
              .build();
        })
        .build();

    server.addTool(spec);
  }

}
