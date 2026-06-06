/**
 * MCP tool for preprocessing external data into Josh's .jshd binary format.
 *
 * <p>Registers the {@code preprocess_data} MCP tool which converts geospatial data files
 * (NetCDF, GeoTIFF) into the {@code .jshd} binary format required for external data access
 * during simulations.</p>
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
import java.util.Map;
import java.util.Optional;
import org.joshsim.mcp.Backend;
import org.joshsim.mcp.JoshPaths;
import org.joshsim.mcp.tool.local.ToolHandlers.MissingArgument;

/**
 * Registers the {@code preprocess_data} MCP tool.
 *
 * <p>Preprocessing converts an external geospatial dataset (NetCDF, GeoTIFF, or existing
 * {@code .jshd} file) into Josh's binary {@code .jshd} format, aligned to the simulation grid
 * defined in a {@code .josh} script. The resulting file can then be referenced from the script
 * using external data expressions.</p>
 */
public final class PreprocessTool {

  private static final String TOOL_NAME = "preprocess_data";

  private PreprocessTool() {
    // Static utility
  }

  /**
   * Registers the {@code preprocess_data} tool on the given server.
   *
   * @param server     the MCP sync server to register the tool on
   * @param backend    the backend that will execute preprocessing
   * @param jsonMapper the JSON mapper used for schema parsing
   */
  public static void register(McpSyncServer server, Backend backend, McpJsonMapper jsonMapper) {
    Tool tool = Tool.builder()
        .name(TOOL_NAME)
        .description(
            "Preprocesses an external geospatial data file into Josh's binary .jshd format, "
            + "aligned to a simulation grid defined in a .josh script. "
            + "Run this once per data file before running the simulation that uses it. "
            + "Supported input formats: NetCDF (.nc), GeoTIFF (.tiff/.tif), "
            + "or an existing .jshd file. "
            + "The output .jshd file is referenced from Josh scripts using external data "
            + "expressions such as 'load \"mydata.jshd\" as temperature'. "
            + "Use .jshdz as the output extension for compressed output."
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
    Map<String, Object> args = request.arguments();
    String scriptArg;
    String simArg;
    String dataFileArg;
    String variableArg;
    String unitsArg;
    String outputArg;
    try {
      scriptArg = ToolHandlers.requireString(args, "script");
      simArg = ToolHandlers.requireString(args, "simulation");
      dataFileArg = ToolHandlers.requireString(args, "dataFile");
      variableArg = ToolHandlers.requireString(args, "variable");
      unitsArg = ToolHandlers.requireString(args, "unitsStr");
      outputArg = ToolHandlers.requireString(args, "outputFile");
    } catch (MissingArgument e) {
      return ToolHandlers.errorResult(e.getMessage());
    }

    Path script = JoshPaths.resolve(scriptArg);
    Path dataFile = JoshPaths.resolve(dataFileArg);
    Path outputFile = JoshPaths.resolve(outputArg);

    Backend.PreprocessResult result = backend.preprocess(
        script, simArg, dataFile, variableArg, unitsArg, outputFile, Optional.empty()
    );
    return CallToolResult.builder()
        .addTextContent(result.getMessage())
        .isError(!result.isSuccess())
        .build();
  }

}
