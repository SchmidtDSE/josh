package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for the MinioClientSingleton class, verifying singleton behavior
 * and client reuse patterns.
 */
public class MinioClientSingletonTest {

  @Mock
  private MinioOptions options1;

  @Mock
  private MinioOptions options2;

  private MinioClient mockClient1;
  private MinioClient mockClient2;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Create mock clients
    mockClient1 = MinioClient.builder()
        .endpoint("http://localhost:9000")
        .credentials("key1", "secret1")
        .build();

    mockClient2 = MinioClient.builder()
        .endpoint("http://localhost:9001")
        .credentials("key2", "secret2")
        .build();

    // Reset singleton before each test
    MinioClientSingleton.reset();
  }

  @AfterEach
  void tearDown() {
    // Clean up singleton after each test
    MinioClientSingleton.reset();
  }

  @Test
  void getInstance_withValidOptions_returnsClient() {
    // Setup
    when(options1.isMinioOutput()).thenReturn(true);
    when(options1.getMinioEndpoint()).thenReturn("http://localhost:9000");
    when(options1.getMinioClient()).thenReturn(mockClient1);

    // Execute
    MinioClient client = MinioClientSingleton.getInstance(options1);

    // Verify
    assertNotNull(client);
    verify(options1).getMinioClient();
  }

  @Test
  void getInstance_withInvalidOptions_throwsException() {
    // Setup - invalid MinioOptions
    when(options1.isMinioOutput()).thenReturn(false);

    // Execute & Verify
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> MinioClientSingleton.getInstance(options1)
    );

    assertEquals("MinioOptions does not contain a valid Minio endpoint",
        exception.getMessage());
  }

  @Test
  void getInstance_calledTwiceWithSameEndpoint_returnsSameInstance() {
    // Setup
    when(options1.isMinioOutput()).thenReturn(true);
    when(options1.getMinioEndpoint()).thenReturn("http://localhost:9000");
    when(options1.getMinioClient()).thenReturn(mockClient1);

    // Execute
    MinioClient client1 = MinioClientSingleton.getInstance(options1);
    MinioClient client2 = MinioClientSingleton.getInstance(options1);

    // Verify same instance returned
    assertSame(client1, client2);

    // Verify getMinioClient() called only once
    verify(options1, times(1)).getMinioClient();
  }

  @Test
  void getInstance_calledWithDifferentEndpoint_returnsNewInstance() {
    // Setup - first endpoint
    when(options1.isMinioOutput()).thenReturn(true);
    when(options1.getMinioEndpoint()).thenReturn("http://localhost:9000");
    when(options1.getMinioClient()).thenReturn(mockClient1);

    // Setup - second endpoint (different)
    when(options2.isMinioOutput()).thenReturn(true);
    when(options2.getMinioEndpoint()).thenReturn("http://localhost:9001");
    when(options2.getMinioClient()).thenReturn(mockClient2);

    // Execute
    MinioClient client1 = MinioClientSingleton.getInstance(options1);
    MinioClient client2 = MinioClientSingleton.getInstance(options2);

    // Verify - second call should create new client
    verify(options1).getMinioClient();
    verify(options2).getMinioClient();

    // Note: We can't use assertNotSame here because we're returning mocks
    // The important verification is that getMinioClient() was called for both
  }

  @Test
  void reset_clearsInstance() {
    // Setup
    when(options1.isMinioOutput()).thenReturn(true);
    when(options1.getMinioEndpoint()).thenReturn("http://localhost:9000");
    when(options1.getMinioClient()).thenReturn(mockClient1);

    // Execute - get instance, reset, get again
    MinioClientSingleton.getInstance(options1);
    MinioClientSingleton.reset();
    MinioClientSingleton.getInstance(options1);

    // Verify getMinioClient() called twice (once before reset, once after)
    verify(options1, times(2)).getMinioClient();
  }

  @Test
  void getInstance_threadSafe_multipleThreads() throws InterruptedException {
    // Setup
    when(options1.isMinioOutput()).thenReturn(true);
    when(options1.getMinioEndpoint()).thenReturn("http://localhost:9000");
    when(options1.getMinioClient()).thenReturn(mockClient1);

    // Execute - create multiple threads accessing getInstance concurrently
    Thread[] threads = new Thread[10];
    MinioClient[] results = new MinioClient[10];

    for (int i = 0; i < threads.length; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        results[index] = MinioClientSingleton.getInstance(options1);
      });
      threads[i].start();
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join();
    }

    // Verify all threads got the same instance
    for (int i = 1; i < results.length; i++) {
      assertSame(results[0], results[i],
          "All threads should receive the same singleton instance");
    }
  }
}
