/**
 * Test for ExportTargetParser.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Tests for the `ExportTargetParser` class.
 *
 * <p>The `ExportTargetParser` class is responsible for parsing a target string into an
 * `ExportTarget` object. Its `parse` method accepts a target string in URI format and determines
 * the export target type based on the scheme of the URI. Supported schemes include `local` and
 * `minio`. Any unsupported schemes or invalid URI formats result in an `IllegalArgumentException`.
 * </p>
 */
public class ExportTargetParserTest {

  @Test
  public void testParseValidLocalScheme() {
    String target = "file:/path/to/file";

    ExportTarget result = ExportTargetParser.parse(target);

    assertEquals("", result.getProtocol());
    assertEquals("", result.getHost());
    assertEquals("/path/to/file", result.getPath());
  }

  @Test
  public void testParseValidLocalSchemeRoot() {
    String target = "file:///path/to/file";

    ExportTarget result = ExportTargetParser.parse(target);

    assertEquals("", result.getProtocol());
    assertEquals("", result.getHost());
    assertEquals("/path/to/file", result.getPath());
  }

  @Test
  public void testParseValidMinioScheme() {
    String target = "minio://bucket.example.com/path/to/resource";

    ExportTarget result = ExportTargetParser.parse(target);

    assertEquals("minio", result.getProtocol());
    assertEquals("bucket.example.com", result.getHost());
    assertEquals("/path/to/resource", result.getPath());
  }

  @Test
  public void testParseInvalidSchemeThrowsException() {
    String target = "unsupported:/path/to/file";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      ExportTargetParser.parse(target);
    });

    assertTrue(exception.getMessage().contains("Unsupported target scheme"));
  }

  @Test
  public void testParseInvalidSyntaxThrowsException() {
    String target = "invalid-uri-format";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      ExportTargetParser.parse(target);
    });
  }

  @Test
  public void testParseEmptyTargetThrowsException() {
    String target = "";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      ExportTargetParser.parse(target);
    });
  }

  @Test
  public void testMemory() {
    String target = "memory://editor/test";

    ExportTarget result = ExportTargetParser.parse(target);

    assertEquals("memory", result.getProtocol());
    assertEquals("editor", result.getHost());
    assertEquals("test", result.getPath());
  }
}
