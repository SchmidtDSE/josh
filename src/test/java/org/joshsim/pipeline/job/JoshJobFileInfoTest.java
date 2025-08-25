/**
 * Unit tests for JoshJobFileInfo class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the JoshJobFileInfo class.
 *
 * <p>Tests immutability, name extraction, validation, and equality semantics
 * of the JoshJobFileInfo class.</p>
 */
public class JoshJobFileInfoTest {

  @Test
  public void testConstructorWithValidInputs() {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    assertEquals("example_1", fileInfo.getName());
    assertEquals("test_data/example_1.jshc", fileInfo.getPath());
  }

  @Test
  public void testConstructorTrimsWhitespace() {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("  example_1  ", "  test_data/example_1.jshc  ");
    assertEquals("example_1", fileInfo.getName());
    assertEquals("test_data/example_1.jshc", fileInfo.getPath());
  }

  @Test
  public void testConstructorWithNullNameThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      new JoshJobFileInfo(null, "test_data/example_1.jshc");
    });
  }

  @Test
  public void testConstructorWithEmptyNameThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      new JoshJobFileInfo("", "test_data/example_1.jshc");
    });
  }

  @Test
  public void testConstructorWithBlankNameThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      new JoshJobFileInfo("   ", "test_data/example_1.jshc");
    });
  }

  @Test
  public void testConstructorWithNullPathThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      new JoshJobFileInfo("example_1", null);
    });
  }

  @Test
  public void testConstructorWithEmptyPathThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      new JoshJobFileInfo("example_1", "");
    });
  }

  @Test
  public void testConstructorWithBlankPathThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      new JoshJobFileInfo("example_1", "   ");
    });
  }

  @Test
  public void testFromPathWithSimpleFilename() {
    JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath("example_1.jshc");
    assertEquals("example_1", fileInfo.getName());
    assertEquals("example_1.jshc", fileInfo.getPath());
  }

  @Test
  public void testFromPathWithDirectoryStructure() {
    JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath("test_data/config/example_1.jshc");
    assertEquals("example_1", fileInfo.getName());
    assertEquals("test_data/config/example_1.jshc", fileInfo.getPath());
  }

  @Test
  public void testFromPathWithDifferentExtension() {
    JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath("data/other_2.jshd");
    assertEquals("other_2", fileInfo.getName());
    assertEquals("data/other_2.jshd", fileInfo.getPath());
  }

  @Test
  public void testFromPathWithNoExtension() {
    JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath("test_data/example_file");
    assertEquals("example_file", fileInfo.getName());
    assertEquals("test_data/example_file", fileInfo.getPath());
  }

  @Test
  public void testFromPathWithWindowsPath() {
    JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath("C:\\test_data\\example_1.jshc");
    assertEquals("example_1", fileInfo.getName());
    assertEquals("C:\\test_data\\example_1.jshc", fileInfo.getPath());
  }

  @Test
  public void testFromPathWithMultipleDots() {
    JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath("config.backup.v1.jshc");
    assertEquals("config.backup.v1", fileInfo.getName());
    assertEquals("config.backup.v1.jshc", fileInfo.getPath());
  }

  @Test
  public void testFromPathWithNullPathThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      JoshJobFileInfo.fromPath(null);
    });
  }

  @Test
  public void testFromPathWithEmptyPathThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      JoshJobFileInfo.fromPath("");
    });
  }

  @Test
  public void testFromPathWithBlankPathThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      JoshJobFileInfo.fromPath("   ");
    });
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testGetPathForLegacyAccess() {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    assertEquals("test_data/example_1.jshc", fileInfo.getPathForLegacyAccess());
  }

  @Test
  public void testEqualityWithSameValues() {
    JoshJobFileInfo fileInfo1 = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    JoshJobFileInfo fileInfo2 = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    assertEquals(fileInfo1, fileInfo2);
    assertEquals(fileInfo1.hashCode(), fileInfo2.hashCode());
  }

  @Test
  public void testEqualityWithDifferentNames() {
    JoshJobFileInfo fileInfo1 = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    JoshJobFileInfo fileInfo2 = new JoshJobFileInfo("example_2", "test_data/example_1.jshc");
    assertNotEquals(fileInfo1, fileInfo2);
  }

  @Test
  public void testEqualityWithDifferentPaths() {
    JoshJobFileInfo fileInfo1 = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    JoshJobFileInfo fileInfo2 = new JoshJobFileInfo("example_1", "test_data/example_2.jshc");
    assertNotEquals(fileInfo1, fileInfo2);
  }

  @Test
  public void testEqualityWithNull() {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    assertNotEquals(fileInfo, null);
  }

  @Test
  public void testEqualityWithSameReference() {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    assertEquals(fileInfo, fileInfo);
  }

  @Test
  public void testEqualityWithDifferentClass() {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    assertNotEquals(fileInfo, "string");
  }

  @Test
  public void testToString() {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    String result = fileInfo.toString();
    assertTrue(result.contains("example_1"));
    assertTrue(result.contains("test_data/example_1.jshc"));
    assertTrue(result.contains("JoshJobFileInfo"));
  }

  @Test
  public void testImmutability() {
    // Test that the object is immutable by ensuring getters return the same values
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    String originalName = fileInfo.getName();
    String originalPath = fileInfo.getPath();

    // Multiple calls should return the same values
    assertEquals(originalName, fileInfo.getName());
    assertEquals(originalPath, fileInfo.getPath());
  }
}
