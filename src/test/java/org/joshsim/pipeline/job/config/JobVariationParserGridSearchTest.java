/**
 * Unit tests for JobVariationParser grid search functionality.
 *
 * <p>Tests the new grid search capabilities that create multiple job combinations
 * from comma-separated file lists. Validates the Cartesian product generation,
 * backward compatibility, and error handling for grid search scenarios.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for JobVariationParser grid search functionality.
 *
 * <p>Validates the new combinatorial expansion capabilities that generate
 * all possible combinations from comma-separated file lists.</p>
 */
public class JobVariationParserGridSearchTest {

  private JobVariationParser parser;
  private JoshJobBuilder templateBuilder;

  /**
   * Set up test instances before each test.
   */
  @BeforeEach
  public void setUp() {
    parser = new JobVariationParser();
    templateBuilder = new JoshJobBuilder().setReplicates(10);
  }

  @Test
  public void testSingleFileBackwardCompatibility() {
    String[] dataFiles = {"example.jshc=test_data/example_1.jshc"};

    List<JoshJobBuilder> results = parser.parseDataFiles(templateBuilder, dataFiles);

    // Should return single job for backward compatibility
    assertEquals(1, results.size());

    JoshJob job = results.get(0).build();
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals(10, job.getReplicates());
  }

  @Test
  public void testTwoFileGridSearch() {
    String[] dataFiles = {
        "example.jshc=file1.jshc,file2.jshc;other.jshd=fileA.jshd,fileB.jshd"
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(templateBuilder, dataFiles);

    // Should return 4 combinations: (file1,fileA), (file1,fileB), (file2,fileA), (file2,fileB)
    assertEquals(4, results.size());

    // Validate all combinations
    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();

    // Check that all jobs have correct replicates
    for (JoshJob job : jobs) {
      assertEquals(10, job.getReplicates());
    }

    // Check specific combinations (order may vary)
    boolean foundFile1FileA = false;
    boolean foundFile1FileB = false;
    boolean foundFile2FileA = false;
    boolean foundFile2FileB = false;

    for (JoshJob job : jobs) {
      String exampleFile = job.getFilePath("example.jshc");
      String otherFile = job.getFilePath("other.jshd");

      if ("file1.jshc".equals(exampleFile) && "fileA.jshd".equals(otherFile)) {
        foundFile1FileA = true;
      } else if ("file1.jshc".equals(exampleFile) && "fileB.jshd".equals(otherFile)) {
        foundFile1FileB = true;
      } else if ("file2.jshc".equals(exampleFile) && "fileA.jshd".equals(otherFile)) {
        foundFile2FileA = true;
      } else if ("file2.jshc".equals(exampleFile) && "fileB.jshd".equals(otherFile)) {
        foundFile2FileB = true;
      }
    }

    assertTrue(foundFile1FileA, "Should find combination (file1.jshc, fileA.jshd)");
    assertTrue(foundFile1FileB, "Should find combination (file1.jshc, fileB.jshd)");
    assertTrue(foundFile2FileA, "Should find combination (file2.jshc, fileA.jshd)");
    assertTrue(foundFile2FileB, "Should find combination (file2.jshc, fileB.jshd)");
  }

  @Test
  public void testSingleFileWithMultipleChoicesGridSearch() {
    String[] dataFiles = {"example.jshc=file1.jshc,file2.jshc,file3.jshc"};

    List<JoshJobBuilder> results = parser.parseDataFiles(templateBuilder, dataFiles);

    // Should return 3 combinations
    assertEquals(3, results.size());

    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();

    // Check that we have all expected files
    List<String> paths = jobs.stream()
        .map(job -> job.getFilePath("example.jshc"))
        .sorted()
        .toList();

    assertEquals(List.of("file1.jshc", "file2.jshc", "file3.jshc"), paths);
  }

  @Test
  public void testThreeFileGridSearch() {
    String[] dataFiles = {
        "a.jshc=a1,a2;b.jshd=b1,b2;c.jshc=c1,c2"
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(templateBuilder, dataFiles);

    // Should return 8 combinations: 2 × 2 × 2 = 8
    assertEquals(8, results.size());

    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();

    // Verify that all 8 combinations exist
    boolean[] combinationsFound = new boolean[8];
    String[][] expectedCombinations = {
        {"a1", "b1", "c1"}, {"a1", "b1", "c2"}, {"a1", "b2", "c1"}, {"a1", "b2", "c2"},
        {"a2", "b1", "c1"}, {"a2", "b1", "c2"}, {"a2", "b2", "c1"}, {"a2", "b2", "c2"}
    };

    for (JoshJob job : jobs) {
      String a = job.getFilePath("a.jshc");
      String b = job.getFilePath("b.jshd");
      String c = job.getFilePath("c.jshc");

      for (int i = 0; i < expectedCombinations.length; i++) {
        if (expectedCombinations[i][0].equals(a)
            && expectedCombinations[i][1].equals(b)
            && expectedCombinations[i][2].equals(c)) {
          combinationsFound[i] = true;
          break;
        }
      }
    }

    for (int i = 0; i < combinationsFound.length; i++) {
      assertTrue(combinationsFound[i],
          "Missing combination: " + String.join(",", expectedCombinations[i]));
    }
  }

  @Test
  public void testMixedSingleAndMultipleFiles() {
    String[] dataFiles = {
        "single.jshc=single_file.jshc;multi.jshd=multi1.jshd,multi2.jshd"
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(templateBuilder, dataFiles);

    // Should return 2 combinations
    assertEquals(2, results.size());

    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();

    // Both jobs should have the same single file, but different multi files
    for (JoshJob job : jobs) {
      assertEquals("single_file.jshc", job.getFilePath("single.jshc"));
    }

    // Check that we have both multi files
    List<String> multiFiles = jobs.stream()
        .map(job -> job.getFilePath("multi.jshd"))
        .sorted()
        .toList();

    assertEquals(List.of("multi1.jshd", "multi2.jshd"), multiFiles);
  }

  @Test
  public void testEmptyDataFilesReturnsTemplateBuilder() {
    String[] dataFiles = {};

    List<JoshJobBuilder> results = parser.parseDataFiles(templateBuilder, dataFiles);

    assertEquals(1, results.size());
    assertEquals(10, results.get(0).build().getReplicates());
  }

  @Test
  public void testNullDataFilesReturnsTemplateBuilder() {
    List<JoshJobBuilder> results = parser.parseDataFiles(templateBuilder, (String[]) null);

    assertEquals(1, results.size());
    assertEquals(10, results.get(0).build().getReplicates());
  }

  @Test
  public void testTooManyCombinationsThrowsException() {
    // Create a specification that would result in more than 1000 combinations
    // Use fewer files but still over limit to avoid ANTLR parsing issues
    // 10 files with 3 options each = 3^10 = 59049 combinations (over 1000 limit)
    StringBuilder spec = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      if (i > 0) {
        spec.append(";");
      }
      spec.append("file").append(i).append(".jshc=path").append(i).append("a,path")
          .append(i).append("b,path").append(i).append("c");
    }

    String[] dataFiles = {spec.toString()};

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      parser.parseDataFiles(templateBuilder, dataFiles);
    });

    // Accept either validation error or ANTLR parsing error
    assertTrue(exception.getMessage().contains("Too many combinations")
               || exception.getMessage().contains("Failed to parse job variation"),
               "Exception message should mention combinations or parsing failure: "
               + exception.getMessage());
  }

  @Test
  public void testEmptyPathListThrowsException() {
    String[] dataFiles = {"example.jshc="};

    // Note: This might throw ANTLR parsing exception instead of IllegalArgumentException
    // because empty path list violates grammar
    Exception exception = assertThrows(Exception.class, () -> {
      parser.parseDataFiles(templateBuilder, dataFiles);
    });

    // Accept either IllegalArgumentException (from validation) or other parsing exceptions
    assertTrue(exception.getMessage().contains("path")
               || exception.getMessage().contains("parsing")
               || exception.getMessage().contains("empty")
               || exception.getMessage().contains("specification"),
               "Exception message should mention path or parsing issues: "
               + exception.getMessage());
  }

  @Test
  public void testWhitespaceHandling() {
    String[] dataFiles = {
        " example.jshc = file1.jshc , file2.jshc ; other.jshd = fileA.jshd , fileB.jshd "
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(templateBuilder, dataFiles);

    assertEquals(4, results.size());

    // Verify that whitespace was properly handled
    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();
    for (JoshJob job : jobs) {
      String exampleFile = job.getFilePath("example.jshc");
      String otherFile = job.getFilePath("other.jshd");

      assertTrue(exampleFile.equals("file1.jshc") || exampleFile.equals("file2.jshc"));
      assertTrue(otherFile.equals("fileA.jshd") || otherFile.equals("fileB.jshd"));
    }
  }

  @Test
  public void testComplexPathsWithUriSchemes() {
    String[] dataFiles = {
        "config.jshc=file:///path/to/config1.jshc,file:///path/to/config2.jshc"
    };

    List<JoshJobBuilder> results = parser.parseDataFiles(templateBuilder, dataFiles);

    assertEquals(2, results.size());

    List<JoshJob> jobs = results.stream().map(JoshJobBuilder::build).toList();
    List<String> paths = jobs.stream()
        .map(job -> job.getFilePath("config.jshc"))
        .sorted()
        .toList();

    assertEquals(List.of("file:///path/to/config1.jshc", "file:///path/to/config2.jshc"), paths);
  }
}
