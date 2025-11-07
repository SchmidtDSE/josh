/**
 * Custom InputStream backed by a BlockingQueue for asynchronous streaming.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * InputStream implementation backed by ArrayBlockingQueue for thread-safe asynchronous data flow.
 *
 * <p>This stream is designed for producer-consumer scenarios where a writer thread enqueues
 * chunks of data and a reader thread consumes them. The stream provides blocking semantics:
 * reads block until data is available, and enqueues block when the queue is full (backpressure).
 * </p>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe for single producer, single consumer usage.
 * The ArrayBlockingQueue provides internal synchronization for enqueue/dequeue operations.
 * </p>
 *
 * <p><b>EOF Handling:</b> EOF is signaled using a sentinel value (empty byte array). Once EOF
 * is signaled, all subsequent reads return -1.
 * </p>
 *
 * <p><b>Exception Propagation:</b> The writer can signal exceptions via signalException(),
 * which will cause subsequent reads to throw IOException wrapping the original exception.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * BlockingQueueInputStream input = new BlockingQueueInputStream(256 * 1024, 8);
 *
 * // Writer thread
 * new Thread(() -&gt; {
 *   try {
 *     byte[] data = "Hello World".getBytes();
 *     input.enqueue(data);
 *     input.signalEof();
 *   } catch (InterruptedException e) {
 *     input.signalException(e);
 *   }
 * }).start();
 *
 * // Reader thread
 * int bytesRead;
 * byte[] buffer = new byte[1024];
 * while ((bytesRead = input.read(buffer)) != -1) {
 *   // Process buffer
 * }
 * </pre>
 */
public class BlockingQueueInputStream extends InputStream {

  /** Sentinel value used to signal EOF to the reader. */
  private static final byte[] EOF_MARKER = new byte[0];

  private final ArrayBlockingQueue<byte[]> queue;
  private final int chunkSize;
  private byte[] currentChunk;
  private int currentPosition;
  private boolean eofSignaled;
  private volatile Throwable writerException;

  /**
   * Constructs a BlockingQueueInputStream.
   *
   * @param chunkSize The size of data chunks in bytes (typically 256KB)
   * @param queueCapacity The maximum number of chunks the queue can hold (typically 8)
   */
  public BlockingQueueInputStream(int chunkSize, int queueCapacity) {
    this.chunkSize = chunkSize;
    this.queue = new ArrayBlockingQueue<>(queueCapacity);
    this.currentChunk = null;
    this.currentPosition = 0;
    this.eofSignaled = false;
    this.writerException = null;
  }

  /**
   * Enqueues a chunk of data for the reader to consume.
   *
   * <p>This method should be called by the writer thread. It blocks if the queue is full,
   * providing backpressure to prevent unbounded memory growth.
   * </p>
   *
   * @param chunk The data chunk to enqueue (should not be null or empty unless signaling EOF)
   * @throws InterruptedException if the thread is interrupted while waiting for queue space
   */
  public void enqueue(byte[] chunk) throws InterruptedException {
    if (chunk == null) {
      throw new IllegalArgumentException("Chunk cannot be null (use signalEof() instead)");
    }
    queue.put(chunk);
  }

  /**
   * Signals end-of-file to the reader.
   *
   * <p>This method should be called by the writer thread after all data has been enqueued.
   * Once called, subsequent reads will return -1 after all queued data is consumed.
   * This method is idempotent - calling it multiple times has no additional effect.
   * </p>
   *
   * <p>Note: The eofSignaled flag is set when the EOF_MARKER is consumed by the reader,
   * not when this method is called. This ensures accurate tracking of whether all data
   * has been read.</p>
   */
  public void signalEof() {
    if (!eofSignaled) {
      try {
        queue.put(EOF_MARKER);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // Set flag on interruption to prevent retries
        eofSignaled = true;
        System.err.println("Warning: Interrupted while signaling EOF");
      }
    }
  }

  /**
   * Signals an exception from the writer thread to the reader.
   *
   * <p>This causes subsequent read operations to throw IOException wrapping the provided
   * exception. This method is useful for propagating errors from the writer thread to
   * the reader thread.
   * </p>
   *
   * @param exception The exception that occurred in the writer thread
   */
  public void signalException(Throwable exception) {
    this.writerException = exception;
  }

  @Override
  public int read() throws IOException {
    byte[] buffer = new byte[1];
    int bytesRead = read(buffer, 0, 1);
    if (bytesRead == -1) {
      return -1;
    }
    return buffer[0] & 0xFF; // Return unsigned byte value
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (b == null) {
      throw new NullPointerException("Buffer cannot be null");
    }
    if (off < 0 || len < 0 || len > b.length - off) {
      throw new IndexOutOfBoundsException(
          "Invalid offset/length: off=" + off + " len=" + len + " buffer.length=" + b.length
      );
    }
    if (len == 0) {
      return 0;
    }

    int totalBytesRead = 0;
    int chunksProcessed = 0;
    int bytesFromFirstChunk = 0;

    // Read from multiple chunks to fill buffer as much as possible without blocking
    // Strategy: Always read at least one complete chunk. Read additional chunks only if:
    // 1. The first chunk was very small (< 8 bytes), OR
    // 2. We explicitly need more data and it's immediately available
    while (totalBytesRead < len) {
      // Load next chunk if current chunk is exhausted
      if (currentChunk == null || currentPosition >= currentChunk.length) {
        // If EOF was already signaled and we have no more data, return immediately
        if (eofSignaled && (currentChunk == null || currentPosition >= currentChunk.length)) {
          return totalBytesRead > 0 ? totalBytesRead : -1;
        }

        // Decide whether to load next chunk
        if (chunksProcessed == 0) {
          // Always load first chunk (blocking)
          loadNextChunk();
        } else if (chunksProcessed == 1 && bytesFromFirstChunk < 8) {
          // Only load second chunk if first chunk was very small (< 8 bytes)
          // This matches standard InputStream behavior: return as soon as
          // reasonable data is available
          loadNextChunkNonBlocking();
        } else {
          // We've read enough chunks, return what we have
          return totalBytesRead;
        }

        // Check if we hit EOF while loading
        if (currentChunk == null) {
          return totalBytesRead > 0 ? totalBytesRead : -1;
        }
      }

      // Copy from current chunk
      int remaining = currentChunk.length - currentPosition;
      int bytesToCopy = Math.min(remaining, len - totalBytesRead);
      System.arraycopy(currentChunk, currentPosition, b, off + totalBytesRead, bytesToCopy);
      currentPosition += bytesToCopy;
      totalBytesRead += bytesToCopy;

      // Track bytes read from first chunk
      if (chunksProcessed == 0) {
        bytesFromFirstChunk += bytesToCopy;
      }

      // If we've exhausted this chunk, mark it as processed
      if (currentPosition >= currentChunk.length) {
        chunksProcessed++;
      }
    }

    return totalBytesRead;
  }

  @Override
  public void close() throws IOException {
    // No-op: The stream is closed when EOF is signaled by the writer
    // Closing here would interfere with the writer's EOF signaling
  }

  /**
   * Loads the next chunk from the queue, blocking until available.
   *
   * <p>This method checks for writer exceptions before blocking. If the writer has signaled
   * an exception, it throws IOException immediately. Otherwise, it blocks on queue.take()
   * until data is available or EOF is signaled.
   * </p>
   *
   * @throws IOException if the writer signaled an exception or if interrupted while waiting
   */
  private void loadNextChunk() throws IOException {
    // Check for writer exceptions first
    if (writerException != null) {
      throw new IOException("Writer thread encountered error", writerException);
    }

    try {
      currentChunk = queue.take(); // Blocks until available

      if (currentChunk == EOF_MARKER) {
        currentChunk = null;
        eofSignaled = true;
        return;
      }

      currentPosition = 0;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while reading from queue", e);
    }
  }

  /**
   * Loads the next chunk from the queue without blocking.
   *
   * <p>This method uses queue.poll() instead of queue.take(), so it returns immediately
   * if no data is available. This is used when reading across chunk boundaries to avoid
   * blocking unnecessarily when some data has already been read.
   * </p>
   *
   * <p>If no chunk is available, currentChunk remains unchanged (null if exhausted).
   * The caller should check if currentChunk is null after calling this method.
   * </p>
   *
   * @throws IOException if the writer signaled an exception
   */
  private void loadNextChunkNonBlocking() throws IOException {
    // Check for writer exceptions first
    if (writerException != null) {
      throw new IOException("Writer thread encountered error", writerException);
    }

    // Non-blocking poll - returns null if queue is empty
    byte[] chunk = queue.poll();

    if (chunk == null) {
      // No data available - leave currentChunk as-is (null if previously exhausted)
      currentChunk = null;
      return;
    }

    if (chunk == EOF_MARKER) {
      currentChunk = null;
      eofSignaled = true;
      return;
    }

    currentChunk = chunk;
    currentPosition = 0;
  }
}
