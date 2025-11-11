/**
 * Strategy for streaming data directly to MinIO storage.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStreamStrategy implementation for direct streaming to MinIO.
 *
 * <p>This strategy uses BlockingQueueInputStream/BlockingQueueOutputStream to work around
 * minio-java's lack of direct OutputStream support (see minio-java issue #956). This approach
 * replaces the previous PipedInputStream/PipedOutputStream implementation which suffered from
 * race conditions when the upload thread started reading before data was written.
 * </p>
 *
 * <p>The BlockingQueue approach provides proper async coordination: the writer thread enqueues
 * data chunks as they're written, and the upload thread consumes them. This eliminates timing
 * issues and supports retry logic since the queue can be read multiple times if needed.
 * </p>
 *
 * <p>The upload happens asynchronously using Thread-based concurrency to maintain WebAssembly
 * compatibility. The close() method blocks until completion to ensure data is fully written
 * before the stream is closed.</p>
 *
 * <p><b>WebAssembly Note:</b> This class is not used in WebAssembly environments (where MinIO
 * client is not available). It uses simple Thread-based async patterns instead of ExecutorService
 * to avoid TeaVM compilation issues with java.util.concurrent classes.</p>
 */
public class MinioOutputStreamStrategy implements OutputStreamStrategy {

  private static final int[] RETRY_DELAYS_MS = {1000, 2000, 4000, 8000, 16000};
  private static final int CHUNK_SIZE = 256 * 1024; // 256KB chunks
  private static final int QUEUE_CAPACITY = 8; // 8 chunks = 2MB buffer

  private final MinioClient minioClient;
  private final String bucketName;
  private final String objectPath;

  /**
   * Constructs a MinioOutputStreamStrategy.
   *
   * @param minioClient The MinIO client instance to use for uploads
   * @param bucketName The name of the bucket to upload to
   * @param objectPath The path within the bucket where the object will be stored
   */
  public MinioOutputStreamStrategy(MinioClient minioClient, String bucketName,
                                   String objectPath) {
    this.minioClient = minioClient;
    this.bucketName = bucketName;
    this.objectPath = objectPath;
  }

  @Override
  public OutputStream open() throws IOException {
    // Ensure bucket exists before attempting upload
    ensureBucketExists();

    // Create BlockingQueue streams for async upload
    BlockingQueueInputStream inputStream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);

    // Start async upload task on a dedicated daemon thread
    // Using Thread instead of ExecutorService for WebAssembly compatibility
    UploadTask uploadTask = new UploadTask(inputStream);
    Thread uploadThread = new Thread(uploadTask);
    uploadThread.setName("minio-upload-" + System.currentTimeMillis());
    uploadThread.setDaemon(true);
    uploadThread.start();

    // Return a wrapper that blocks on close() to ensure upload completes
    // The input stream uses an ArrayBlockingQueue with 8-chunk capacity (2MB total)
    BlockingQueueOutputStream outputStream = new BlockingQueueOutputStream(inputStream, CHUNK_SIZE);
    return new MinioOutputStream(outputStream, uploadTask, uploadThread);
  }

  /**
   * Ensures the target bucket exists, creating it if necessary.
   *
   * @throws IOException if bucket operations fail
   */
  private void ensureBucketExists() throws IOException {
    try {
      boolean exists = minioClient.bucketExists(
          BucketExistsArgs.builder()
              .bucket(bucketName)
              .build()
      );

      if (!exists) {
        minioClient.makeBucket(
            MakeBucketArgs.builder()
                .bucket(bucketName)
                .build()
        );
      }
    } catch (Exception e) {
      throw new IOException("Failed to ensure bucket exists: " + bucketName, e);
    }
  }

  /**
   * Runnable task that uploads data from a BlockingQueue input stream.
   *
   * <p>This task runs asynchronously on a dedicated thread and signals completion
   * or failure via volatile fields. Using Runnable + Thread instead of ExecutorService
   * and CompletableFuture for WebAssembly compatibility.</p>
   */
  private class UploadTask implements Runnable {
    private final BlockingQueueInputStream inputStream;
    private volatile boolean completed = false;
    private volatile Exception exception = null;

    public UploadTask(BlockingQueueInputStream inputStream) {
      this.inputStream = inputStream;
    }

    public boolean isCompleted() {
      return completed;
    }

    public Exception getException() {
      return exception;
    }

    @Override
    public void run() {
      try {
        uploadWithRetry(inputStream);
        completed = true;
      } catch (Exception e) {
        exception = e;
        completed = true;
      }
    }
  }

  /**
   * Uploads data from the BlockingQueue input stream with exponential backoff retry logic.
   *
   * <p>The BlockingQueue approach supports proper retry logic since the stream can be
   * consumed multiple times if needed. The writer thread enqueues data chunks, and the
   * upload thread reads them asynchronously without timing dependencies.</p>
   *
   * <p>NOTE: The input stream is NOT closed in this method. The BlockingQueueOutputStream
   * manages stream lifecycle by signaling EOF when closed, which allows the upload to
   * complete naturally.</p>
   *
   * @param inputStream The BlockingQueue input stream containing data to upload
   * @throws RuntimeException if all retry attempts fail
   */
  private void uploadWithRetry(BlockingQueueInputStream inputStream) {
    Exception lastException = null;
    for (int attempt = 0; attempt < RETRY_DELAYS_MS.length; attempt++) {
      try {
        // Attempt upload
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectPath)
                .stream(inputStream, -1, 10485760) // -1 = unknown size, 10MB part size
                .build()
        );

        // Success - upload completed
        return;

      } catch (Exception e) {
        lastException = e;

        // If not the last attempt, wait before retrying
        if (attempt < RETRY_DELAYS_MS.length - 1) {
          try {
            Thread.sleep(RETRY_DELAYS_MS[attempt]);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upload interrupted during retry backoff", ie);
          }
        }
      }
    }

    // All retries exhausted - propagate error
    throw new RuntimeException(
        "Failed to upload to MinIO after " + RETRY_DELAYS_MS.length + " attempts: "
            + bucketName + "/" + objectPath,
        lastException
    );
  }

  /**
   * Wrapper OutputStream that delegates to BlockingQueueOutputStream and blocks on close().
   *
   * <p>This ensures the async upload completes before the stream is considered closed,
   * preventing data loss or incomplete uploads.</p>
   *
   * <p><b>BlockingQueue lifecycle:</b></p>
   * <ol>
   *   <li>Writer calls close() → triggers flush() and delegate.close()</li>
   *   <li>delegate.close() flushes remaining data and signals EOF to the input stream</li>
   *   <li>Upload thread detects EOF and finishes upload</li>
   *   <li>Writer thread waits via thread.join() for upload to complete</li>
   *   <li>Upload completes successfully or propagates error</li>
   * </ol>
   *
   * <p><b>Note:</b> Unlike PipedOutputStream, BlockingQueueOutputStream doesn't require
   * special close ordering. The output stream manages EOF signaling automatically.</p>
   *
   * <p><b>WebAssembly Note:</b> Uses Thread.join() instead of CompletableFuture for
   * WebAssembly compatibility.</p>
   */
  private static class MinioOutputStream extends OutputStream {
    private final BlockingQueueOutputStream delegate;
    private final UploadTask uploadTask;
    private final Thread uploadThread;
    private boolean closed = false;

    /**
     * Constructs a MinioOutputStream.
     *
     * @param delegate The underlying BlockingQueueOutputStream to write to
     * @param uploadTask The task performing the async upload
     * @param uploadThread The thread running the upload task
     */
    public MinioOutputStream(BlockingQueueOutputStream delegate,
                             UploadTask uploadTask,
                             Thread uploadThread) {
      this.delegate = delegate;
      this.uploadTask = uploadTask;
      this.uploadThread = uploadThread;
    }

    @Override
    public void write(int b) throws IOException {
      delegate.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      delegate.flush();
    }

    @Override
    public void close() throws IOException {
      if (closed) {
        return;
      }

      try {
        // Flush and close the output stream
        // BlockingQueueOutputStream handles EOF signaling automatically
        delegate.flush();
        delegate.close();

        // Wait for upload thread to complete
        uploadThread.join();

        // Check for upload errors
        if (uploadTask.exception != null) {
          throw new IOException("Upload failed: " + uploadTask.exception.getMessage(),
                                uploadTask.exception);
        }

        closed = true;

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Upload interrupted while waiting for completion", e);
      }
    }
  }
}
