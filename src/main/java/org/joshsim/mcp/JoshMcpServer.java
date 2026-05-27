/**
 * MCP server wiring for the Josh simulation toolkit.
 *
 * <p>Constructs the stdio MCP server, registers all Phase 1 tools, and starts serving. The server
 * runs until the client closes stdin (or the process is killed); the SDK manages I/O on its own
 * threads.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.McpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import java.util.ServiceLoader;
import org.joshsim.mcp.tool.local.DiscoverConfigTool;
import org.joshsim.mcp.tool.local.PreprocessTool;
import org.joshsim.mcp.tool.local.RunSimulationTool;
import org.joshsim.mcp.tool.local.ValidateTool;

/**
 * Wires the MCP Java SDK and registers Josh tools for stdio transport.
 *
 * <p>Construct via {@link #JoshMcpServer(Backend)} then call {@link #start()} to begin
 * serving. The caller is responsible for blocking the main thread after start returns
 * (e.g. by joining on stdin).</p>
 */
public class JoshMcpServer {

  private final Backend backend;
  private McpSyncServer server;

  /**
   * Constructs a JoshMcpServer with the given compute backend.
   *
   * @param backend the backend implementation to use for tool execution
   */
  public JoshMcpServer(Backend backend) {
    this.backend = backend;
  }

  /**
   * Returns the McpJsonMapper loaded via the service loader (backed by Jackson 3).
   *
   * @return a McpJsonMapper instance
   * @throws IllegalStateException if no McpJsonMapperSupplier can be found on the classpath
   */
  public static McpJsonMapper loadJsonMapper() {
    ServiceLoader<McpJsonMapperSupplier> loader = ServiceLoader.load(McpJsonMapperSupplier.class);
    return loader.findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No McpJsonMapperSupplier found on classpath — "
            + "ensure mcp-json-jackson3 is on the runtime classpath"))
        .get();
  }

  /**
   * Builds and starts the MCP server, registering all Phase 1 tools.
   *
   * <p>Returns immediately after construction; the SDK manages stdio I/O on its own threads.
   * The caller must keep the main thread alive until the session ends.</p>
   */
  public void start() {
    McpJsonMapper jsonMapper = loadJsonMapper();

    StdioServerTransportProvider transportProvider =
        new StdioServerTransportProvider(jsonMapper);

    server = McpServer.sync(transportProvider)
        .serverInfo("joshsim", "1.0.0")
        .capabilities(ServerCapabilities.builder().tools(true).build())
        .build();

    ValidateTool.register(server, backend, jsonMapper);
    DiscoverConfigTool.register(server, backend, jsonMapper);
    PreprocessTool.register(server, backend, jsonMapper);
    RunSimulationTool.register(server, backend, jsonMapper);
  }

  /**
   * Closes the MCP server and releases resources.
   */
  public void close() {
    if (server != null) {
      server.close();
    }
  }

}
