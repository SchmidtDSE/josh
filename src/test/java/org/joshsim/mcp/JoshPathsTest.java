/**
 * Tests for JoshPaths path-resolution utility.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for the JoshPaths utility class.
 *
 * <p>Verifies that path resolution produces absolute, normalized paths regardless
 * of the input format.</p>
 */
public class JoshPathsTest {

  /**
   * Tests that an absolute path is returned unchanged (modulo normalization).
   */
  @Test
  public void testAbsolutePathIsAbsolute() {
    Path result = JoshPaths.resolve("/tmp/test.josh");
    assertTrue(result.isAbsolute(), "Resolved path must be absolute");
  }

  /**
   * Tests that a relative path is resolved to an absolute path.
   */
  @Test
  public void testRelativePathBecomesAbsolute() {
    Path result = JoshPaths.resolve("relative/test.josh");
    assertTrue(result.isAbsolute(), "Relative path must become absolute after resolution");
  }

  /**
   * Tests that the result is not null for a simple filename.
   */
  @Test
  public void testSimpleFileNameIsNotNull() {
    Path result = JoshPaths.resolve("myfile.josh");
    assertNotNull(result, "Result must not be null");
  }

  /**
   * Tests that dot-dot segments are normalized away.
   */
  @Test
  public void testNormalizationRemovesDotDot() {
    Path result = JoshPaths.resolve("/tmp/dir/../test.josh");
    assertEquals("/tmp/test.josh", result.toString(),
        "Path with .. segment must be normalized");
  }

  /**
   * Tests that single-dot segments are normalized away.
   */
  @Test
  public void testNormalizationRemovesDot() {
    Path result = JoshPaths.resolve("/tmp/./test.josh");
    assertEquals("/tmp/test.josh", result.toString(),
        "Path with . segment must be normalized");
  }

  /**
   * Tests that the filename is preserved at the end of the path.
   */
  @Test
  public void testFileNamePreserved() {
    Path result = JoshPaths.resolve("/some/dir/simulation.josh");
    assertEquals("simulation.josh", result.getFileName().toString(),
        "Filename must be preserved");
  }
}
