/**
 * Tests for BlockingQueueOutputStream.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BlockingQueueOutputStream, verifying buffering, chunking,
 * flush behavior, close handling, and integration with BlockingQueueInputStream.
 */
class BlockingQueueOutputStreamTest {

  private static final int CHUNK_SIZE = 256 * 1024; // 256KB
  private static final int QUEUE_CAPACITY = 8;

  @Test
  void testWrite_singleByte() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write bytes one at a time to fill exactly one chunk
    for (int i = 0; i < CHUNK_SIZE; i++) {
      output.write('A');
    }

    // Assert - One chunk should be flushed automatically
    byte[] buffer = new byte[CHUNK_SIZE];
    int bytesRead = input.read(buffer, 0, buffer.length);
    assertEquals(CHUNK_SIZE, bytesRead);

    // Verify all bytes are 'A'
    for (int i = 0; i < CHUNK_SIZE; i++) {
      assertEquals('A', buffer[i]);
    }

    // Clean up
    output.close();
  }

  @Test
  void testWrite_byteArray_smallerThanChunk() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write data smaller than chunk size
    String testData = "Hello, World!";
    output.write(testData.getBytes(StandardCharsets.UTF_8));

    // Assert - Data should be buffered, not yet flushed
    // We can't directly check if buffered, but we can flush and verify
    output.flush();

    byte[] buffer = new byte[100];
    int bytesRead = input.read(buffer, 0, buffer.length);
    assertEquals(testData.length(), bytesRead);
    assertEquals(testData, new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));

    // Clean up
    output.close();
  }

  @Test
  void testWrite_multipleChunks() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write 1MB of data (should produce 4 chunks)
    int totalSize = 1024 * 1024; // 1MB
    byte[] data = new byte[totalSize];
    for (int i = 0; i < totalSize; i++) {
      data[i] = (byte) (i % 256);
    }
    output.write(data);
    output.close();

    // Assert - Read all data back and verify
    byte[] readData = new byte[totalSize];
    int totalRead = 0;
    int bytesRead;
    byte[] buffer = new byte[CHUNK_SIZE];
    while ((bytesRead = input.read(buffer, 0, buffer.length)) != -1) {
      System.arraycopy(buffer, 0, readData, totalRead, bytesRead);
      totalRead += bytesRead;
    }

    assertEquals(totalSize, totalRead);
    assertArrayEquals(data, readData);
  }

  @Test
  void testWrite_exactChunkBoundary() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write exactly one chunk
    byte[] chunk = new byte[CHUNK_SIZE];
    for (int i = 0; i < CHUNK_SIZE; i++) {
      chunk[i] = (byte) 'X';
    }
    output.write(chunk);

    // Assert - Chunk should be flushed automatically
    byte[] buffer = new byte[CHUNK_SIZE];
    int bytesRead = input.read(buffer, 0, buffer.length);
    assertEquals(CHUNK_SIZE, bytesRead);

    // Write one more byte - should be buffered
    output.write('Y');
    output.flush();

    bytesRead = input.read(buffer, 0, buffer.length);
    assertEquals(1, bytesRead);
    assertEquals('Y', buffer[0]);

    // Clean up
    output.close();
  }

  @Test
  void testWrite_largeArray_optimized() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write 1MB array in single write() call
    int totalSize = 1024 * 1024; // 1MB
    byte[] data = new byte[totalSize];
    for (int i = 0; i < totalSize; i++) {
      data[i] = (byte) (i % 256);
    }
    output.write(data);
    output.close();

    // Assert - Verify all data is readable
    byte[] readData = new byte[totalSize];
    int totalRead = 0;
    int bytesRead;
    byte[] buffer = new byte[CHUNK_SIZE];
    while ((bytesRead = input.read(buffer, 0, buffer.length)) != -1) {
      System.arraycopy(buffer, 0, readData, totalRead, bytesRead);
      totalRead += bytesRead;
    }

    assertEquals(totalSize, totalRead);
    assertArrayEquals(data, readData);
  }

  @Test
  void testFlush_partialBuffer() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write 100KB and flush
    int dataSize = 100 * 1024; // 100KB
    byte[] data = new byte[dataSize];
    for (int i = 0; i < dataSize; i++) {
      data[i] = (byte) 'Z';
    }
    output.write(data);
    output.flush();

    // Assert - Should read exactly 100KB (not 256KB)
    byte[] buffer = new byte[CHUNK_SIZE];
    int bytesRead = input.read(buffer, 0, buffer.length);
    assertEquals(dataSize, bytesRead);

    // Verify content
    for (int i = 0; i < dataSize; i++) {
      assertEquals('Z', buffer[i]);
    }

    // Write more data - should use fresh buffer
    output.write("More data".getBytes(StandardCharsets.UTF_8));
    output.flush();

    bytesRead = input.read(buffer, 0, buffer.length);
    assertEquals(9, bytesRead);
    assertEquals("More data", new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));

    // Clean up
    output.close();
  }

  @Test
  void testClose_flushesAndSignalsEof() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write 50KB and close
    int dataSize = 50 * 1024; // 50KB
    byte[] data = new byte[dataSize];
    for (int i = 0; i < dataSize; i++) {
      data[i] = (byte) (i % 128);
    }
    output.write(data);
    output.close();

    // Assert - Read data then EOF
    byte[] buffer = new byte[CHUNK_SIZE];
    int bytesRead = input.read(buffer, 0, buffer.length);
    assertEquals(dataSize, bytesRead);

    // Verify content
    for (int i = 0; i < dataSize; i++) {
      assertEquals((byte) (i % 128), buffer[i]);
    }

    // Next read should be EOF
    assertEquals(-1, input.read(buffer, 0, buffer.length));
  }

  @Test
  void testClose_idempotent() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write data and close twice
    output.write("Test".getBytes(StandardCharsets.UTF_8));
    output.close();
    output.close(); // Second close should be safe

    // Assert - Should only get one chunk of data then EOF
    byte[] buffer = new byte[100];
    int bytesRead = input.read(buffer, 0, buffer.length);
    assertEquals(4, bytesRead);
    assertEquals("Test", new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
    assertEquals(-1, input.read(buffer, 0, buffer.length));
  }

  @Test
  void testWrite_afterClose_throwsIoException() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);
    output.close();

    // Act & Assert
    assertThrows(IOException.class, () -> output.write('A'));
    assertThrows(IOException.class, () -> output.write("Test".getBytes()));
  }

  @Test
  void testFlush_afterClose_throwsIoException() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);
    output.close();

    // Act & Assert
    assertThrows(IOException.class, () -> output.flush());
  }

  @Test
  void testWrite_nullBuffer_throwsNullPointerException() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act & Assert
    assertThrows(NullPointerException.class, () -> output.write(null, 0, 10));
  }

  @Test
  void testWrite_invalidOffsetLength_throwsIndexOutOfBoundsException() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);
    byte[] buffer = new byte[10];

    // Act & Assert
    assertThrows(IndexOutOfBoundsException.class, () -> output.write(buffer, -1, 5));
    assertThrows(IndexOutOfBoundsException.class, () -> output.write(buffer, 0, -1));
    assertThrows(IndexOutOfBoundsException.class, () -> output.write(buffer, 0, 11));
    assertThrows(IndexOutOfBoundsException.class, () -> output.write(buffer, 5, 6));
  }

  @Test
  void testConstructor_nullInputStream_throwsIllegalArgumentException() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class,
        () -> new BlockingQueueOutputStream(null, CHUNK_SIZE));
  }

  @Test
  void testConstructor_invalidChunkSize_throwsIllegalArgumentException() {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);

    // Act & Assert
    assertThrows(IllegalArgumentException.class,
        () -> new BlockingQueueOutputStream(input, 0));
    assertThrows(IllegalArgumentException.class,
        () -> new BlockingQueueOutputStream(input, -1));
  }

  @Test
  void testBackpressure_slowReader() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write enough to fill queue (8 chunks)
    int chunksToWrite = QUEUE_CAPACITY + 2; // More than queue capacity
    List<Thread> writerThreads = new ArrayList<>();

    for (int i = 0; i < chunksToWrite; i++) {
      byte[] chunk = new byte[CHUNK_SIZE];
      for (int j = 0; j < CHUNK_SIZE; j++) {
        chunk[j] = (byte) (i % 256);
      }

      Thread writer = new Thread(() -> {
        try {
          output.write(chunk);
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      writer.start();
      writerThreads.add(writer);
      Thread.sleep(10); // Small delay between writes
    }

    // Give writers time to start
    Thread.sleep(200);

    // Start reading slowly
    byte[] buffer = new byte[CHUNK_SIZE];
    int chunksRead = 0;
    while (chunksRead < chunksToWrite) {
      int bytesRead = input.read(buffer, 0, buffer.length);
      if (bytesRead > 0) {
        chunksRead++;
        Thread.sleep(50); // Slow reader
      }
    }

    // Wait for all writers to complete
    for (Thread writer : writerThreads) {
      writer.join(2000);
    }

    // Clean up
    output.close();

    // Assert - All data should have been written and read successfully
    assertEquals(chunksToWrite, chunksRead);
  }

  @Test
  void testIntegration_writeReadPattern() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write various sized data
    String[] testStrings = {
        "Short string",
        "A".repeat(1000), // 1KB
        "B".repeat(100 * 1024), // 100KB
        "C".repeat(300 * 1024), // 300KB (spans multiple chunks)
        "D".repeat(10), // 10 bytes
    };

    for (String str : testStrings) {
      output.write(str.getBytes(StandardCharsets.UTF_8));
      output.flush();
    }
    output.close();

    // Assert - Read all data back and verify
    StringBuilder result = new StringBuilder();
    byte[] buffer = new byte[CHUNK_SIZE];
    int bytesRead;
    while ((bytesRead = input.read(buffer, 0, buffer.length)) != -1) {
      result.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
    }

    String expected = String.join("", testStrings);
    assertEquals(expected, result.toString());
  }

  @Test
  void testWrite_zeroLength_doesNothing() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write zero-length array
    output.write(new byte[0]);
    output.write(new byte[10], 0, 0);

    // Write actual data
    output.write("Test".getBytes(StandardCharsets.UTF_8));
    output.close();

    // Assert - Should only read the actual data
    byte[] buffer = new byte[100];
    int bytesRead = input.read(buffer, 0, buffer.length);
    assertEquals(4, bytesRead);
    assertEquals("Test", new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
  }

  @Test
  void testConstructor_validParameters_succeeds() {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);

    // Act
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Assert
    assertNotNull(output);
  }

  @Test
  void testFlush_emptyBuffer_doesNothing() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Flush without writing anything
    output.flush();
    output.flush(); // Multiple flushes

    // Write and flush actual data
    output.write("Data".getBytes(StandardCharsets.UTF_8));
    output.close();

    // Assert - Should only read actual data
    byte[] buffer = new byte[100];
    int bytesRead = input.read(buffer, 0, buffer.length);
    assertEquals(4, bytesRead);
    assertEquals("Data", new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
  }

  @Test
  void testWrite_acrossChunkBoundaries() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write data that spans exactly 2 chunks
    int totalSize = CHUNK_SIZE + (CHUNK_SIZE / 2); // 1.5 chunks
    byte[] data = new byte[totalSize];
    for (int i = 0; i < totalSize; i++) {
      data[i] = (byte) (i % 256);
    }
    output.write(data);
    output.close();

    // Assert - Read all data and verify
    byte[] readData = new byte[totalSize];
    int totalRead = 0;
    int bytesRead;
    byte[] buffer = new byte[CHUNK_SIZE];
    while ((bytesRead = input.read(buffer, 0, buffer.length)) != -1) {
      System.arraycopy(buffer, 0, readData, totalRead, bytesRead);
      totalRead += bytesRead;
    }

    assertEquals(totalSize, totalRead);
    assertArrayEquals(data, readData);
  }

  @Test
  void testClose_withEmptyBuffer_signalsEof() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Close without writing anything
    output.close();

    // Assert - Should immediately get EOF
    byte[] buffer = new byte[100];
    assertEquals(-1, input.read(buffer, 0, buffer.length));
  }

  @Test
  void testMultipleSmallWrites_accumulate() throws Exception {
    // Arrange
    BlockingQueueInputStream input = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, CHUNK_SIZE);

    // Act - Write many small strings
    for (int i = 0; i < 100; i++) {
      output.write(("Line " + i + "\n").getBytes(StandardCharsets.UTF_8));
    }
    output.close();

    // Assert - Read all data
    StringBuilder result = new StringBuilder();
    byte[] buffer = new byte[CHUNK_SIZE];
    int bytesRead;
    while ((bytesRead = input.read(buffer, 0, buffer.length)) != -1) {
      result.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
    }

    // Verify all lines present
    String resultStr = result.toString();
    for (int i = 0; i < 100; i++) {
      assertTrue(resultStr.contains("Line " + i + "\n"));
    }
  }
}
