/**
 * Tests for LocalOutputStreamStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Test a local file output stream.
 */
class LocalOutputStreamStrategyTest {

  @Test
  void testOpenMethodCreatesFileOutputStream() throws IOException {
    // Arrange
    String filePath = "/tmp/josh-open-test.txt";
    File file = new File(filePath);
    if (file.exists()) {
      assertTrue(file.delete());
    }
    LocalOutputStreamStrategy strategy = new LocalOutputStreamStrategy(filePath);

    // Act
    OutputStream outputStream = strategy.open();

    // Assert
    assertNotNull(outputStream);
    assertTrue(file.exists());
    outputStream.close();
    assertTrue(file.delete());
  }

  @Test
  void testOverwriteMode() throws IOException {
    // Arrange
    String filePath = "/tmp/josh-overwrite-test.txt";
    File file = new File(filePath);
    if (file.exists()) {
      assertTrue(file.delete());
    }

    // Write initial content
    LocalOutputStreamStrategy strategy1 = new LocalOutputStreamStrategy(filePath, false);
    try (OutputStream out1 = strategy1.open()) {
      out1.write("First write\n".getBytes(StandardCharsets.UTF_8));
    }

    // Overwrite with new content
    LocalOutputStreamStrategy strategy2 = new LocalOutputStreamStrategy(filePath, false);
    try (OutputStream out2 = strategy2.open()) {
      out2.write("Second write\n".getBytes(StandardCharsets.UTF_8));
    }

    // Assert
    String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    assertEquals("Second write\n", content);
    assertTrue(file.delete());
  }

  @Test
  void testAppendMode() throws IOException {
    // Arrange
    String filePath = "/tmp/josh-append-test.txt";
    File file = new File(filePath);
    if (file.exists()) {
      assertTrue(file.delete());
    }

    // Write initial content in overwrite mode
    LocalOutputStreamStrategy strategy1 = new LocalOutputStreamStrategy(filePath, false);
    try (OutputStream out1 = strategy1.open()) {
      out1.write("First write\n".getBytes(StandardCharsets.UTF_8));
    }

    // Append additional content
    LocalOutputStreamStrategy strategy2 = new LocalOutputStreamStrategy(filePath, true);
    try (OutputStream out2 = strategy2.open()) {
      out2.write("Second write\n".getBytes(StandardCharsets.UTF_8));
    }

    // Append more content
    LocalOutputStreamStrategy strategy3 = new LocalOutputStreamStrategy(filePath, true);
    try (OutputStream out3 = strategy3.open()) {
      out3.write("Third write\n".getBytes(StandardCharsets.UTF_8));
    }

    // Assert
    String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    assertEquals("First write\nSecond write\nThird write\n", content);
    assertTrue(file.delete());
  }

  @Test
  void testSequentialReplicateWrites() throws IOException {
    // Arrange
    String filePath = "/tmp/josh-replicate-test.txt";
    File file = new File(filePath);
    if (file.exists()) {
      assertTrue(file.delete());
    }

    // Act - Simulate 3 replicates writing to the same file
    // Each replicate opens, writes multiple lines, and closes
    for (int replicate = 0; replicate < 3; replicate++) {
      LocalOutputStreamStrategy strategy = new LocalOutputStreamStrategy(filePath, true);
      try (OutputStream out = strategy.open()) {
        for (int line = 0; line < 10; line++) {
          String content = String.format("Replicate-%d-Line-%d\n", replicate, line);
          out.write(content.getBytes(StandardCharsets.UTF_8));
        }
      }
    }

    // Assert
    assertTrue(file.exists(), "Output file should exist");
    List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
    assertEquals(30, lines.size(), "Should have exactly 30 lines (3 replicates * 10 lines)");

    // Verify all replicates present
    long replicate0Count = lines.stream().filter(l -> l.startsWith("Replicate-0-")).count();
    long replicate1Count = lines.stream().filter(l -> l.startsWith("Replicate-1-")).count();
    long replicate2Count = lines.stream().filter(l -> l.startsWith("Replicate-2-")).count();

    assertEquals(10, replicate0Count, "Should have 10 lines from replicate 0");
    assertEquals(10, replicate1Count, "Should have 10 lines from replicate 1");
    assertEquals(10, replicate2Count, "Should have 10 lines from replicate 2");

    assertTrue(file.delete());
  }

  @Test
  void testBackwardCompatibilityDefaultConstructor() throws IOException {
    // Arrange
    String filePath = "/tmp/josh-backward-compat-test.txt";
    File file = new File(filePath);
    if (file.exists()) {
      assertTrue(file.delete());
    }

    // Write initial content
    try (OutputStream out = new LocalOutputStreamStrategy(filePath).open()) {
      out.write("First write\n".getBytes(StandardCharsets.UTF_8));
    }

    // Write again with default constructor (should overwrite)
    try (OutputStream out = new LocalOutputStreamStrategy(filePath).open()) {
      out.write("Second write\n".getBytes(StandardCharsets.UTF_8));
    }

    // Assert - default should overwrite
    String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    assertEquals("Second write\n", content);
    assertTrue(file.delete());
  }

}
