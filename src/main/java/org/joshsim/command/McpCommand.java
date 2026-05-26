/**
 * Picocli subcommand that starts the Josh MCP server over stdio.
 *
 * <p>Spawning this command starts a Model Context Protocol server that exposes Josh simulation
 * operations as MCP tools. The server communicates via JSON-RPC over stdin/stdout, conforming to
 * the MCP stdio transport specification.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.util.concurrent.Callable;
import org.joshsim.mcp.JoshMcpServer;
import org.joshsim.mcp.LocalBackend;
import org.joshsim.mcp.StderrOutputOptions;
import picocli.CommandLine.Command;

/**
 * Starts the Josh MCP server using stdio transport.
 *
 * <p>This command blocks until stdin is closed. It is intended to be spawned as a subprocess by
 * an MCP client such as opencode. Example opencode configuration:</p>
 *
 * <pre>{@code
 * {
 *   "mcp": {
 *     "josh": {
 *       "type": "local",
 *       "command": ["java", "-jar", "/path/to/joshsim-fat.jar", "mcp"]
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p><strong>CRITICAL:</strong> All output from this command and its tool handlers must go to
 * {@code System.err}. Any writes to {@code System.out} corrupt the JSON-RPC framing and break the
 * MCP session.</p>
 */
@Command(
    name = "mcp",
    description = "Start the Josh MCP server (stdio transport)"
)
public class McpCommand implements Callable<Integer> {

  @Override
  public Integer call() {
    StderrOutputOptions output = new StderrOutputOptions();
    LocalBackend backend = new LocalBackend(output);
    JoshMcpServer mcpServer = new JoshMcpServer(backend);

    try {
      mcpServer.start();

      // Block the main thread forever. The SDK owns stdin/stdout on its own threads;
      // reading System.in here would race with the SDK's reader and corrupt JSON-RPC framing.
      // When the parent process closes the pipe, the SDK shuts down its threads and the
      // JVM exits naturally (or via signal from the parent).
      try {
        Thread.currentThread().join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } finally {
      mcpServer.close();
    }

    return 0;
  }

}
