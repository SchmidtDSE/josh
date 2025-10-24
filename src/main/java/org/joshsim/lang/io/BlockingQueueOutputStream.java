/**
 * Custom OutputStream that buffers writes and enqueues chunks to BlockingQueueInputStream.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * OutputStream implementation that buffers writes into chunks and enqueues them to a paired
 * BlockingQueueInputStream.
 *
 * <p>This stream is designed for producer-consumer scenarios where writes should be buffered
 * into efficient-sized chunks before being passed to a reader thread. The stream automatically
 * flushes chunks when the buffer fills, and can be explicitly flushed or closed to send
 * partial buffers.
 * </p>
 *
 * <p><b>Buffering Strategy:</b> Writes accumulate in an internal buffer until the chunk size
 * is reached, at which point the chunk is enqueued. For large writes exceeding the chunk size,
 * data is enqueued in chunk-sized pieces directly without intermediate buffering.
 * </p>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe when paired with BlockingQueueInputStream,
 * as the underlying queue provides synchronization.
 * </p>
 *
 * <p><b>Close Behavior:</b> Calling close() flushes any remaining buffered data and signals
 * EOF to the paired input stream. The close() method is idempotent.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * BlockingQueueInputStream input = new BlockingQueueInputStream(256 * 1024, 8);
 * BlockingQueueOutputStream output = new BlockingQueueOutputStream(input, 256 * 1024);
 *
 * // Write data
 * output.write("Hello World".getBytes());
 * output.flush(); // Sends partial buffer if desired
 * output.close(); // Sends remaining data and signals EOF
 *
 * // Read from paired input stream in another thread
 * </pre>
 */
public class BlockingQueueOutputStream extends OutputStream {

  private final BlockingQueueInputStream pairedInputStream;
  private final int chunkSize;
  private byte[] currentBuffer;
  private int bufferPosition;
  private boolean closed;

  /**
   * Constructs a BlockingQueueOutputStream.
   *
   * @param pairedInputStream The BlockingQueueInputStream to write chunks to
   * @param chunkSize The size of each chunk in bytes (typically 256KB)
   */
  public BlockingQueueOutputStream(BlockingQueueInputStream pairedInputStream, int chunkSize) {
    if (pairedInputStream == null) {
      throw new IllegalArgumentException("Paired input stream cannot be null");
    }
    if (chunkSize <= 0) {
      throw new IllegalArgumentException("Chunk size must be positive");
    }
    this.pairedInputStream = pairedInputStream;
    this.chunkSize = chunkSize;
    this.currentBuffer = new byte[chunkSize];
    this.bufferPosition = 0;
    this.closed = false;
  }

  @Override
  public void write(int b) throws IOException {
    ensureOpen();
    currentBuffer[bufferPosition++] = (byte) b;
    if (bufferPosition >= chunkSize) {
      flushCurrentBuffer();
    }
  }

  @Override
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    ensureOpen();

    if (b == null) {
      throw new NullPointerException("Buffer cannot be null");
    }
    if (off < 0 || len < 0 || len > b.length - off) {
      throw new IndexOutOfBoundsException(
          "Invalid offset/length: off=" + off + " len=" + len + " buffer.length=" + b.length
      );
    }

    int remaining = len;
    int currentOff = off;

    while (remaining > 0) {
      int spaceInBuffer = chunkSize - bufferPosition;

      if (remaining >= chunkSize && bufferPosition == 0) {
        // Optimization: write full chunks directly without buffering
        flushFullChunk(b, currentOff);
        currentOff += chunkSize;
        remaining -= chunkSize;
      } else if (remaining >= spaceInBuffer) {
        // Fill current buffer and flush
        System.arraycopy(b, currentOff, currentBuffer, bufferPosition, spaceInBuffer);
        bufferPosition += spaceInBuffer;
        flushCurrentBuffer();
        currentOff += spaceInBuffer;
        remaining -= spaceInBuffer;
      } else {
        // Partial write to buffer
        System.arraycopy(b, currentOff, currentBuffer, bufferPosition, remaining);
        bufferPosition += remaining;
        remaining = 0;
      }
    }
  }

  @Override
  public void flush() throws IOException {
    ensureOpen();
    if (bufferPosition > 0) {
      flushCurrentBuffer();
    }
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }

    try {
      // Flush any remaining data
      if (bufferPosition > 0) {
        flushCurrentBuffer();
      }

      // Signal EOF to paired input stream
      pairedInputStream.signalEof();
    } finally {
      closed = true;
    }
  }

  /**
   * Flushes the current buffer to the queue.
   *
   * <p>Creates a chunk of exact size (not the full buffer if only partially filled) and
   * enqueues it to the paired input stream. Resets the buffer position for the next write.
   * </p>
   *
   * @throws IOException if interrupted while enqueuing or if the stream is closed
   */
  private void flushCurrentBuffer() throws IOException {
    if (bufferPosition > 0) {
      // Create chunk of exact size (don't send full buffer if only partial data)
      byte[] chunk = Arrays.copyOf(currentBuffer, bufferPosition);
      try {
        pairedInputStream.enqueue(chunk);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while writing to queue", e);
      }
      bufferPosition = 0;
    }
  }

  /**
   * Enqueues a full chunk directly from the source array without buffering.
   *
   * <p>This optimization is used when writing large arrays that are already chunk-sized
   * or larger, avoiding unnecessary copying to the internal buffer.
   * </p>
   *
   * @param b The source array
   * @param off The offset in the source array
   * @throws IOException if interrupted while enqueuing
   */
  private void flushFullChunk(byte[] b, int off) throws IOException {
    byte[] chunk = Arrays.copyOfRange(b, off, off + chunkSize);
    try {
      pairedInputStream.enqueue(chunk);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while writing to queue", e);
    }
  }

  /**
   * Ensures the stream is still open.
   *
   * @throws IOException if the stream has been closed
   */
  private void ensureOpen() throws IOException {
    if (closed) {
      throw new IOException("Stream closed");
    }
  }
}
