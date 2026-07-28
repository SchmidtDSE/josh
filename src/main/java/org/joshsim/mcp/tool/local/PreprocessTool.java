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
import java.util.function.Consumer;
import org.joshsim.command.PreprocessOptions;
import org.joshsim.command.TimeAxisParams;
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
        script, simArg, dataFile, variableArg, unitsArg, outputFile,
        Optional.of(buildOptions(args))
    );
    return CallToolResult.builder()
        .addTextContent(result.getMessage())
        .isError(!result.isSuccess())
        .build();
  }

  /**
   * Builds preprocessing options from the tool call's optional arguments.
   *
   * <p>An omitted argument keeps {@link PreprocessOptions}' default, which is the
   * {@code preprocess} CLI's default, so an MCP client and a shell get the same result from the
   * same arguments. The time axis fields are read through {@link TimeAxisParams#fromLookup} keyed
   * on the same names the {@code /preprocessBatch} form uses, so all three surfaces name them
   * identically.</p>
   *
   * @param args the tool call's argument map
   * @return the resolved preprocessing options
   */
  static PreprocessOptions buildOptions(Map<String, Object> args) {
    PreprocessOptions.Builder builder = PreprocessOptions.builder()
        .timestep(ToolHandlers.optionalString(args, "timestep", ""))
        .defaultValue(ToolHandlers.optionalString(args, "defaultValue", null))
        .parallel(ToolHandlers.optionalBoolean(args, "parallel", false))
        .amend(ToolHandlers.optionalBoolean(args, "amend", false))
        .timeAxis(TimeAxisParams.fromLookup(
            field -> ToolHandlers.optionalString(args, field.getFieldName(), "")));

    applyIfPresent(args, "crs", builder::crsCode);
    applyIfPresent(args, "xCoord", builder::horizCoordName);
    applyIfPresent(args, "yCoord", builder::vertCoordName);

    if (ToolHandlers.optionalBoolean(args, "noTimeDim", false)) {
      builder.noTimeDim();
    } else {
      applyIfPresent(args, "timeDim", builder::timeName);
    }

    return builder.build();
  }

  private static void applyIfPresent(
      Map<String, Object> args, String key, Consumer<String> setter) {
    String value = ToolHandlers.optionalString(args, key, null);
    if (value != null) {
      setter.accept(value);
    }
  }

}
