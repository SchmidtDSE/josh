/**
 * Path resolution utilities for the Josh MCP server.
 *
 * <p>Centralises all path resolution so that future sandboxing (root allow-lists, temp-directory
 * materialisation for hosted mode) can be plugged in at a single seam without touching individual
 * tool handlers.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Static helper for resolving user-supplied path strings to absolute, normalised {@link Path}s.
 *
 * <p>Today the implementation is trivial: {@code Paths.get(arg).toAbsolutePath().normalize()}.
 * This is the seam where root-fencing (local mode) or resource materialisation (hosted mode)
 * will be inserted in a future phase.</p>
 */
public final class JoshPaths {

  private JoshPaths() {
    // Static utility class
  }

  /**
   * Resolves a user-supplied path string to an absolute, normalised {@link Path}.
   *
   * <p>Relative paths are resolved against the JVM's current working directory, which is the
   * user's project directory when the MCP server is spawned by an MCP client such as opencode.</p>
   *
   * @param arg path string supplied by the MCP client
   * @return absolute, normalised {@link Path}
   */
  public static Path resolve(String arg) {
    return Paths.get(arg).toAbsolutePath().normalize();
  }

}
