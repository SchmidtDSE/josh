/**
 * Tests for BlockingQueueInputStream.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BlockingQueueInputStream, verifying blocking behavior,
 * EOF signaling, exception propagation, and concurrent read/write operations.
 */
class BlockingQueueInputStreamTest {

  private static final int CHUNK_SIZE = 256 * 1024; // 256KB
  private static final int QUEUE_CAPACITY = 8;

  @Test
  void testRead_singleByte() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
    stream.enqueue(data);
    stream.signalEof();

    // Act & Assert
    assertEquals('H', stream.read());
    assertEquals('e', stream.read());
    assertEquals('l', stream.read());
    assertEquals('l', stream.read());
    assertEquals('o', stream.read());
    assertEquals(-1, stream.read()); // EOF
    assertEquals(-1, stream.read()); // Subsequent reads return EOF
  }

  @Test
  void testRead_byteArray() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    byte[] chunk1 = "First chunk ".getBytes(StandardCharsets.UTF_8);
    byte[] chunk2 = "Second chunk".getBytes(StandardCharsets.UTF_8);
    stream.enqueue(chunk1);
    stream.enqueue(chunk2);
    stream.signalEof();

    // Act
    byte[] buffer = new byte[100];
    int bytesRead1 = stream.read(buffer, 0, 50);
    int bytesRead2 = stream.read(buffer, bytesRead1, 50);
    int bytesRead3 = stream.read(buffer, 0, 1);

    // Assert
    assertEquals(chunk1.length, bytesRead1);
    assertEquals(chunk2.length, bytesRead2);
    assertEquals(-1, bytesRead3); // EOF

    byte[] result = new byte[bytesRead1 + bytesRead2];
    System.arraycopy(buffer, 0, result, 0, result.length);
    assertArrayEquals("First chunk Second chunk".getBytes(StandardCharsets.UTF_8), result);
  }

  @Test
  void testRead_byteArray_edgeCases() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    byte[] data = "Test".getBytes(StandardCharsets.UTF_8);
    stream.enqueue(data);
    stream.signalEof();

    // Act & Assert
    byte[] buffer = new byte[10];

    // len = 0 should return 0
    assertEquals(0, stream.read(buffer, 0, 0));

    // Normal read
    int bytesRead = stream.read(buffer, 0, 2);
    assertEquals(2, bytesRead);
    assertEquals('T', buffer[0]);
    assertEquals('e', buffer[1]);

    // Read remaining
    bytesRead = stream.read(buffer, 0, 10);
    assertEquals(2, bytesRead); // Only 2 bytes remaining
    assertEquals('s', buffer[0]);
    assertEquals('t', buffer[1]);

    // EOF
    assertEquals(-1, stream.read(buffer, 0, 10));
  }

  @Test
  void testRead_nullBuffer_throwsNullPointerException() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);

    // Act & Assert
    assertThrows(NullPointerException.class, () -> stream.read(null, 0, 10));
  }

  @Test
  void testRead_invalidOffsetLength_throwsIndexOutOfBoundsException() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    byte[] buffer = new byte[10];

    // Act & Assert
    assertThrows(IndexOutOfBoundsException.class, () -> stream.read(buffer, -1, 5));
    assertThrows(IndexOutOfBoundsException.class, () -> stream.read(buffer, 0, -1));
    assertThrows(IndexOutOfBoundsException.class, () -> stream.read(buffer, 0, 11));
    assertThrows(IndexOutOfBoundsException.class, () -> stream.read(buffer, 5, 6));
  }

  @Test
  void testRead_blockingBehavior() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    AtomicBoolean readerBlocked = new AtomicBoolean(false);
    AtomicBoolean readerUnblocked = new AtomicBoolean(false);
    CountDownLatch readerStarted = new CountDownLatch(1);

    // Act - Start reader thread that will block
    Thread reader = new Thread(() -> {
      try {
        readerStarted.countDown();
        readerBlocked.set(true);
        stream.read(); // This should block until data available
        readerUnblocked.set(true);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    reader.start();

    // Wait for reader to start
    assertTrue(readerStarted.await(1, TimeUnit.SECONDS));
    Thread.sleep(100); // Give reader time to block

    // Assert - Reader should be blocked
    assertTrue(readerBlocked.get());
    assertFalse(readerUnblocked.get());

    // Enqueue data - reader should unblock
    stream.enqueue(new byte[]{'A'});
    reader.join(1000);

    // Assert - Reader should have unblocked
    assertTrue(readerUnblocked.get());
  }

  @Test
  void testEofSignaling() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    stream.enqueue("Chunk 1 ".getBytes(StandardCharsets.UTF_8));
    stream.enqueue("Chunk 2".getBytes(StandardCharsets.UTF_8));
    stream.signalEof();

    // Act - Read all chunks
    byte[] buffer = new byte[100];
    int total = 0;
    int bytesRead;
    while ((bytesRead = stream.read(buffer, total, buffer.length - total)) != -1) {
      total += bytesRead;
    }

    // Assert
    assertEquals(15, total); // "Chunk 1 Chunk 2"
    assertEquals("Chunk 1 Chunk 2", new String(buffer, 0, total, StandardCharsets.UTF_8));

    // Subsequent reads return -1
    assertEquals(-1, stream.read());
    assertEquals(-1, stream.read(buffer, 0, buffer.length));
  }

  @Test
  void testEofSignaling_idempotent() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    stream.enqueue("Test".getBytes(StandardCharsets.UTF_8));

    // Act
    stream.signalEof();
    stream.signalEof(); // Call twice
    stream.signalEof(); // And again

    // Assert - Should only get one EOF
    byte[] buffer = new byte[100];
    int bytesRead = stream.read(buffer, 0, buffer.length);
    assertEquals(4, bytesRead);
    assertEquals(-1, stream.read(buffer, 0, buffer.length));
  }

  @Test
  void testExceptionPropagation() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    RuntimeException testException = new RuntimeException("Test error from writer");

    // Act
    stream.signalException(testException);

    // Assert - Next read should throw IOException wrapping the original exception
    IOException thrown = assertThrows(IOException.class, () -> stream.read());
    assertNotNull(thrown.getCause());
    assertEquals(testException, thrown.getCause());
    assertTrue(thrown.getMessage().contains("Writer thread encountered error"));
  }

  @Test
  void testExceptionPropagation_afterData() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    stream.enqueue("Data before error".getBytes(StandardCharsets.UTF_8));

    // Assert - Can still read existing data even if exception signaled
    byte[] buffer = new byte[100];
    int bytesRead = stream.read(buffer, 0, buffer.length);
    assertEquals(17, bytesRead);

    // Act - Signal exception AFTER reading the data
    RuntimeException testException = new RuntimeException("Error occurred");
    stream.signalException(testException);

    // But next read (which would block for more data) throws exception
    IOException thrown = assertThrows(IOException.class,
        () -> stream.read(buffer, 0, buffer.length));
    assertEquals(testException, thrown.getCause());
  }

  @Test
  void testConcurrentWriteRead() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    int numChunks = 20;
    CountDownLatch writerDone = new CountDownLatch(1);
    CountDownLatch readerDone = new CountDownLatch(1);
    List<byte[]> writtenChunks = new ArrayList<>();
    AtomicReference<byte[]> readData = new AtomicReference<>(new byte[0]);

    // Act - Writer thread
    Thread writer = new Thread(() -> {
      try {
        for (int i = 0; i < numChunks; i++) {
          String data = "Chunk " + i + "\n";
          byte[] chunk = data.getBytes(StandardCharsets.UTF_8);
          writtenChunks.add(chunk);
          stream.enqueue(chunk);
        }
        stream.signalEof();
        writerDone.countDown();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    // Reader thread
    Thread reader = new Thread(() -> {
      try {
        byte[] buffer = new byte[1024];
        List<Byte> allBytes = new ArrayList<>();
        int bytesRead;
        while ((bytesRead = stream.read(buffer, 0, buffer.length)) != -1) {
          for (int i = 0; i < bytesRead; i++) {
            allBytes.add(buffer[i]);
          }
        }
        byte[] result = new byte[allBytes.size()];
        for (int i = 0; i < allBytes.size(); i++) {
          result[i] = allBytes.get(i);
        }
        readData.set(result);
        readerDone.countDown();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    writer.start();
    reader.start();

    // Assert - Wait for completion
    assertTrue(writerDone.await(5, TimeUnit.SECONDS));
    assertTrue(readerDone.await(5, TimeUnit.SECONDS));

    // Verify all data was read correctly
    int expectedSize = 0;
    for (byte[] chunk : writtenChunks) {
      expectedSize += chunk.length;
    }
    assertEquals(expectedSize, readData.get().length);

    // Verify content
    StringBuilder expected = new StringBuilder();
    for (int i = 0; i < numChunks; i++) {
      expected.append("Chunk ").append(i).append("\n");
    }
    assertEquals(expected.toString(), new String(readData.get(), StandardCharsets.UTF_8));
  }

  @Test
  void testBackpressure() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    AtomicBoolean writerBlocked = new AtomicBoolean(false);
    CountDownLatch queueFull = new CountDownLatch(1);

    // Act - Fill queue to capacity
    for (int i = 0; i < QUEUE_CAPACITY; i++) {
      stream.enqueue(("Chunk " + i).getBytes(StandardCharsets.UTF_8));
    }

    // Try to enqueue one more - should block
    Thread writer = new Thread(() -> {
      try {
        queueFull.countDown();
        writerBlocked.set(true);
        stream.enqueue("Overflow chunk".getBytes(StandardCharsets.UTF_8));
        writerBlocked.set(false);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    writer.start();

    // Wait for writer to reach blocking call
    assertTrue(queueFull.await(1, TimeUnit.SECONDS));
    Thread.sleep(200); // Give writer time to block on enqueue

    // Assert - Writer should be blocked
    assertTrue(writerBlocked.get());

    // Read one chunk - this should unblock writer
    byte[] buffer = new byte[100];
    int bytesRead = stream.read(buffer, 0, buffer.length);
    assertTrue(bytesRead > 0);

    // Wait for writer to complete
    writer.join(1000);

    // Assert - Writer should have unblocked
    assertFalse(writerBlocked.get());
  }

  @Test
  void testEnqueue_nullChunk_throwsIllegalArgumentException() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> stream.enqueue(null));
  }

  @Test
  void testRead_acrossChunkBoundaries() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    stream.enqueue("ABC".getBytes(StandardCharsets.UTF_8));
    stream.enqueue("DEF".getBytes(StandardCharsets.UTF_8));
    stream.enqueue("GHI".getBytes(StandardCharsets.UTF_8));
    stream.signalEof();

    // Act - Read in a way that spans chunk boundaries
    byte[] buffer = new byte[100];
    int bytesRead1 = stream.read(buffer, 0, 5); // Should read ABC + DE
    int bytesRead2 = stream.read(buffer, 5, 5); // Should read F + GHI

    // Assert
    assertEquals(5, bytesRead1);
    assertEquals(4, bytesRead2); // Only 4 bytes remain
    assertEquals("ABCDEFGHI", new String(buffer, 0, 9, StandardCharsets.UTF_8));
    assertEquals(-1, stream.read(buffer, 0, buffer.length));
  }

  @Test
  void testClose_doesNotBlockReading() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    stream.enqueue("Test data".getBytes(StandardCharsets.UTF_8));
    stream.signalEof();

    // Act
    stream.close();

    // Assert - Should still be able to read after close
    byte[] buffer = new byte[100];
    int bytesRead = stream.read(buffer, 0, buffer.length);
    assertEquals(9, bytesRead);
    assertEquals("Test data", new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
  }

  @Test
  void testInterruptedWhileReading_throwsIoException() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    AtomicReference<Exception> caughtException = new AtomicReference<>();
    CountDownLatch readerStarted = new CountDownLatch(1);

    // Act - Start reader thread that will block
    Thread reader = new Thread(() -> {
      try {
        readerStarted.countDown();
        stream.read(); // Will block waiting for data
      } catch (Exception e) {
        caughtException.set(e);
      }
    });
    reader.start();

    // Wait for reader to start and block
    assertTrue(readerStarted.await(1, TimeUnit.SECONDS));
    Thread.sleep(100);

    // Interrupt the reader
    reader.interrupt();
    reader.join(1000);

    // Assert - Should have caught IOException
    assertNotNull(caughtException.get());
    assertTrue(caughtException.get() instanceof IOException);
    assertTrue(caughtException.get().getMessage().contains("Interrupted while reading from queue"));
  }

  @Test
  void testEmptyStream_immediateEof() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    stream.signalEof();

    // Act & Assert
    assertEquals(-1, stream.read());
    byte[] buffer = new byte[100];
    assertEquals(-1, stream.read(buffer, 0, buffer.length));
  }

  @Test
  void testLargeData_multipleChunks() throws Exception {
    // Arrange
    BlockingQueueInputStream stream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    int numChunks = 5;
    int chunkDataSize = 1024; // 1KB per chunk
    byte[] expectedData = new byte[numChunks * chunkDataSize];

    // Enqueue multiple chunks with known pattern
    for (int i = 0; i < numChunks; i++) {
      byte[] chunk = new byte[chunkDataSize];
      byte value = (byte) (i + 'A');
      for (int j = 0; j < chunkDataSize; j++) {
        chunk[j] = value;
        expectedData[i * chunkDataSize + j] = value;
      }
      stream.enqueue(chunk);
    }
    stream.signalEof();

    // Act - Read all data
    byte[] actualData = new byte[numChunks * chunkDataSize];
    int totalRead = 0;
    int bytesRead;
    byte[] buffer = new byte[512]; // Read in smaller chunks than written
    while ((bytesRead = stream.read(buffer, 0, buffer.length)) != -1) {
      System.arraycopy(buffer, 0, actualData, totalRead, bytesRead);
      totalRead += bytesRead;
    }

    // Assert
    assertEquals(expectedData.length, totalRead);
    assertArrayEquals(expectedData, actualData);
  }
}
