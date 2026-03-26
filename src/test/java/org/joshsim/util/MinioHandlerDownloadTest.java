package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for MinioHandler download methods.
 */
public class MinioHandlerDownloadTest {

  @Mock
  private MinioClient minioClient;

  @Mock
  private OutputOptions output;

  @Mock
  private MinioOptions options;

  @TempDir
  File tempDir;

  private final String testBucket = "test-bucket";

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    when(options.isMinioOutput()).thenReturn(true);
    when(options.getMinioClient()).thenReturn(minioClient);
    when(options.getBucketName()).thenReturn(testBucket);
    when(options.getObjectPath()).thenReturn("");
    when(options.isEnsureBucketExists()).thenReturn(false);
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
  }

  @Test
  void downloadFile_shouldWriteContentToDestination() throws Exception {
    // Setup
    String objectPath = "job-123/input/simulation.josh";
    String content = "start simulation Main\nend simulation\n";
    File destination = new File(tempDir, "simulation.josh");

    GetObjectResponse response = createGetObjectResponse(content);
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

    MinioHandler handler = new MinioHandler(options, output);

    // Execute
    boolean result = handler.downloadFile(objectPath, destination);

    // Verify
    assertTrue(result);
    assertTrue(destination.exists());
    assertEquals(content, Files.readString(destination.toPath()));
    verify(output).printInfo(contains("Downloaded"));
  }

  @Test
  void downloadFile_whenMinioThrows_shouldReturnFalse() throws Exception {
    // Setup
    String objectPath = "nonexistent/file.josh";
    File destination = new File(tempDir, "file.josh");

    when(minioClient.getObject(any(GetObjectArgs.class)))
        .thenThrow(new RuntimeException("Object not found"));

    MinioHandler handler = new MinioHandler(options, output);

    // Execute
    boolean result = handler.downloadFile(objectPath, destination);

    // Verify
    assertFalse(result);
    assertFalse(destination.exists());
    verify(output).printError(contains("Failed to download"));
  }

  @Test
  void downloadFile_shouldCreateParentDirectories() throws Exception {
    // Setup
    String objectPath = "job-123/input/data/temp.jshd";
    String content = "binary data";
    File destination = new File(tempDir, "nested/subdir/temp.jshd");

    GetObjectResponse response = createGetObjectResponse(content);
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

    MinioHandler handler = new MinioHandler(options, output);

    // Execute
    boolean result = handler.downloadFile(objectPath, destination);

    // Verify
    assertTrue(result);
    assertTrue(destination.exists());
    assertTrue(destination.getParentFile().exists());
  }

  @Test
  void downloadDirectory_shouldDownloadAllObjectsUnderPrefix() throws Exception {
    // Setup
    String prefix = "job-123/input/";

    Item item1 = createMockItem("job-123/input/simulation.josh");
    Item item2 = createMockItem("job-123/input/data/temp.jshd");

    @SuppressWarnings("unchecked")
    Result<Item> result1 = (Result<Item>) org.mockito.Mockito.mock(Result.class);
    when(result1.get()).thenReturn(item1);

    @SuppressWarnings("unchecked")
    Result<Item> result2 = (Result<Item>) org.mockito.Mockito.mock(Result.class);
    when(result2.get()).thenReturn(item2);

    when(minioClient.listObjects(any(ListObjectsArgs.class)))
        .thenReturn(Arrays.asList(result1, result2));

    // Return fresh response for each getObject call
    when(minioClient.getObject(any(GetObjectArgs.class)))
        .thenAnswer(inv -> createGetObjectResponse("test content"));

    MinioHandler handler = new MinioHandler(options, output);

    // Execute
    int count = handler.downloadDirectory(prefix, tempDir);

    // Verify
    assertEquals(2, count);
  }

  @Test
  void downloadDirectory_shouldSkipDirectoryMarkers() throws Exception {
    // Setup
    String prefix = "job-123/input/";

    // Directory markers end with / — the downloadDirectory method checks this
    Item dirItem = createMockItem("job-123/input/data/");
    Item fileItem = createMockItem("job-123/input/simulation.josh");

    @SuppressWarnings("unchecked")
    Result<Item> result1 = (Result<Item>) org.mockito.Mockito.mock(Result.class);
    when(result1.get()).thenReturn(dirItem);

    @SuppressWarnings("unchecked")
    Result<Item> result2 = (Result<Item>) org.mockito.Mockito.mock(Result.class);
    when(result2.get()).thenReturn(fileItem);

    when(minioClient.listObjects(any(ListObjectsArgs.class)))
        .thenReturn(Arrays.asList(result1, result2));

    when(minioClient.getObject(any(GetObjectArgs.class)))
        .thenAnswer(inv -> createGetObjectResponse("test content"));

    MinioHandler handler = new MinioHandler(options, output);

    // Execute
    int count = handler.downloadDirectory(prefix, tempDir);

    // Verify - only the file, not the directory marker
    assertEquals(1, count);
  }

  @Test
  void downloadDirectory_withEmptyPrefix_shouldReturnZero() throws Exception {
    // Setup
    String prefix = "empty-prefix/";

    when(minioClient.listObjects(any(ListObjectsArgs.class)))
        .thenReturn(Collections.emptyList());

    MinioHandler handler = new MinioHandler(options, output);

    // Execute
    int count = handler.downloadDirectory(prefix, tempDir);

    // Verify
    assertEquals(0, count);
  }

  /**
   * Creates a real GetObjectResponse wrapping a ByteArrayInputStream.
   * This avoids mocking FilterInputStream internals that Files.copy depends on.
   */
  private GetObjectResponse createGetObjectResponse(String content) {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
    Headers headers = new Headers.Builder().build();
    return new GetObjectResponse(headers, testBucket, "", "test-object", stream);
  }

  /**
   * Creates a mock Item with the given object name.
   */
  private Item createMockItem(String objectName) {
    Item item = org.mockito.Mockito.mock(Item.class);
    when(item.objectName()).thenReturn(objectName);
    return item;
  }
}
