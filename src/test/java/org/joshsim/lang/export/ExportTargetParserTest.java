package org.joshsim.lang.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
  public void testParse_ValidLocalScheme() {
    String target = "local:/path/to/file";

    ExportTarget result = ExportTargetParser.parse(target);

    assertEquals("", result.getProtocol());
    assertEquals("", result.getHost());
    assertEquals("/path/to/file", result.getPath());
  }

  @Test
  public void testParse_ValidMinioScheme() {
    String target = "minio://bucket.example.com/path/to/resource";

    ExportTarget result = ExportTargetParser.parse(target);

    assertEquals("minio", result.getProtocol());
    assertEquals("bucket.example.com", result.getHost());
    assertEquals("/path/to/resource", result.getPath());
  }

  @Test
  public void testParse_InvalidSchemeThrowsException() {
    String target = "unsupported:/path/to/file";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      ExportTargetParser.parse(target);
    });

    assertTrue(exception.getMessage().contains("Unsupported target scheme"));
  }

  @Test
  public void testParse_InvalidURISyntaxThrowsException() {
    String target = "invalid-uri-format";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      ExportTargetParser.parse(target);
    });
  }

  @Test
  public void testParse_EmptyTargetThrowsException() {
    String target = "";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      ExportTargetParser.parse(target);
    });
  }
}