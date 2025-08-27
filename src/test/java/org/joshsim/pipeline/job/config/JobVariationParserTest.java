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
import java.util.List;
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

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());

    // Should return single job for backward compatibility

    JoshJob job = results.get(0).build();
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
    assertEquals(2, job.getFileNames().size());
  }

  @Test
  public void testSingleDataFile() {
    String[] dataFiles = {"config.jshc=data/config_1.jshc"};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    assertEquals("data/config_1.jshc", job.getFilePath("config.jshc"));
    assertEquals(1, job.getFileNames().size());
  }

  @Test
  public void testEmptyDataFiles() {
    String[] dataFiles = {};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());

    // Test backward compatibility - should return single job builder
    JoshJob job = results.get(0).build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testNullDataFiles() {
    List<JoshJobBuilder> results = parser.parseDataFiles(builder, (String[]) null);
    assertEquals(1, results.size());

    // Test backward compatibility - should return single job builder
    JoshJob job = results.get(0).build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testEmptyString() {
    String[] dataFiles = {""};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());

    // Test backward compatibility - should return single job builder
    JoshJob job = results.get(0).build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testWhitespaceOnly() {
    String[] dataFiles = {"   "};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());

    // Test backward compatibility - should return single job builder
    JoshJob job = results.get(0).build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testWhitespaceHandling() {
    String[] dataFiles = {
        "  example.jshc  =  test_data/example_1.jshc  ;other.jshd= test_data/other_1.jshd  "
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
  }

  @Test
  public void testIterableInterface() {
    Iterable<String> dataFiles = Arrays.asList(
        "example.jshc=test_data/example_1.jshc;other.jshd=test_data/other_1.jshd");

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
    assertEquals(2, job.getFileNames().size());
  }

  @Test
  public void testEmptyIterable() {
    Iterable<String> dataFiles = Collections.emptyList();

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());

    // Test backward compatibility - should return single job builder
    JoshJob job = results.get(0).build();
    assertTrue(job.getFileNames().isEmpty());
  }

  @Test
  public void testNullIterable() {
    Iterable<String> dataFiles = null;

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());

    // Test backward compatibility - should return single job builder
    JoshJob job = results.get(0).build();
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
    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

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

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

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

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    assertEquals("http://example.com:8080/config.jshc", job.getFilePath("remote.jshc"));
    assertEquals("file:///tmp/data.jshd", job.getFilePath("local.jshd"));
  }

  @Test
  public void testWindowsPathSupport() {
    String[] dataFiles = {
        "config.jshc=C:\\Users\\test\\config.jshc;data.jshd=D:\\data\\file.jshd"
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    assertEquals("C:\\Users\\test\\config.jshc", job.getFilePath("config.jshc"));
    assertEquals("D:\\data\\file.jshd", job.getFilePath("data.jshd"));
  }

  @Test
  public void testCommaInPathWarning() {
    // Test malformed input: comma where semicolon should be used
    String[] dataFiles = {
        "example.jshc=test_data/example_1.jshc,other.jshd=test_data/other_1.jshd"
    };

    // This input is actually parsed as: example.jshc has two paths in its list:
    // Path 1: "test_data/example_1.jshc"
    // Path 2: "other.jshd=test_data/other_1.jshd" (equals signs allowed in paths after first token)
    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(2, results.size()); // 2 combinations from the two paths
    
    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();
    
    // Both jobs should have the example.jshc key, but with different paths
    for (JoshJob job : jobs) {
      assertTrue(job.getFileNames().contains("example.jshc"));
      assertEquals(1, job.getFileNames().size());
    }
    
    // Collect the paths for the example.jshc key
    List<String> paths = jobs.stream()
        .map(job -> job.getFilePath("example.jshc"))
        .sorted()
        .toList();
    
    // Should have both paths (one normal, one with equals in it)
    assertEquals(2, paths.size());
    assertEquals("other.jshd=test_data/other_1.jshd", paths.get(0)); // alphabetically first
    assertEquals("test_data/example_1.jshc", paths.get(1));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testIntegrationWithExistingBuilder() {
    // Pre-configure builder
    builder.setReplicates(5).setFilePath("existing.jshc", "existing_path.jshc");

    String[] dataFiles = {"new.jshd=new_path.jshd"};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

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

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    // Should overwrite the existing path
    assertEquals("new_path.jshc", job.getFilePath("config.jshc"));
    assertEquals(1, job.getFileNames().size());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testChainedCallsWithParser() {
    String[] dataFiles = {"example.jshc=path1.jshc"};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    
    JoshJob job = results.get(0)
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

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

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

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

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

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    JoshJobFileInfo fileInfo = job.getFileInfo("config.jshc");
    assertNotNull(fileInfo);
    assertEquals("config_file", fileInfo.getName());
    assertEquals("test_data/config_file", fileInfo.getPath());
  }

  @Test
  public void testFileNameExtractionWithMultipleDots() {
    String[] dataFiles = {"backup.jshc=data/config.backup.v1.jshc"};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    JoshJobFileInfo fileInfo = job.getFileInfo("backup.jshc");
    assertNotNull(fileInfo);
    assertEquals("config.backup.v1", fileInfo.getName());  // Should remove only last extension
    assertEquals("data/config.backup.v1.jshc", fileInfo.getPath());
  }

  @Test
  public void testWindowsPathHandling() {
    String[] dataFiles = {"windows.jshc=C:\\test_data\\windows_file.jshc"};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

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

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
  }

  @Test
  public void testThreeFileSpecifications() {
    String[] dataFiles = {
        "first.jshc=path1.jshc;second.jshd=path2.jshd;third.jshc=path3.jshc"
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

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

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    assertEquals(10, job.getReplicates());
    assertEquals(3, job.getFileNames().size());
    assertEquals("legacy_path.jshc", job.getFilePath("legacy.jshc"));
    assertEquals("new_path.jshc", job.getFilePath("new.jshc"));
    assertEquals("another_path.jshd", job.getFilePath("another.jshd"));
  }

  // Component 9: Comprehensive test coverage for comma-separated lists

  @Test
  public void testCommaListUsesFirstPathOnly() {
    String[] dataFiles = {"example.jshc=path1.jshc,path2.jshc,path3.jshc"};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    // Component 10: Should now create 3 combinations, not just use first path
    assertEquals(3, results.size());
    
    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();
    List<String> paths = jobs.stream()
        .map(job -> job.getFilePath("example.jshc"))
        .sorted()
        .toList();
    
    // Should have all three paths as separate combinations
    assertEquals(List.of("path1.jshc", "path2.jshc", "path3.jshc"), paths);
    
    // Check that all jobs have correct file info
    for (JoshJob job : jobs) {
      assertEquals(1, job.getFileNames().size());
      JoshJobFileInfo fileInfo = job.getFileInfo("example.jshc");
      assertNotNull(fileInfo);
    }
  }

  @Test
  public void testMultipleCommaListsUsesFirstOfEach() {
    String[] dataFiles = {
        "first.jshc=path1.jshc,path2.jshc;second.jshd=pathA.jshd,pathB.jshd,pathC.jshd"
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    // Component 10: Should create all combinations: 2 × 3 = 6
    assertEquals(6, results.size());
    
    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();
    
    // Verify all jobs have correct structure
    for (JoshJob job : jobs) {
      assertEquals(2, job.getFileNames().size());
      assertTrue(job.getFilePath("first.jshc").matches("path[12]\\.jshc"));
      assertTrue(job.getFilePath("second.jshd").matches("path[ABC]\\.jshd"));
    }
    
    // Verify we have all expected combinations
    boolean foundPath1PathA = jobs.stream().anyMatch(job ->
        "path1.jshc".equals(job.getFilePath("first.jshc"))
        && "pathA.jshd".equals(job.getFilePath("second.jshd")));
    assertTrue(foundPath1PathA, "Should find combination (path1.jshc, pathA.jshd)");
  }

  @Test
  public void testCommaListWithComplexPaths() {
    String[] dataFiles = {
        "config.jshc=C:\\Users\\test\\config1.jshc,C:\\Users\\test\\config2.jshc;"
        + "data.jshd=http://example.com/data1.jshd,file:///tmp/data2.jshd"
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    // Component 10: Should create all combinations: 2 × 2 = 4
    assertEquals(4, results.size());
    
    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();
    
    // Verify we have all expected combinations with complex paths
    boolean foundConfig1Data1 = jobs.stream().anyMatch(job ->
        "C:\\Users\\test\\config1.jshc".equals(job.getFilePath("config.jshc"))
        && "http://example.com/data1.jshd".equals(job.getFilePath("data.jshd")));
    assertTrue(foundConfig1Data1, "Should find combination with config1 and data1");
    
    boolean foundConfig2Data2 = jobs.stream().anyMatch(job ->
        "C:\\Users\\test\\config2.jshc".equals(job.getFilePath("config.jshc"))
        && "file:///tmp/data2.jshd".equals(job.getFilePath("data.jshd")));
    assertTrue(foundConfig2Data2, "Should find combination with config2 and data2");
  }

  @Test
  public void testCommaListWithEqualsInPaths() {
    String[] dataFiles = {"test.jshc=path1=value,path2=other"};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    // Component 10: Should create 2 combinations
    assertEquals(2, results.size());
    
    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();
    List<String> paths = jobs.stream()
        .map(job -> job.getFilePath("test.jshc"))
        .sorted()
        .toList();
    
    assertEquals(List.of("path1=value", "path2=other"), paths);
  }

  @Test
  public void testEmptyPathListThrowsException() {
    String[] dataFiles = {"example.jshc="};

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> parser.parseDataFiles(builder, dataFiles)
    );

    // Should get parsing error due to grammar requiring at least one TEXT_ token after equals
    assertTrue(exception.getMessage().contains("Failed to parse job variation"));
  }

  @Test
  public void testTrailingCommaThrowsException() {
    // Test that trailing comma causes a parsing error (grammar requires TEXT_ after comma)
    String[] dataFiles = {"example.jshc=path1.jshc,"};

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> parser.parseDataFiles(builder, dataFiles)
    );

    // Should get parsing error due to trailing comma
    assertTrue(exception.getMessage().contains("Failed to parse job variation"));
  }

  @Test
  public void testCommaListWithWhitespace() {
    String[] dataFiles = {"test.jshc= path1.jshc , path2.jshc , path3.jshc "};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    // Component 10: Should create 3 combinations with trimmed whitespace
    assertEquals(3, results.size());
    
    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();
    List<String> paths = jobs.stream()
        .map(job -> job.getFilePath("test.jshc"))
        .sorted()
        .toList();
    
    // Should have all three paths with whitespace trimmed
    assertEquals(List.of("path1.jshc", "path2.jshc", "path3.jshc"), paths);
  }

  @Test
  public void testSinglePathStillWorks() {
    // Ensure backward compatibility - single paths without commas still work
    String[] dataFiles = {"simple.jshc=single_path.jshc"};

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    assertEquals(1, results.size());
    JoshJob job = results.get(0).build();

    assertEquals("single_path.jshc", job.getFilePath("simple.jshc"));
    assertEquals(1, job.getFileNames().size());
  }

  @Test
  public void testMixedSingleAndListPaths() {
    String[] dataFiles = {
        "single.jshc=single_path.jshc;list.jshd=path1.jshd,path2.jshd"
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    // Component 10: Should create 2 combinations (1 × 2)
    assertEquals(2, results.size());
    
    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();
    
    // All jobs should have the same single.jshc path
    for (JoshJob job : jobs) {
      assertEquals("single_path.jshc", job.getFilePath("single.jshc"));
      assertEquals(2, job.getFileNames().size());
    }
    
    // Should have both list.jshd paths across the combinations
    List<String> listPaths = jobs.stream()
        .map(job -> job.getFilePath("list.jshd"))
        .sorted()
        .toList();
    assertEquals(List.of("path1.jshd", "path2.jshd"), listPaths);
  }

  @Test
  public void testLongCommaList() {
    String[] dataFiles = {
        "long.jshc=path1.jshc,path2.jshc,path3.jshc,path4.jshc,path5.jshc,path6.jshc"
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(builder, dataFiles);
    // Component 10: Should create 6 combinations for all paths
    assertEquals(6, results.size());
    
    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();
    List<String> paths = jobs.stream()
        .map(job -> job.getFilePath("long.jshc"))
        .sorted()
        .toList();
    
    // Should have all six paths as separate combinations
    assertEquals(List.of("path1.jshc", "path2.jshc", "path3.jshc", 
                        "path4.jshc", "path5.jshc", "path6.jshc"), paths);
    
    // Each job should have only one file
    for (JoshJob job : jobs) {
      assertEquals(1, job.getFileNames().size());
    }
  }
}
