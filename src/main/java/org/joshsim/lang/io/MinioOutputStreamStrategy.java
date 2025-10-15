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
    return new MinioOutputStream(pipedOutput, uploadFuture);
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
   */
  private static class MinioOutputStream extends OutputStream {
    private final PipedOutputStream delegate;
    private final CompletableFuture<Void> uploadFuture;
    private boolean closed = false;

    /**
     * Constructs a MinioOutputStream.
     *
     * @param delegate The underlying PipedOutputStream to write to
     * @param uploadFuture The future representing the async upload task
     */
    public MinioOutputStream(PipedOutputStream delegate, CompletableFuture<Void> uploadFuture) {
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
        // Close the output stream to signal end of data
        delegate.close();

        // Block until upload completes
        uploadFuture.get();

        closed = true;

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Upload interrupted while waiting for completion", e);

      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw new IOException("Upload failed", cause);
        } else {
          throw new IOException("Upload failed with unexpected error", cause);
        }
      }
    }
  }
}
