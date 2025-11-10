/**
 * Tests for TextOutputWriter.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

/**
 * Tests for TextOutputWriter implementation.
 */
public class TextOutputWriterTest {

  /**
   * Test that stdout destination writes synchronously.
   */
  @Test
  public void testStdoutDestination() throws IOException {
    // Create a test output stream to capture stdout writes
    ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
    OutputStreamStrategy strategy = new TestOutputStreamStrategy(testOutput);

    OutputTarget target = new OutputTarget("stdout", "", "");
    TextOutputWriter writer = new TextOutputWriter(target, strategy);

    // Write should be synchronous for stdout
    writer.start();
    writer.write("Test message", 1);
    writer.join();

    String output = testOutput.toString();
    assertTrue(output.contains("Test message"));
    assertTrue(output.contains("Step 1"));
  }

  /**
   * Test that file destination uses async writing.
   */
  @Test
  public void testFileDestination() throws IOException {
    ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
    OutputStreamStrategy strategy = new TestOutputStreamStrategy(testOutput);

    OutputTarget target = new OutputTarget("file", "", "/tmp/test.txt");
    TextOutputWriter writer = new TextOutputWriter(target, strategy);

    writer.start();
    writer.write("File message", 2);
    writer.join();

    String output = testOutput.toString();
    assertTrue(output.contains("File message"));
    assertTrue(output.contains("Step 2"));
  }

  /**
   * Test that path is correctly returned.
   */
  @Test
  public void testGetPath() throws IOException {
    OutputStreamStrategy strategy = new TestOutputStreamStrategy(new ByteArrayOutputStream());
    OutputTarget target = new OutputTarget("file", "", "/tmp/debug.txt");

    TextOutputWriter writer = new TextOutputWriter(target, strategy);

    assertTrue(writer.getPath().equals("/tmp/debug.txt"));
  }

  /**
   * Test writing with entity type.
   */
  @Test
  public void testWriteWithEntityType() throws IOException {
    ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
    OutputStreamStrategy strategy = new TestOutputStreamStrategy(testOutput);

    OutputTarget target = new OutputTarget("stdout", "", "");
    TextOutputWriter writer = new TextOutputWriter(target, strategy);

    writer.start();
    writer.write("Entity message", 3, "ForeverTree");
    writer.join();

    String output = testOutput.toString();
    assertTrue(output.contains("Entity message"));
    assertTrue(output.contains("Step 3"));
    assertTrue(output.contains("ForeverTree"));
  }

  /**
   * Test output stream strategy for testing.
   */
  private static class TestOutputStreamStrategy implements OutputStreamStrategy {
    private final OutputStream stream;

    public TestOutputStreamStrategy(OutputStream stream) {
      this.stream = stream;
    }

    @Override
    public OutputStream open() throws IOException {
      return stream;
    }
  }
}
