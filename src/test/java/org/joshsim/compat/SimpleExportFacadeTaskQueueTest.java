/**
 * Tests for non-thread safe export facade task queue.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.joshsim.lang.export.ExportTask;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


/**
 * Tests for non-thread safe export facade task queue.
 */
class SimpleExportFacadeTaskQueueTest {

  @Test
  void testDequeueReturnsTaskWhenQueueIsNotEmpty() {
    // Arrange
    SimpleExportFacadeTaskQueue taskQueue = new SimpleExportFacadeTaskQueue();
    ExportTask mockTask = Mockito.mock(ExportTask.class);
    taskQueue.enqueue(mockTask);

    // Act
    ExportTask result = taskQueue.dequeue();

    // Assert
    assertEquals(mockTask, result, "Dequeue should return the first task in the queue.");
  }

  @Test
  void testDequeueReturnsNullWhenQueueIsEmpty() {
    // Arrange
    SimpleExportFacadeTaskQueue taskQueue = new SimpleExportFacadeTaskQueue();

    // Act
    ExportTask result = taskQueue.dequeue();

    // Assert
    assertNull(result, "Dequeue should return null when the queue is empty.");
  }

  @Test
  void testDequeueRemovesTaskFromQueue() {
    // Arrange
    SimpleExportFacadeTaskQueue taskQueue = new SimpleExportFacadeTaskQueue();
    ExportTask task1 = Mockito.mock(ExportTask.class);
    ExportTask task2 = Mockito.mock(ExportTask.class);
    taskQueue.enqueue(task1);
    taskQueue.enqueue(task2);

    // Act
    ExportTask firstDequeue = taskQueue.dequeue();
    ExportTask secondDequeue = taskQueue.dequeue();

    // Assert
    assertEquals(task1, firstDequeue, "The first dequeue should return the first task.");
    assertEquals(task2, secondDequeue, "The second dequeue should return the second task.");
  }
}
