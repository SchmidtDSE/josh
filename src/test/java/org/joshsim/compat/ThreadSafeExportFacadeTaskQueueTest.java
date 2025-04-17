/**
 * Tests for thread safe export facade task queue.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import org.joshsim.lang.export.ExportTask;
import org.junit.jupiter.api.Test;


/**
 * Tests for thread safe export facade task queue.
 */
public class ThreadSafeExportFacadeTaskQueueTest {

  @Test
  public void testDequeueFromEmptyQueue() {
    ThreadSafeExportFacadeTaskQueue taskQueue = new ThreadSafeExportFacadeTaskQueue();
    assertNull(taskQueue.dequeue(), "Dequeue from an empty queue should return null.");
  }

  @Test
  public void testDequeueSingleElement() {
    ThreadSafeExportFacadeTaskQueue taskQueue = new ThreadSafeExportFacadeTaskQueue();
    ExportTask mockTask = mock(ExportTask.class);

    taskQueue.enqueue(mockTask);
    ExportTask dequeuedTask = taskQueue.dequeue();

    assertNotNull(dequeuedTask, "Dequeued task should not be null.");
    assertEquals(mockTask, dequeuedTask, "Dequeued task should match the enqueued task.");
  }

  @Test
  public void testDequeueMultipleElements() {
    ThreadSafeExportFacadeTaskQueue taskQueue = new ThreadSafeExportFacadeTaskQueue();
    ExportTask mockTask1 = mock(ExportTask.class);
    ExportTask mockTask2 = mock(ExportTask.class);

    taskQueue.enqueue(mockTask1);
    taskQueue.enqueue(mockTask2);

    ExportTask firstDequeued = taskQueue.dequeue();
    ExportTask secondDequeued = taskQueue.dequeue();

    assertNotNull(firstDequeued);
    assertNotNull(secondDequeued);
    assertEquals(mockTask1, firstDequeued);
    assertEquals(mockTask2, secondDequeued);
  }

  @Test
  public void testQueueBecomesEmptyAfterDequeueAll() {
    ThreadSafeExportFacadeTaskQueue taskQueue = new ThreadSafeExportFacadeTaskQueue();
    ExportTask mockTask = mock(ExportTask.class);

    taskQueue.enqueue(mockTask);
    taskQueue.dequeue();

    assertNull(taskQueue.dequeue(), "Dequeue after all tasks are dequeued should return null.");
  }
}
