/**
 * OutputOptions that routes all output to System.err instead of System.out.
 *
 * <p>The stdio MCP transport uses stdout exclusively for JSON-RPC messages. Any stray writes to
 * stdout corrupt the protocol framing and make the server unusable. This class ensures that all
 * informational and error messages produced by Josh facades are written to stderr, which is safe
 * for both stdio and HTTP transport modes.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp;

import org.joshsim.util.OutputOptions;

/**
 * OutputOptions implementation that writes all messages to System.err.
 *
 * <p>Used by MCP tool handlers and the LocalBackend to ensure that no Josh output
 * is written to stdout, which would corrupt the stdio MCP transport.</p>
 */
public class StderrOutputOptions extends OutputOptions {

  /**
   * Prints an informational message to stderr.
   *
   * @param message the informational message to print
   */
  @Override
  public void printInfo(String message) {
    System.err.println(message);
  }

  /**
   * Prints an error message to stderr.
   *
   * @param message the error message to print
   */
  @Override
  public void printError(String message) {
    System.err.println(message);
  }

}
