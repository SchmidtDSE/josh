/**
 * Tests for TestMetadata.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the TestMetadata parser which extracts metadata from Josh test files.
 */
class TestMetadataTest {

  @TempDir
  Path tempDir;

  @Test
  void testParseCompleteMetadata() throws Exception {
    String content = """
        # @category: lifecycle
        # @subcategory: events
        # @priority: critical
        # @issue: #123
        # @description: Entities should execute at all timesteps when using .end handlers

        start simulation TestSimulation
          steps.low = 0
          steps.high = 10
        end simulation
        """;

    Path testFile = tempDir.resolve("test_complete.josh");
    Files.writeString(testFile, content);

    TestMetadata metadata = TestMetadata.parse(testFile);

    assertNotNull(metadata);
    assertEquals("lifecycle", metadata.category);
    assertEquals("events", metadata.subcategory);
    assertEquals("critical", metadata.priority);
    assertEquals("#123", metadata.issue);
    assertEquals("Entities should execute at all timesteps when using .end handlers",
        metadata.description);
  }

  @Test
  void testParsePartialMetadata() throws Exception {
    String content = """
        # @category: types
        # @priority: high
        # @description: Test partial metadata parsing

        start simulation TestSimulation
        end simulation
        """;

    Path testFile = tempDir.resolve("test_partial.josh");
    Files.writeString(testFile, content);

    TestMetadata metadata = TestMetadata.parse(testFile);

    assertNotNull(metadata);
    assertEquals("types", metadata.category);
    assertNull(metadata.subcategory);
    assertEquals("high", metadata.priority);
    assertNull(metadata.issue);
    assertEquals("Test partial metadata parsing", metadata.description);
  }

  @Test
  void testParseNoMetadata() throws Exception {
    String content = """
        start simulation TestSimulation
          steps.low = 0
          steps.high = 10
        end simulation
        """;

    Path testFile = tempDir.resolve("test_no_metadata.josh");
    Files.writeString(testFile, content);

    TestMetadata metadata = TestMetadata.parse(testFile);

    assertNotNull(metadata);
    assertNull(metadata.category);
    assertNull(metadata.subcategory);
    assertNull(metadata.priority);
    assertNull(metadata.issue);
    assertNull(metadata.description);
  }

  @Test
  void testParseStopsAtFirstNonComment() throws Exception {
    String content = """
        # @category: spatial
        # @priority: medium

        start simulation TestSimulation
        end simulation

        # @description: This should not be parsed
        """;

    Path testFile = tempDir.resolve("test_stop_parsing.josh");
    Files.writeString(testFile, content);

    TestMetadata metadata = TestMetadata.parse(testFile);

    assertNotNull(metadata);
    assertEquals("spatial", metadata.category);
    assertEquals("medium", metadata.priority);
    assertNull(metadata.description);
  }

  @Test
  void testParseWhitespaceHandling() throws Exception {
    String content = """
        #   @category:   lifecycle
        # @priority:critical
        #@description:    Test with extra whitespace

        start simulation TestSimulation
        end simulation
        """;

    Path testFile = tempDir.resolve("test_whitespace.josh");
    Files.writeString(testFile, content);

    TestMetadata metadata = TestMetadata.parse(testFile);

    assertNotNull(metadata);
    assertEquals("lifecycle", metadata.category);
    assertEquals("critical", metadata.priority);
    assertEquals("Test with extra whitespace", metadata.description);
  }

  @Test
  void testParseUnknownMetadataKeys() throws Exception {
    String content = """
        # @category: lifecycle
        # @unknown: some value
        # @priority: high
        # @anotherUnknown: another value

        start simulation TestSimulation
        end simulation
        """;

    Path testFile = tempDir.resolve("test_unknown_keys.josh");
    Files.writeString(testFile, content);

    TestMetadata metadata = TestMetadata.parse(testFile);

    assertNotNull(metadata);
    assertEquals("lifecycle", metadata.category);
    assertEquals("high", metadata.priority);
  }

  @Test
  void testParseEmptyFile() throws Exception {
    String content = "";

    Path testFile = tempDir.resolve("test_empty.josh");
    Files.writeString(testFile, content);

    TestMetadata metadata = TestMetadata.parse(testFile);

    assertNotNull(metadata);
    assertNull(metadata.category);
    assertNull(metadata.subcategory);
    assertNull(metadata.priority);
    assertNull(metadata.issue);
    assertNull(metadata.description);
  }

  @Test
  void testParseOnlyComments() throws Exception {
    String content = """
        # This is a regular comment
        # Another comment without metadata
        # @category: lifecycle
        # More comments
        """;

    Path testFile = tempDir.resolve("test_only_comments.josh");
    Files.writeString(testFile, content);

    TestMetadata metadata = TestMetadata.parse(testFile);

    assertNotNull(metadata);
    assertEquals("lifecycle", metadata.category);
    assertNull(metadata.subcategory);
    assertNull(metadata.priority);
    assertNull(metadata.issue);
    assertNull(metadata.description);
  }

  @Test
  void testParseEmptyLinesBeforeContent() throws Exception {
    String content = """
        # @category: lifecycle
        # @priority: critical



        start simulation TestSimulation
        end simulation
        """;

    Path testFile = tempDir.resolve("test_empty_lines.josh");
    Files.writeString(testFile, content);

    TestMetadata metadata = TestMetadata.parse(testFile);

    assertNotNull(metadata);
    assertEquals("lifecycle", metadata.category);
    assertEquals("critical", metadata.priority);
  }

  @Test
  void testParseMultilineDescription() throws Exception {
    String content = """
        # @category: lifecycle
        # @description: Description with special characters: @#$%
        # @priority: high

        start simulation TestSimulation
        end simulation
        """;

    Path testFile = tempDir.resolve("test_multiline_desc.josh");
    Files.writeString(testFile, content);

    TestMetadata metadata = TestMetadata.parse(testFile);

    assertNotNull(metadata);
    assertEquals("lifecycle", metadata.category);
    assertEquals("Description with special characters: @#$%",
        metadata.description);
    assertEquals("high", metadata.priority);
  }

  @Test
  void testConstructor() {
    TestMetadata metadata = new TestMetadata(
        "lifecycle",
        "events",
        "critical",
        "#456",
        "Test description"
    );

    assertEquals("lifecycle", metadata.category);
    assertEquals("events", metadata.subcategory);
    assertEquals("critical", metadata.priority);
    assertEquals("#456", metadata.issue);
    assertEquals("Test description", metadata.description);
  }

  @Test
  void testConstructorWithNulls() {
    TestMetadata metadata = new TestMetadata(null, null, null, null, null);

    assertNull(metadata.category);
    assertNull(metadata.subcategory);
    assertNull(metadata.priority);
    assertNull(metadata.issue);
    assertNull(metadata.description);
  }
}
