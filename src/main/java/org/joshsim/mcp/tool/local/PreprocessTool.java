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
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.nio.file.Path;
import java.util.Optional;
import org.joshsim.mcp.Backend;
import org.joshsim.mcp.JoshPaths;

/**
 * Registers the {@code preprocess_data} MCP tool.
 *
 * <p>Preprocessing converts an external geospatial dataset (NetCDF, GeoTIFF, or existing
 * {@code .jshd} file) into Josh's binary {@code .jshd} format, aligned to the simulation grid
 * defined in a {@code .josh} script. The resulting file can then be referenced from the script
 * using external data expressions.</p>
 */
public final class PreprocessTool {

  private static final String INPUT_SCHEMA = "{"
      + "\"type\": \"object\","
      + "\"properties\": {"
      + "  \"script\": {"
      + "    \"type\": \"string\","
      + "    \"description\": \"Path to the .josh simulation script file whose grid definition "
      + "      will be used to align the data.\""
      + "  },"
      + "  \"simulation\": {"
      + "    \"type\": \"string\","
      + "    \"description\": \"Name of the simulation block inside the script that defines "
      + "      the target grid (e.g. 'Main').\""
      + "  },"
      + "  \"dataFile\": {"
      + "    \"type\": \"string\","
      + "    \"description\": \"Path to the input data file. Supported formats: NetCDF (.nc), "
      + "      GeoTIFF (.tiff/.tif), or existing Josh binary (.jshd).\""
      + "  },"
      + "  \"variable\": {"
      + "    \"type\": \"string\","
      + "    \"description\": \"Variable name to extract from the data file, or a band number "
      + "      for GeoTIFF files.\""
      + "  },"
      + "  \"unitsStr\": {"
      + "    \"type\": \"string\","
      + "    \"description\": \"Physical units for the extracted data, as understood by Josh "
      + "      (e.g. 'count', 'meters', 'kg/m^2'). These units will be attached to the values "
      + "      in the output .jshd file and must match what the simulation expects.\""
      + "  },"
      + "  \"outputFile\": {"
      + "    \"type\": \"string\","
      + "    \"description\": \"Path where the preprocessed .jshd file should be written. "
      + "      Use .jshdz extension for compressed output.\""
      + "  }"
      + "},"
      + "\"required\": [\"script\", \"simulation\", \"dataFile\", \"variable\", "
      + "\"unitsStr\", \"outputFile\"]"
      + "}";

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
        .name("preprocess_data")
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
        .inputSchema(jsonMapper, INPUT_SCHEMA)
        .build();

    SyncToolSpecification spec = SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, request) -> {
          String scriptArg = (String) request.arguments().get("script");
          String simArg = (String) request.arguments().get("simulation");
          String dataFileArg = (String) request.arguments().get("dataFile");

          if (scriptArg == null || scriptArg.isBlank()) {
            return errorResult("Missing required argument: script");
          }
          if (simArg == null || simArg.isBlank()) {
            return errorResult("Missing required argument: simulation");
          }
          if (dataFileArg == null || dataFileArg.isBlank()) {
            return errorResult("Missing required argument: dataFile");
          }

          String variableArg = (String) request.arguments().get("variable");
          String unitsArg = (String) request.arguments().get("unitsStr");
          String outputArg = (String) request.arguments().get("outputFile");

          if (variableArg == null || variableArg.isBlank()) {
            return errorResult("Missing required argument: variable");
          }
          if (unitsArg == null || unitsArg.isBlank()) {
            return errorResult("Missing required argument: unitsStr");
          }
          if (outputArg == null || outputArg.isBlank()) {
            return errorResult("Missing required argument: outputFile");
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
        })
        .build();

    server.addTool(spec);
  }

  private static CallToolResult errorResult(String message) {
    return CallToolResult.builder()
        .addTextContent(message)
        .isError(Boolean.TRUE)
        .build();
  }

}
