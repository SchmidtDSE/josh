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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * OutputStreamStrategy implementation for direct streaming to MinIO.
 *
 * <p>This strategy uses PipedInputStream/PipedOutputStream to work around minio-java's lack
 * of direct OutputStream support (see minio-java issue #956). It implements exponential backoff
 * retry logic to handle transient network failures, and auto-creates buckets if they don't exist.
 * </p>
 *
 * <p>The upload happens asynchronously in a background thread, but the close() method blocks
 * until completion to ensure data is fully written before the stream is closed.</p>
 */
public class MinioOutputStreamStrategy implements OutputStreamStrategy {

  private static final int[] RETRY_DELAYS_MS = {1000, 2000, 4000, 8000, 16000};
  private static final int PIPE_BUFFER_SIZE = 1024 * 1024; // 1MB buffer

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

    // Create piped streams for async upload
    PipedInputStream pipedInput = new PipedInputStream(PIPE_BUFFER_SIZE);
    PipedOutputStream pipedOutput = new PipedOutputStream(pipedInput);

    // Start async upload task
    CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
      uploadWithRetry(pipedInput);
    });

    // Return a wrapper that blocks on close()
    // Pass input stream so it can be closed AFTER output stream (prevents "Pipe closed" errors)
    return new MinioOutputStream(pipedOutput, uploadFuture, pipedInput);
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
   * Uploads data from the piped input stream with exponential backoff retry logic.
   *
   * <p>IMPORTANT: Retry logic has limitations with piped streams. Once the stream has been
   * partially read, it cannot be rewound for retry attempts. This means retries will only
   * work for errors that occur before any data is read (e.g., connection failures). Errors
   * during data transfer will fail immediately without successful retry.</p>
   *
   * <p>NOTE: The input stream is NOT closed in this method. It must be closed by the caller
   * (MinioOutputStream.close()) AFTER the output stream is closed to prevent "Pipe closed"
   * errors. Java pipes require closing the output side first, then the input side.</p>
   *
   * @param inputStream The input stream containing data to upload
   * @throws RuntimeException if all retry attempts fail
   */
  private void uploadWithRetry(PipedInputStream inputStream) {
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
   * Wrapper OutputStream that delegates to PipedOutputStream and blocks on close().
   *
   * <p>This ensures the async upload completes before the stream is considered closed,
   * preventing data loss or incomplete uploads.</p>
   *
   * <p><b>Pipe lifecycle and close ordering:</b></p>
   * <ol>
   *   <li>Writer calls close() → triggers flush() and delegate.close()</li>
   *   <li>delegate.close() signals EOF to the PipedInputStream reader</li>
   *   <li>Reader thread detects EOF and finishes upload</li>
   *   <li>Writer thread waits via uploadFuture.get() for reader to complete</li>
   *   <li>Writer closes input stream AFTER output stream (prevents "Pipe closed" errors)</li>
   *   <li>Upload completes successfully or propagates error</li>
   * </ol>
   *
   * <p><b>IMPORTANT:</b> Java pipes require closing the output stream (PipedOutputStream)
   * before closing the input stream (PipedInputStream). Closing them in the wrong order
   * causes "Pipe closed" errors. This class ensures proper ordering by holding a reference
   * to the input stream and closing it last.</p>
   */
  private static class MinioOutputStream extends OutputStream {
    private final PipedOutputStream delegate;
    private final CompletableFuture<Void> uploadFuture;
    private final PipedInputStream inputStream;
    private boolean closed = false;

    /**
     * Constructs a MinioOutputStream.
     *
     * @param delegate The underlying PipedOutputStream to write to
     * @param uploadFuture The future representing the async upload task
     * @param inputStream The PipedInputStream connected to delegate (closed last to prevent errors)
     */
    public MinioOutputStream(PipedOutputStream delegate, CompletableFuture<Void> uploadFuture,
                             PipedInputStream inputStream) {
      this.delegate = delegate;
      this.uploadFuture = uploadFuture;
      this.inputStream = inputStream;
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
        // Flush any buffered data to ensure all writes are in the pipe
        delegate.flush();

        // Close the output stream to signal EOF to the reader
        // This must happen BEFORE waiting so reader can detect end of stream
        delegate.close();

        // Wait for upload to complete
        // The upload thread will see EOF and finish reading
        uploadFuture.get();

        closed = true;

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Upload interrupted while waiting for completion", e);

      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        throw new IOException("Upload failed: " + cause.getMessage(), cause);

      } finally {
        // ALWAYS close input stream in finally block to ensure cleanup on success AND failure
        // This must be in finally to handle cases where uploadFuture.get() throws an exception
        // Java pipes must close output side first, then input side
        // Since delegate.close() happens before this finally block, order is correct
        try {
          inputStream.close();
        } catch (IOException e) {
          // Input stream may already be closed, which is acceptable
          // Suppress this exception to avoid masking the real error from uploadFuture.get()
        }
      }
    }
  }
}
