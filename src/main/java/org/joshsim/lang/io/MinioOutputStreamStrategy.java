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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * <p>The upload happens asynchronously in a dedicated thread pool to prevent thread exhaustion
 * when MinIO's internal async operations compete for ForkJoinPool threads. The close() method
 * blocks until completion to ensure data is fully written before the stream is closed.</p>
 */
public class MinioOutputStreamStrategy implements OutputStreamStrategy {

  private static final int[] RETRY_DELAYS_MS = {1000, 2000, 4000, 8000, 16000};
  private static final int CHUNK_SIZE = 256 * 1024; // 256KB chunks
  private static final int QUEUE_CAPACITY = 8; // 8 chunks = 2MB buffer

  /**
   * Dedicated thread pool for MinIO uploads.
   *
   * <p>Using a separate ExecutorService prevents thread starvation issues when MinIO's
   * internal async operations (which also use ForkJoinPool) compete with upload threads.
   * A cached thread pool grows as needed to handle concurrent uploads without blocking.</p>
   */
  private static final ExecutorService UPLOAD_EXECUTOR =
      Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("minio-upload-" + thread.threadId());
        thread.setDaemon(true); // Allow JVM to exit even if uploads are pending
        return thread;
      });

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
    // The input stream uses an ArrayBlockingQueue with 8-chunk capacity (2MB total)
    BlockingQueueInputStream inputStream = new BlockingQueueInputStream(CHUNK_SIZE, QUEUE_CAPACITY);
    BlockingQueueOutputStream outputStream = new BlockingQueueOutputStream(inputStream, CHUNK_SIZE);

    // Start async upload task on dedicated thread pool
    // This prevents thread starvation when MinIO's internal async operations
    // compete for ForkJoinPool.commonPool threads
    CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
      uploadWithRetry(inputStream);
    }, UPLOAD_EXECUTOR);

    // Return a wrapper that blocks on close() to ensure upload completes
    return new MinioOutputStream(outputStream, uploadFuture);
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
   *   <li>Writer thread waits via uploadFuture.get() for upload to complete</li>
   *   <li>Upload completes successfully or propagates error</li>
   * </ol>
   *
   * <p><b>Note:</b> Unlike PipedOutputStream, BlockingQueueOutputStream doesn't require
   * special close ordering. The output stream manages EOF signaling automatically.</p>
   */
  private static class MinioOutputStream extends OutputStream {
    private final BlockingQueueOutputStream delegate;
    private final CompletableFuture<Void> uploadFuture;
    private boolean closed = false;

    /**
     * Constructs a MinioOutputStream.
     *
     * @param delegate The underlying BlockingQueueOutputStream to write to
     * @param uploadFuture The future representing the async upload task
     */
    public MinioOutputStream(BlockingQueueOutputStream delegate,
                             CompletableFuture<Void> uploadFuture) {
      this.delegate = delegate;
      this.uploadFuture = uploadFuture;
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

        // Wait for upload to complete
        uploadFuture.get();
        closed = true;

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Upload interrupted while waiting for completion", e);

      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        throw new IOException("Upload failed: " + cause.getMessage(), cause);
      }
    }
  }
}
