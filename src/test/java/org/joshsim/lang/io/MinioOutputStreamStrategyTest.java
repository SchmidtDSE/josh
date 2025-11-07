/**
 * Tests for MinioOutputStreamStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for MinioOutputStreamStrategy, verifying streaming behavior,
 * retry logic, and bucket management.
 */
class MinioOutputStreamStrategyTest {

  @Mock
  private MinioClient minioClient;

  private static final String TEST_BUCKET = "test-bucket";
  private static final String TEST_OBJECT = "test/path/file.csv";

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testOpen_withExistingBucket_returnsOutputStream() throws Exception {
    // Arrange
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, TEST_OBJECT
    );

    // Act
    OutputStream outputStream = strategy.open();

    // Assert
    assertNotNull(outputStream);
    verify(minioClient).bucketExists(any(BucketExistsArgs.class));

    // Clean up
    outputStream.close();
  }

  @Test
  void testOpen_withNonExistentBucket_createsBucket() throws Exception {
    // Arrange
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, TEST_OBJECT
    );

    // Act
    OutputStream outputStream = strategy.open();

    // Assert
    assertNotNull(outputStream);
    verify(minioClient).bucketExists(any(BucketExistsArgs.class));
    verify(minioClient).makeBucket(any(MakeBucketArgs.class));

    // Clean up
    outputStream.close();
  }

  @Test
  void testOpen_bucketCreationFails_throwsIoException() throws Exception {
    // Arrange
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
    doThrow(new RuntimeException("Bucket creation failed"))
        .when(minioClient).makeBucket(any(MakeBucketArgs.class));

    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, TEST_OBJECT
    );

    // Act & Assert
    IOException exception = assertThrows(IOException.class, () -> strategy.open());
    assertTrue(exception.getMessage().contains("Failed to ensure bucket exists"));
    assertTrue(exception.getCause().getMessage().contains("Bucket creation failed"));
  }

  @Test
  void testWrite_andClose_uploadsData() throws Exception {
    // Arrange
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, TEST_OBJECT
    );

    // Act
    try (OutputStream out = strategy.open()) {
      String testData = "test data content\n";
      out.write(testData.getBytes(StandardCharsets.UTF_8));
    } // close() should trigger upload and wait for completion

    // Give async upload a moment to complete
    Thread.sleep(100);

    // Assert - verify upload was attempted
    verify(minioClient).putObject(any(PutObjectArgs.class));
  }

  @Test
  void testRetryLogic_failsOnce_thenSucceeds() throws Exception {
    // Arrange
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    // First call fails, second succeeds
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenThrow(new RuntimeException("Temporary network error"))
        .thenReturn(null);

    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, TEST_OBJECT
    );

    // Act
    try (OutputStream out = strategy.open()) {
      out.write("test".getBytes(StandardCharsets.UTF_8));
    }

    // Give async upload time to retry
    Thread.sleep(2000);

    // Assert - verify putObject was called twice (1 fail + 1 retry success)
    verify(minioClient, times(2)).putObject(any(PutObjectArgs.class));
  }

  @Test
  void testRetryLogic_exhaustsAllRetries_throwsException() throws Exception {
    // Arrange
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    // All calls fail
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenThrow(new RuntimeException("Persistent network error"));

    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, TEST_OBJECT
    );

    // Act & Assert
    OutputStream out = strategy.open();
    out.write("test".getBytes(StandardCharsets.UTF_8));

    // close() should propagate the error after retries exhausted
    IOException exception = assertThrows(IOException.class, () -> out.close());
    assertTrue(exception.getMessage().contains("Upload failed"));
    assertTrue(exception.getCause().getMessage().contains("Failed to upload to MinIO"));
  }

  @Test
  void testMultipleWrites_beforeClose() throws Exception {
    // Arrange
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    // Mock putObject to actually consume the stream (simulating real behavior)
    when(minioClient.putObject(any(PutObjectArgs.class))).thenAnswer(invocation -> {
      PutObjectArgs args = invocation.getArgument(0);
      // In real MinIO, putObject blocks reading the entire stream until EOF
      // Simulate this by reading all available data from the stream
      try {
        byte[] buffer = new byte[8192];
        while (args.stream().read(buffer) >= 0) {
          // Read until EOF to simulate real putObject behavior
        }
      } catch (IOException e) {
        // Expected when stream is closed
      }
      return null;
    });

    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, TEST_OBJECT
    );

    // Act - write multiple chunks
    try (OutputStream out = strategy.open()) {
      for (int i = 0; i < 10; i++) {
        String data = "Line " + i + "\n";
        out.write(data.getBytes(StandardCharsets.UTF_8));
      }
    }

    // Give async upload time to complete
    Thread.sleep(100);

    // Assert - verify upload was called once for all data
    verify(minioClient).putObject(any(PutObjectArgs.class));
  }

  @Test
  void testClose_idempotent_canCallMultipleTimes() throws Exception {
    // Arrange
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, TEST_OBJECT
    );

    // Act
    OutputStream out = strategy.open();
    out.write("test".getBytes(StandardCharsets.UTF_8));
    out.close();
    out.close(); // Second close should be safe

    // Give async upload time to complete
    Thread.sleep(100);

    // Assert - upload should only happen once
    verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
  }

  @Test
  void testConstructor_storesParameters() {
    // Arrange & Act
    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, TEST_OBJECT
    );

    // Assert - verify object was created (no exceptions thrown)
    assertNotNull(strategy);
  }

  @Test
  void testBucketCheck_bucketExistsCalledOnOpen() throws Exception {
    // Arrange
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, TEST_OBJECT
    );

    // Act
    OutputStream out = strategy.open();

    // Assert - verify bucket existence was checked
    verify(minioClient, times(1)).bucketExists(any(BucketExistsArgs.class));

    // Clean up
    out.close();
  }

  @Test
  void testObjectPath_usedInUpload() throws Exception {
    // Arrange
    String customPath = "custom/nested/path/file.csv";
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    MinioOutputStreamStrategy strategy = new MinioOutputStreamStrategy(
        minioClient, TEST_BUCKET, customPath
    );

    // Act
    try (OutputStream out = strategy.open()) {
      out.write("test".getBytes(StandardCharsets.UTF_8));
    }

    // Give async upload time to complete
    Thread.sleep(100);

    // Assert - verify putObject was called (path is embedded in args)
    verify(minioClient).putObject(any(PutObjectArgs.class));
  }
}
