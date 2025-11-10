/**
 * Tests for OutputWriterFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for OutputWriterFactory implementation.
 */
public class OutputWriterFactoryTest {

  @TempDir
  java.nio.file.Path tempDir;

  /**
   * Test creating a text writer for stdout.
   */
  @Test
  public void testCreateTextWriterStdout() {
    OutputWriterFactory factory = new OutputWriterFactory(0);

    OutputWriter<String> writer = factory.createTextWriter("stdout");

    assertNotNull(writer);
  }

  /**
   * Test creating a text writer for file.
   */
  @Test
  public void testCreateTextWriterFile() {
    OutputWriterFactory factory = new OutputWriterFactory(0);

    String path = tempDir.resolve("debug.txt").toAbsolutePath().toString();
    OutputWriter<String> writer = factory.createTextWriter("file://" + path);

    assertNotNull(writer);
    assertEquals(path, writer.getPath());
  }

  /**
   * Test path template resolution.
   */
  @Test
  public void testPathTemplateResolution() {
    OutputWriterFactory factory = new OutputWriterFactory(42);

    String resolved = factory.resolvePath("/tmp/debug_{replicate}.txt");

    assertEquals("/tmp/debug_42.txt", resolved);
  }

  /**
   * Test parseTarget for file URI.
   */
  @Test
  public void testParseTargetFile() {
    OutputWriterFactory factory = new OutputWriterFactory(0);

    OutputTarget target = factory.parseTarget("file:///tmp/test.txt");

    assertEquals("file", target.getProtocol());
    assertEquals("/tmp/test.txt", target.getPath());
  }

  /**
   * Test parseTarget for stdout.
   */
  @Test
  public void testParseTargetStdout() {
    OutputWriterFactory factory = new OutputWriterFactory(0);

    OutputTarget target = factory.parseTarget("stdout");

    assertEquals("stdout", target.getProtocol());
  }

  /**
   * Test creating combined text writer.
   */
  @Test
  public void testCreateCombinedTextWriter() {
    OutputWriterFactory factory = new OutputWriterFactory(0);

    Map<String, String> targets = new HashMap<>();
    targets.put("patch", "stdout");
    targets.put("organism", "stdout");

    OutputWriter<String> writer = factory.createCombinedTextWriter(targets);

    assertNotNull(writer);
    assertTrue(writer instanceof CombinedTextWriter);
  }

  /**
   * Test hasMinioSupport with no MinIO.
   */
  @Test
  public void testHasMinioSupportFalse() {
    OutputWriterFactory factory = new OutputWriterFactory(0);

    assertFalse(factory.hasMinioSupport());
  }

  /**
   * Test getReplicateNumber.
   */
  @Test
  public void testGetReplicateNumber() {
    OutputWriterFactory factory = new OutputWriterFactory(42);

    assertEquals(42, factory.getReplicateNumber());
  }

  /**
   * Test MinIO target without MinIO configured throws exception.
   */
  @Test
  public void testMinioWithoutConfigThrows() {
    OutputWriterFactory factory = new OutputWriterFactory(0);

    assertThrows(IllegalArgumentException.class, () -> {
      factory.createTextWriter("minio://bucket/path.txt");
    });
  }

  /**
   * Test memory target throws exception (not yet supported).
   */
  @Test
  public void testMemoryTargetThrows() {
    OutputWriterFactory factory = new OutputWriterFactory(0);

    assertThrows(IllegalArgumentException.class, () -> {
      factory.createTextWriter("memory://editor/output");
    });
  }
}
