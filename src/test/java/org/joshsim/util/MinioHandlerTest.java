package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for the MinioHandler class, verifying its behavior with mocked dependencies.
 */
public class MinioHandlerTest {

  @Mock
  private MinioClient minioClient;

  @Mock
  private OutputOptions output;

  @Mock
  private MinioOptions options;

  private final String testBucket = "test-bucket";

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Common setup
    when(options.isMinioOutput()).thenReturn(true);
    when(options.getMinioClient()).thenReturn(minioClient);
    when(options.getBucketName()).thenReturn(testBucket);
    when(options.getObjectPath()).thenReturn("");
  }

  @Test
  void whenBucketExists_shouldNotCreateBucket() throws Exception {
    // Setup
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    when(options.isEnsureBucketExists()).thenReturn(false);

    // Execute
    MinioHandler handler = new MinioHandler(options, output);

    // Verify
    verify(minioClient).bucketExists(any(BucketExistsArgs.class));
    verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    assertEquals(testBucket, handler.getBucketName());
  }

  @Test
  void whenBucketDoesNotExistAndEnsureIsTrue_shouldCreateBucket() throws Exception {
    // Setup
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
    when(options.isEnsureBucketExists()).thenReturn(true);

    // Execute
    MinioHandler handler = new MinioHandler(options, output);
    // Use handler to make checkstyle happy
    handler.toString();

    // Verify
    verify(minioClient).bucketExists(any(BucketExistsArgs.class));
    verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    verify(output).printInfo(contains("Created bucket"));
  }

  @Test
  void whenBucketDoesNotExistAndEnsureIsFalse_shouldThrowException() throws Exception {
    // Setup
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
    when(options.isEnsureBucketExists()).thenReturn(false);

    // Execute & Verify
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new MinioHandler(options, output)
    );

    assertTrue(exception.getMessage().contains("does not exist"));
    verify(minioClient).bucketExists(any(BucketExistsArgs.class));
    verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
  }

  @Test
  void whenClientThrowsException_shouldPropagateException() throws Exception {
    // Setup
    when(minioClient.bucketExists(any(BucketExistsArgs.class)))
        .thenThrow(new RuntimeException("Minio connection failed"));

    // Execute & Verify
    Exception exception = assertThrows(
        Exception.class,
        () -> new MinioHandler(options, output)
    );

    assertTrue(exception.getMessage().contains("connection failed"));
  }
}
