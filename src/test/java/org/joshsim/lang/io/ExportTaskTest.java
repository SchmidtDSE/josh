/**
 * Unit tests for ExportTask class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.engine.entity.base.Entity;
import org.junit.jupiter.api.Test;

/**
 * Test suite for the ExportTask class.
 */
public class ExportTaskTest {

  @Test
  public void testEntityConstructor() {
    Entity mockEntity = mock(Entity.class);
    long step = 42L;

    ExportTask task = new ExportTask(mockEntity, step);

    assertTrue(task.hasEntity());
    assertFalse(task.hasNamedMap());
    assertEquals(mockEntity, task.getEntity().get());
    assertTrue(task.getNamedMap().isEmpty());
    assertEquals(step, task.getStep());
  }

  @Test
  public void testNamedMapConstructor() {
    Map<String, String> testMap = new HashMap<>();
    testMap.put("key1", "value1");
    testMap.put("key2", "value2");
    NamedMap namedMap = new NamedMap("testName", testMap);
    long step = 123L;

    ExportTask task = new ExportTask(namedMap, step);

    assertFalse(task.hasEntity());
    assertTrue(task.hasNamedMap());
    assertTrue(task.getEntity().isEmpty());
    assertEquals(namedMap, task.getNamedMap().get());
    assertEquals(step, task.getStep());
  }

  @Test
  public void testEntityConstructorNullEntity() {
    assertThrows(IllegalArgumentException.class, () -> {
      new ExportTask((Entity) null, 0L);
    });
  }

  @Test
  public void testNamedMapConstructorNullNamedMap() {
    assertThrows(IllegalArgumentException.class, () -> {
      new ExportTask((NamedMap) null, 0L);
    });
  }

  @Test
  public void testToStringWithEntity() {
    Entity mockEntity = mock(Entity.class);
    ExportTask task = new ExportTask(mockEntity, 42L);

    String result = task.toString();

    assertTrue(result.contains("ExportTask"));
    assertTrue(result.contains("entity="));
    assertTrue(result.contains("step=42"));
  }

  @Test
  public void testToStringWithNamedMap() {
    Map<String, String> testMap = new HashMap<>();
    testMap.put("test", "value");
    NamedMap namedMap = new NamedMap("testName", testMap);
    ExportTask task = new ExportTask(namedMap, 123L);

    String result = task.toString();

    assertTrue(result.contains("ExportTask"));
    assertTrue(result.contains("namedMap="));
    assertTrue(result.contains("step=123"));
  }

  @Test
  public void testStepValues() {
    Entity mockEntity = mock(Entity.class);
    long[] testSteps = {0L, 1L, 100L, Long.MAX_VALUE};

    for (long step : testSteps) {
      ExportTask task = new ExportTask(mockEntity, step);
      assertEquals(step, task.getStep());
    }
  }
}