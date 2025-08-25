/**
 * Unit tests for JobVariationParser class.
 *
 * <p>Tests the ANTLR-based parsing logic for job variation specifications,
 * error handling, and integration with JoshJobBuilder. Validates the new
 * semicolon-separated format and compatibility with existing functionality.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for JobVariationParser ANTLR-based parsing functionality.
 *
 * <p>Validates parsing logic, error handling, and integration
 * with the job builder pattern using semicolon-separated format.</p>
 */
public class JobVariationParserTest {

  private JobVariationParser parser;
  private JoshJobBuilder builder;

  /**
   * Set up test instances before each test.
   */
  @BeforeEach
  public void setUp() {
    parser = new JobVariationParser();
    builder = new JoshJobBuilder();
  }

  @Test
  public void testBasicSemicolonParsing() {
    String[] dataFiles = {
        "example.jshc=test_data/example_1.jshc;other.jshd=test_data/other_1.jshd"
    };
    
    JoshJobBuilder result = parser.parseDataFiles(builder, dataFiles);
    
    // Should return the same builder instance for chaining
    assertSame(builder, result);
    
    JoshJob job = result.build();
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
    assertEquals(2, job.getFileNames().size());
  }

  @Test
  public void testSingleDataFile() {
    String[] dataFiles = {"config.jshc=data/config_1.jshc"};
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals("data/config_1.jshc", job.getFilePath("config.jshc"));
    assertEquals(1, job.getFileNames().size());
  }

  @Test
  public void testEmptyDataFiles() {
    String[] dataFiles = {};
    
    JoshJobBuilder result = parser.parseDataFiles(builder, dataFiles);
    
    assertSame(builder, result);
    JoshJob job = result.build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testNullDataFiles() {
    JoshJobBuilder result = parser.parseDataFiles(builder, (String[]) null);
    
    assertSame(builder, result);
    JoshJob job = result.build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testEmptyString() {
    String[] dataFiles = {""};
    
    JoshJobBuilder result = parser.parseDataFiles(builder, dataFiles);
    
    assertSame(builder, result);
    JoshJob job = result.build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testWhitespaceOnly() {
    String[] dataFiles = {"   "};
    
    JoshJobBuilder result = parser.parseDataFiles(builder, dataFiles);
    
    assertSame(builder, result);
    JoshJob job = result.build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testWhitespaceHandling() {
    String[] dataFiles = {
        "  example.jshc  =  test_data/example_1.jshc  ;other.jshd= test_data/other_1.jshd  "
    };
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
  }

  @Test
  public void testIterableInterface() {
    Iterable<String> dataFiles = Arrays.asList(
        "example.jshc=test_data/example_1.jshc;other.jshd=test_data/other_1.jshd");
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
    assertEquals(2, job.getFileNames().size());
  }

  @Test
  public void testEmptyIterable() {
    Iterable<String> dataFiles = Collections.emptyList();
    
    JoshJobBuilder result = parser.parseDataFiles(builder, dataFiles);
    
    assertSame(builder, result);
    JoshJob job = result.build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testNullIterable() {
    Iterable<String> dataFiles = null;
    
    JoshJobBuilder result = parser.parseDataFiles(builder, dataFiles);
    
    assertSame(builder, result);
    JoshJob job = result.build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testInvalidFormatNoEquals() {
    String[] dataFiles = {"example.jshc_test_data/example_1.jshc"};
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> parser.parseDataFiles(builder, dataFiles)
    );
    
    assertTrue(exception.getMessage().contains("Failed to parse job variation"));
    assertTrue(exception.getMessage().contains("example.jshc_test_data/example_1.jshc"));
  }

  @Test
  public void testMultipleEqualsInPath() {
    String[] dataFiles = {"example.jshc=test=data=example_1.jshc"};
    
    // Should work - ANTLR handles everything after first equals as filepath
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals("test=data=example_1.jshc", job.getFilePath("example.jshc"));
  }

  @Test
  public void testInvalidFormatOnlyEquals() {
    String[] dataFiles = {"="};
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> parser.parseDataFiles(builder, dataFiles)
    );
    
    // Should get parsing error since "=" doesn't match the grammar
    assertTrue(exception.getMessage().contains("Failed to parse job variation"));
  }

  @Test
  public void testInvalidFormatEmptyName() {
    String[] dataFiles = {"=test_data/example_1.jshc"};
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> parser.parseDataFiles(builder, dataFiles)
    );
    
    assertTrue(exception.getMessage().contains("Failed to parse job variation"));
  }

  @Test
  public void testInvalidFormatEmptyPath() {
    String[] dataFiles = {"example.jshc="};
    
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> parser.parseDataFiles(builder, dataFiles)
    );
    
    assertTrue(exception.getMessage().contains("Failed to parse job variation"));
  }

  @Test
  public void testComplexPaths() {
    String[] dataFiles = {
        "config.jshc=C:\\Users\\test\\data\\config.jshc;"
        + "data.jshd=/home/user/project/data/file.jshd;"
        + "url.jshc=https://example.com/data/config.jshc"
    };
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals("C:\\Users\\test\\data\\config.jshc", job.getFilePath("config.jshc"));
    assertEquals("/home/user/project/data/file.jshd", job.getFilePath("data.jshd"));
    assertEquals("https://example.com/data/config.jshc", job.getFilePath("url.jshc"));
  }

  @Test
  public void testUriSupport() {
    String[] dataFiles = {
        "remote.jshc=http://example.com:8080/config.jshc;"
        + "local.jshd=file:///tmp/data.jshd"
    };
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals("http://example.com:8080/config.jshc", job.getFilePath("remote.jshc"));
    assertEquals("file:///tmp/data.jshd", job.getFilePath("local.jshd"));
  }

  @Test
  public void testWindowsPathSupport() {
    String[] dataFiles = {
        "config.jshc=C:\\Users\\test\\config.jshc;data.jshd=D:\\data\\file.jshd"
    };
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals("C:\\Users\\test\\config.jshc", job.getFilePath("config.jshc"));
    assertEquals("D:\\data\\file.jshd", job.getFilePath("data.jshd"));
  }

  @Test
  public void testCommaInPathWarning() {
    // Test that using comma instead of semicolon gives helpful error message
    String[] dataFiles = {
        "example.jshc=test_data/example_1.jshc,other.jshd=test_data/other_1.jshd"
    };
    
    // This should actually work since comma is allowed in paths now
    // The new parser doesn't split on comma like the old one did
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    // Should have one file with comma in path
    assertEquals("test_data/example_1.jshc,other.jshd=test_data/other_1.jshd", 
                 job.getFilePath("example.jshc"));
    assertEquals(1, job.getFileNames().size());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testIntegrationWithExistingBuilder() {
    // Pre-configure builder
    builder.setReplicates(5).setFilePath("existing.jshc", "existing_path.jshc");
    
    String[] dataFiles = {"new.jshd=new_path.jshd"};
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    // Should have both existing and new file paths
    assertEquals(5, job.getReplicates());
    assertEquals("existing_path.jshc", job.getFilePath("existing.jshc"));
    assertEquals("new_path.jshd", job.getFilePath("new.jshd"));
    assertEquals(2, job.getFileNames().size());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testOverwriteExistingFilePath() {
    // Pre-configure builder with a file
    builder.setFilePath("config.jshc", "old_path.jshc");
    
    String[] dataFiles = {"config.jshc=new_path.jshc"};
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    // Should overwrite the existing path
    assertEquals("new_path.jshc", job.getFilePath("config.jshc"));
    assertEquals(1, job.getFileNames().size());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testChainedCallsWithParser() {
    String[] dataFiles = {"example.jshc=path1.jshc"};
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles)
        .setReplicates(3)
        .setFilePath("additional.jshd", "path2.jshd")
        .build();
    
    assertNotNull(job);
    assertEquals(3, job.getReplicates());
    assertEquals("path1.jshc", job.getFilePath("example.jshc"));
    assertEquals("path2.jshd", job.getFilePath("additional.jshd"));
    assertEquals(2, job.getFileNames().size());
  }

  @Test
  public void testSpecialCharactersInPaths() {
    String[] dataFiles = {
        "test2.jshd=path-with-dashes_and_underscores.jshd;"
        + "test3.jshc=./relative/path/file.jshc"
    };
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals("path-with-dashes_and_underscores.jshd", job.getFilePath("test2.jshd"));
    assertEquals("./relative/path/file.jshc", job.getFilePath("test3.jshc"));
  }

  @Test
  public void testFileNameExtractionFromPath() {
    String[] dataFiles = {
        "example.jshc=test_data/example_1.jshc;"
        + "other.jshd=data/config/other_2.jshd;"
        + "simple.jshc=simple_file.jshc"
    };
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    // Test that JoshJobFileInfo objects are created correctly
    JoshJobFileInfo exampleInfo = job.getFileInfo("example.jshc");
    assertNotNull(exampleInfo);
    assertEquals("example_1", exampleInfo.getName());
    assertEquals("test_data/example_1.jshc", exampleInfo.getPath());
    
    JoshJobFileInfo otherInfo = job.getFileInfo("other.jshd");
    assertNotNull(otherInfo);
    assertEquals("other_2", otherInfo.getName());
    assertEquals("data/config/other_2.jshd", otherInfo.getPath());
    
    JoshJobFileInfo simpleInfo = job.getFileInfo("simple.jshc");
    assertNotNull(simpleInfo);
    assertEquals("simple_file", simpleInfo.getName());
    assertEquals("simple_file.jshc", simpleInfo.getPath());
  }

  @Test
  public void testFileNameExtractionWithoutExtension() {
    String[] dataFiles = {"config.jshc=test_data/config_file"};  // No extension
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    JoshJobFileInfo fileInfo = job.getFileInfo("config.jshc");
    assertNotNull(fileInfo);
    assertEquals("config_file", fileInfo.getName());
    assertEquals("test_data/config_file", fileInfo.getPath());
  }

  @Test
  public void testFileNameExtractionWithMultipleDots() {
    String[] dataFiles = {"backup.jshc=data/config.backup.v1.jshc"};
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    JoshJobFileInfo fileInfo = job.getFileInfo("backup.jshc");
    assertNotNull(fileInfo);
    assertEquals("config.backup.v1", fileInfo.getName());  // Should remove only last extension
    assertEquals("data/config.backup.v1.jshc", fileInfo.getPath());
  }

  @Test
  public void testWindowsPathHandling() {
    String[] dataFiles = {"windows.jshc=C:\\test_data\\windows_file.jshc"};
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    JoshJobFileInfo fileInfo = job.getFileInfo("windows.jshc");
    assertNotNull(fileInfo);
    assertEquals("windows_file", fileInfo.getName());
    assertEquals("C:\\test_data\\windows_file.jshc", fileInfo.getPath());
  }

  @Test
  public void testTrailingSemicolon() {
    String[] dataFiles = {"example.jshc=test_data/example_1.jshc;"};
    
    // Should fail due to empty second filespec after semicolon
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> parser.parseDataFiles(builder, dataFiles)
    );
    
    assertTrue(exception.getMessage().contains("Failed to parse job variation"));
  }

  @Test
  public void testMultipleSpaces() {
    String[] dataFiles = {
        "example.jshc   =   test_data/example_1.jshc   ;   other.jshd   =   test_data/other_1.jshd"
    };
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
  }

  @Test
  public void testThreeFileSpecifications() {
    String[] dataFiles = {
        "first.jshc=path1.jshc;second.jshd=path2.jshd;third.jshc=path3.jshc"
    };
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals("path1.jshc", job.getFilePath("first.jshc"));
    assertEquals("path2.jshd", job.getFilePath("second.jshd"));
    assertEquals("path3.jshc", job.getFilePath("third.jshc"));
    assertEquals(3, job.getFileNames().size());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testBackwardCompatibilityWithLegacyBuilding() {
    // Test that new parser integrates well with legacy builder methods
    builder.setReplicates(10).setFilePath("legacy.jshc", "legacy_path.jshc");
    
    String[] dataFiles = {"new.jshc=new_path.jshc;another.jshd=another_path.jshd"};
    
    JoshJob job = parser.parseDataFiles(builder, dataFiles).build();
    
    assertEquals(10, job.getReplicates());
    assertEquals(3, job.getFileNames().size());
    assertEquals("legacy_path.jshc", job.getFilePath("legacy.jshc"));
    assertEquals("new_path.jshc", job.getFilePath("new.jshc"));
    assertEquals("another_path.jshd", job.getFilePath("another.jshd"));
  }
}