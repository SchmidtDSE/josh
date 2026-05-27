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
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.mcp.Backend;
import org.joshsim.mcp.JoshPaths;
import org.joshsim.mcp.tool.local.ToolHandlers.MissingArgument;

/**
 * Registers the {@code run_simulation} MCP tool.
 *
 * <p>Runs a Josh simulation in the local JVM. Outputs are written to the paths defined
 * in the script's {@code exportFiles} configuration. The tool returns a short summary
 * (simulation name, replicate count, last completed step) rather than inlining the
 * full output data, which can be very large.</p>
 */
public final class RunSimulationTool {

  private static final String TOOL_NAME = "run_simulation";

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
        .name(TOOL_NAME)
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
    Map<String, Path> dataFiles;
    try {
      scriptArg = ToolHandlers.requireString(args, "script");
      simArg = ToolHandlers.requireString(args, "simulation");
      dataFiles = parseDataFiles(args.get("data"));
    } catch (MissingArgument | IllegalArgumentException e) {
      return ToolHandlers.errorResult(e.getMessage());
    }

    int replicates = 1;
    Object repObj = args.get("replicates");
    if (repObj instanceof Number repNum) {
      replicates = repNum.intValue();
    }

    boolean serialPatches = false;
    Object serialObj = args.get("serialPatches");
    if (serialObj instanceof Boolean serialBool) {
      serialPatches = serialBool;
    }

    Optional<Long> seed = Optional.empty();
    Object seedObj = args.get("seed");
    if (seedObj instanceof Number seedNum) {
      seed = Optional.of(seedNum.longValue());
    }

    Path script = JoshPaths.resolve(scriptArg);
    Backend.RunSimulationResult result = backend.runSimulation(
        script, simArg, replicates, serialPatches, seed, dataFiles
    );
    return CallToolResult.builder()
        .addTextContent(result.getMessage())
        .isError(!result.isSuccess())
        .build();
  }

  /**
   * Parses the optional {@code data} argument into a map of external resource name to resolved
   * path. Each value is resolved through {@link JoshPaths#resolve(String)}; keys are left as the
   * script-facing names. Returns an empty map when {@code data} is absent.
   *
   * @param dataObj the raw {@code data} argument value (expected to be an object), or null
   * @return a map of resource name to resolved {@link Path}
   * @throws IllegalArgumentException if {@code data} is not an object, or any entry has a blank
   *     name or a missing/blank/non-string path
   */
  static Map<String, Path> parseDataFiles(Object dataObj) {
    Map<String, Path> result = new LinkedHashMap<>();
    if (dataObj == null) {
      return result;
    }
    if (!(dataObj instanceof Map<?, ?> rawMap)) {
      throw new IllegalArgumentException(
          "Argument 'data' must be an object mapping names to paths");
    }
    for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
      String name = String.valueOf(entry.getKey()).trim();
      if (name.isEmpty()) {
        throw new IllegalArgumentException("data contains an entry with an empty name");
      }
      Object value = entry.getValue();
      if (!(value instanceof String pathStr) || pathStr.isBlank()) {
        throw new IllegalArgumentException(
            "data entry '" + name + "' must have a non-empty string path");
      }
      result.put(name, JoshPaths.resolve(pathStr));
    }
    return result;
  }

}
