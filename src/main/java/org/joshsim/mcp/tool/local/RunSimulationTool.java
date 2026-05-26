/**
 * MCP tool for running Josh simulations.
 *
 * <p>Registers the {@code run_simulation} MCP tool which executes a named simulation
 * from a {@code .josh} file, optionally with multiple replicates and a fixed random seed.</p>
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
 * Registers the {@code run_simulation} MCP tool.
 *
 * <p>Runs a Josh simulation in the local JVM. Outputs are written to the paths defined
 * in the script's {@code exportFiles} configuration. The tool returns a short summary
 * (simulation name, replicate count, last completed step) rather than inlining the
 * full output data, which can be very large.</p>
 */
public final class RunSimulationTool {

  private static final String INPUT_SCHEMA = "{"
      + "\"type\": \"object\","
      + "\"properties\": {"
      + "  \"script\": {"
      + "    \"type\": \"string\","
      + "    \"description\": \"Path to the .josh simulation script file.\""
      + "  },"
      + "  \"simulation\": {"
      + "    \"type\": \"string\","
      + "    \"description\": \"Name of the simulation block to run, exactly as it appears "
      + "      after 'start simulation' in the .josh file (e.g. 'Main').\""
      + "  },"
      + "  \"replicates\": {"
      + "    \"type\": \"integer\","
      + "    \"description\": \"Number of independent simulation replicates to run. "
      + "      Each replicate uses a different random seed unless seed is specified. "
      + "      Defaults to 1.\","
      + "    \"default\": 1,"
      + "    \"minimum\": 1"
      + "  },"
      + "  \"serialPatches\": {"
      + "    \"type\": \"boolean\","
      + "    \"description\": \"If true, patches are processed one at a time rather than in "
      + "      parallel. Serial mode is slower but required for reproducibility when using a "
      + "      fixed seed. Automatically set to true when seed is supplied. "
      + "      Defaults to false.\","
      + "    \"default\": false"
      + "  },"
      + "  \"seed\": {"
      + "    \"type\": \"integer\","
      + "    \"description\": \"Optional random seed for reproducible simulations. When "
      + "      provided, all random sampling uses this seed, and serial patch processing is "
      + "      automatically enabled to ensure deterministic results.\""
      + "  }"
      + "},"
      + "\"required\": [\"script\", \"simulation\"]"
      + "}";

  private RunSimulationTool() {
    // Static utility
  }

  /**
   * Registers the {@code run_simulation} tool on the given server.
   *
   * @param server     the MCP sync server to register the tool on
   * @param backend    the backend that will execute the simulation
   * @param jsonMapper the JSON mapper used for schema parsing
   */
  public static void register(McpSyncServer server, Backend backend, McpJsonMapper jsonMapper) {
    Tool tool = Tool.builder()
        .name("run_simulation")
        .description(
            "Runs a Josh simulation defined in a .josh script file. "
            + "The simulation name must match the 'start simulation NAME' block in the script. "
            + "Output files are written to the paths defined in the script's 'exportFiles' "
            + "configuration — the tool returns a short summary (replicate count and last step) "
            + "rather than the full output data, which is typically stored as CSV files. "
            + "Josh simulations define a spatial grid, organism types, and step logic. "
            + "Each step corresponds to one time unit (typically a year). "
            + "Run validate_simulation first to catch any script errors before a long simulation."
        )
        .inputSchema(jsonMapper, INPUT_SCHEMA)
        .build();

    SyncToolSpecification spec = SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, request) -> {
          String scriptArg = (String) request.arguments().get("script");
          String simArg = (String) request.arguments().get("simulation");

          if (scriptArg == null || scriptArg.isBlank()) {
            return errorResult("Missing required argument: script");
          }
          if (simArg == null || simArg.isBlank()) {
            return errorResult("Missing required argument: simulation");
          }

          int replicates = 1;
          Object repObj = request.arguments().get("replicates");
          if (repObj instanceof Number) {
            replicates = ((Number) repObj).intValue();
          }

          boolean serialPatches = false;
          Object serialObj = request.arguments().get("serialPatches");
          if (serialObj instanceof Boolean) {
            serialPatches = (Boolean) serialObj;
          }

          Optional<Long> seed = Optional.empty();
          Object seedObj = request.arguments().get("seed");
          if (seedObj instanceof Number) {
            seed = Optional.of(((Number) seedObj).longValue());
          }

          Path script = JoshPaths.resolve(scriptArg);
          Backend.RunSimulationResult result = backend.runSimulation(
              script, simArg, replicates, serialPatches, seed
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
