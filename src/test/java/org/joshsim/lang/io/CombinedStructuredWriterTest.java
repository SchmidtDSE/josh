/**
 * Tests for CombinedStructuredWriter.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CombinedStructuredWriter.
 *
 * <p>These tests verify that CombinedStructuredWriter correctly routes structured data
 * to the appropriate per-entity-type writers based on ThreadLocal context.</p>
 */
public class CombinedStructuredWriterTest {

  private MockStructuredWriter patchWriter;
  private MockStructuredWriter organismWriter;
  private MockStructuredWriter metaWriter;
  private CombinedStructuredWriter combinedWriter;

  /**
   * Set up test fixtures before each test.
   */
  @BeforeEach
  public void setUp() {
    patchWriter = new MockStructuredWriter("patch");
    organismWriter = new MockStructuredWriter("organism");
    metaWriter = new MockStructuredWriter("meta");

    Map<String, OutputWriter<Map<String, String>>> writersByType = new HashMap<>();
    writersByType.put("patch", patchWriter);
    writersByType.put("organism", organismWriter);
    writersByType.put("meta", metaWriter);

    combinedWriter = new CombinedStructuredWriter(writersByType);
  }

  @Test
  public void testRouteToPatchWriter() {
    combinedWriter.setCurrentEntityType("patch");

    Map<String, String> row = new LinkedHashMap<>();
    row.put("biomass", "100.5");
    combinedWriter.write(row, 50);

    assertEquals(1, patchWriter.getWriteCount());
    assertEquals(0, organismWriter.getWriteCount());
    assertEquals(0, metaWriter.getWriteCount());
    assertEquals(row, patchWriter.getLastRow());
    assertEquals(50L, patchWriter.getLastStep());
  }

  @Test
  public void testRouteToOrganismWriter() {
    combinedWriter.setCurrentEntityType("organism");

    Map<String, String> row = new LinkedHashMap<>();
    row.put("age", "5.0");
    combinedWriter.write(row, 100);

    assertEquals(0, patchWriter.getWriteCount());
    assertEquals(1, organismWriter.getWriteCount());
    assertEquals(0, metaWriter.getWriteCount());
    assertEquals(row, organismWriter.getLastRow());
    assertEquals(100L, organismWriter.getLastStep());
  }

  @Test
  public void testRouteToMetaWriter() {
    combinedWriter.setCurrentEntityType("meta");

    Map<String, String> row = new LinkedHashMap<>();
    row.put("totalBiomass", "1000.0");
    combinedWriter.write(row, 200);

    assertEquals(0, patchWriter.getWriteCount());
    assertEquals(0, organismWriter.getWriteCount());
    assertEquals(1, metaWriter.getWriteCount());
    assertEquals(row, metaWriter.getLastRow());
    assertEquals(200L, metaWriter.getLastStep());
  }

  @Test
  public void testUnconfiguredEntityType() {
    // Set to entity type that doesn't have a writer
    combinedWriter.setCurrentEntityType("unconfigured");

    Map<String, String> row = new LinkedHashMap<>();
    row.put("value", "123");
    combinedWriter.write(row, 75);

    // No writers should receive the write (silent no-op)
    assertEquals(0, patchWriter.getWriteCount());
    assertEquals(0, organismWriter.getWriteCount());
    assertEquals(0, metaWriter.getWriteCount());
  }

  @Test
  public void testGetCurrentEntityType() {
    assertNull(combinedWriter.getCurrentEntityType());

    combinedWriter.setCurrentEntityType("patch");
    assertEquals("patch", combinedWriter.getCurrentEntityType());

    combinedWriter.setCurrentEntityType("organism");
    assertEquals("organism", combinedWriter.getCurrentEntityType());
  }

  @Test
  public void testClearCurrentEntityType() {
    combinedWriter.setCurrentEntityType("patch");
    assertEquals("patch", combinedWriter.getCurrentEntityType());

    combinedWriter.clearCurrentEntityType();
    assertNull(combinedWriter.getCurrentEntityType());
  }

  @Test
  public void testIsConfigured() {
    assertTrue(combinedWriter.isConfigured("patch"));
    assertTrue(combinedWriter.isConfigured("organism"));
    assertTrue(combinedWriter.isConfigured("meta"));
    assertFalse(combinedWriter.isConfigured("unconfigured"));
  }

  @Test
  public void testGetWriterCount() {
    assertEquals(3, combinedWriter.getWriterCount());
  }

  @Test
  public void testStartCallsAllWriters() {
    combinedWriter.start();

    assertTrue(patchWriter.isStarted());
    assertTrue(organismWriter.isStarted());
    assertTrue(metaWriter.isStarted());
  }

  @Test
  public void testJoinCallsAllWriters() {
    combinedWriter.join();

    assertTrue(patchWriter.isJoined());
    assertTrue(organismWriter.isJoined());
    assertTrue(metaWriter.isJoined());
  }

  @Test
  public void testGetPath() {
    combinedWriter.setCurrentEntityType("patch");
    assertEquals("patch-path", combinedWriter.getPath());

    combinedWriter.setCurrentEntityType("organism");
    assertEquals("organism-path", combinedWriter.getPath());

    combinedWriter.setCurrentEntityType("unconfigured");
    assertEquals("", combinedWriter.getPath());

    combinedWriter.clearCurrentEntityType();
    assertEquals("", combinedWriter.getPath());
  }

  /**
   * Mock OutputWriter for testing that tracks write calls.
   */
  private static class MockStructuredWriter implements OutputWriter<Map<String, String>> {
    private final String path;
    private int writeCount = 0;
    private Map<String, String> lastRow = null;
    private long lastStep = -1;
    private boolean started = false;
    private boolean joined = false;

    public MockStructuredWriter(String entityType) {
      this.path = entityType + "-path";
    }

    @Override
    public void start() {
      started = true;
    }

    @Override
    public void join() {
      joined = true;
    }

    @Override
    public void write(Map<String, String> data, long step) {
      writeCount++;
      lastRow = data;
      lastStep = step;
    }

    @Override
    public String getPath() {
      return path;
    }

    public int getWriteCount() {
      return writeCount;
    }

    public Map<String, String> getLastRow() {
      return lastRow;
    }

    public long getLastStep() {
      return lastStep;
    }

    public boolean isStarted() {
      return started;
    }

    public boolean isJoined() {
      return joined;
    }
  }
}
