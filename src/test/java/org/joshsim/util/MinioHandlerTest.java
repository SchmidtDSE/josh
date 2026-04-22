package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.UploadObjectArgs;
import io.minio.messages.Item;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import okhttp3.Headers;
import org.joshsim.util.MinioHandler.StagedState;
import org.joshsim.util.MinioHandler.StagedStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
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

  // --- Download tests ---

  @TempDir
  File tempDir;

  private MinioHandler createHandler() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    when(options.isEnsureBucketExists()).thenReturn(false);
    return new MinioHandler(options, output);
  }

  @Test
  void downloadFile_shouldWriteContentsToDestination() throws Exception {
    // Setup
    MinioHandler handler = createHandler();
    byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
    GetObjectResponse mockResponse = new GetObjectResponse(
        Headers.of(), testBucket, "", "test/file.txt",
        new ByteArrayInputStream(content)
    );
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

    File destination = new File(tempDir, "downloaded.txt");

    // Execute
    handler.downloadFile("test/file.txt", destination);

    // Verify
    assertTrue(destination.exists());
    assertEquals("hello world", Files.readString(destination.toPath()));
    verify(output).printInfo(contains("Downloaded"));
  }

  @Test
  void downloadFile_shouldUseCorrectObjectPath() throws Exception {
    // Setup - handler with a base path
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    when(options.isEnsureBucketExists()).thenReturn(false);
    when(options.getObjectPath()).thenReturn("base/path");
    MinioHandler handler = new MinioHandler(options, output);

    byte[] content = "data".getBytes(StandardCharsets.UTF_8);
    GetObjectResponse mockResponse = new GetObjectResponse(
        Headers.of(), testBucket, "", "base/path/sub/file.txt",
        new ByteArrayInputStream(content)
    );
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

    File destination = new File(tempDir, "out.txt");

    // Execute
    handler.downloadFile("sub/file.txt", destination);

    // Verify the constructed path includes the base
    ArgumentCaptor<GetObjectArgs> captor = ArgumentCaptor.forClass(GetObjectArgs.class);
    verify(minioClient).getObject(captor.capture());
    assertEquals("base/path/sub/file.txt", captor.getValue().object());
  }

  @Test
  void downloadStream_shouldReturnInputStream() throws Exception {
    // Setup
    MinioHandler handler = createHandler();
    byte[] content = "stream content".getBytes(StandardCharsets.UTF_8);
    GetObjectResponse mockResponse = new GetObjectResponse(
        Headers.of(), testBucket, "", "obj.txt",
        new ByteArrayInputStream(content)
    );
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

    // Execute
    try (InputStream stream = handler.downloadStream("obj.txt")) {
      String result = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      assertEquals("stream content", result);
    }
  }

  @Test
  void listObjects_shouldReturnObjectKeys() throws Exception {
    // Setup
    MinioHandler handler = createHandler();

    Item item1 = new Item() {
      @Override
      public String objectName() {
        return "prefix/file1.csv";
      }
    };
    Item item2 = new Item() {
      @Override
      public String objectName() {
        return "prefix/file2.csv";
      }
    };

    @SuppressWarnings("unchecked")
    Iterable<Result<Item>> results = Arrays.asList(
        new Result<>(item1),
        new Result<>(item2)
    );
    when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(results);

    // Execute
    List<String> keys = handler.listObjects("prefix/");

    // Verify
    assertEquals(2, keys.size());
    assertEquals("prefix/file1.csv", keys.get(0));
    assertEquals("prefix/file2.csv", keys.get(1));
  }

  @Test
  void listObjects_shouldReturnEmptyListWhenNoneMatch() throws Exception {
    // Setup
    MinioHandler handler = createHandler();
    @SuppressWarnings("unchecked")
    Iterable<Result<Item>> results = Collections.emptyList();
    when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(results);

    // Execute
    List<String> keys = handler.listObjects("nonexistent/");

    // Verify
    assertTrue(keys.isEmpty());
  }

  // --- putBytes tests ---

  @Test
  void putBytes_shouldUploadDataWithCorrectPath() throws Exception {
    // Setup
    MinioHandler handler = createHandler();
    byte[] data = "{\"status\":\"running\"}".getBytes(StandardCharsets.UTF_8);

    // Execute
    handler.putBytes(data, "batch-status/job-1/status.json", "application/json");

    // Verify
    ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
    verify(minioClient).putObject(captor.capture());
    assertEquals(testBucket, captor.getValue().bucket());
    assertEquals("batch-status/job-1/status.json", captor.getValue().object());
    assertEquals("application/json", captor.getValue().contentType());
    verify(output).printInfo(contains("Put " + data.length + " bytes"));
  }

  @Test
  void putBytes_shouldPrependBasePath() throws Exception {
    // Setup - handler with a base path
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    when(options.isEnsureBucketExists()).thenReturn(false);
    when(options.getObjectPath()).thenReturn("base/path");
    MinioHandler handler = new MinioHandler(options, output);

    byte[] data = "test".getBytes(StandardCharsets.UTF_8);

    // Execute
    handler.putBytes(data, "status.json", "application/json");

    // Verify
    ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
    verify(minioClient).putObject(captor.capture());
    assertEquals("base/path/status.json", captor.getValue().object());
  }

  @Test
  void putBytes_shouldPropagateException() throws Exception {
    // Setup
    MinioHandler handler = createHandler();
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenThrow(new RuntimeException("upload failed"));

    byte[] data = "test".getBytes(StandardCharsets.UTF_8);

    // Execute & Verify
    Exception exception = assertThrows(
        RuntimeException.class,
        () -> handler.putBytes(data, "path.json", "application/json")
    );
    assertTrue(exception.getMessage().contains("upload failed"));
  }

  // --- deleteObjects tests ---

  @Test
  void deleteObjects_shouldDeleteAllListedObjects() throws Exception {
    // Setup
    MinioHandler handler = createHandler();

    Item item1 = new Item() {
      @Override
      public String objectName() {
        return "prefix/file1.csv";
      }
    };
    Item item2 = new Item() {
      @Override
      public String objectName() {
        return "prefix/file2.csv";
      }
    };

    @SuppressWarnings("unchecked")
    Iterable<Result<Item>> results = Arrays.asList(
        new Result<>(item1),
        new Result<>(item2)
    );
    when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(results);

    // Execute
    int deleted = handler.deleteObjects("prefix/");

    // Verify
    assertEquals(2, deleted);
    verify(minioClient, times(2)).removeObject(any(RemoveObjectArgs.class));
    verify(output).printInfo(contains("Deleted 2 objects"));
  }

  @Test
  void deleteObjects_shouldReturnZeroWhenNothingToDelete() throws Exception {
    // Setup
    MinioHandler handler = createHandler();
    @SuppressWarnings("unchecked")
    Iterable<Result<Item>> results = Collections.emptyList();
    when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(results);

    // Execute
    int deleted = handler.deleteObjects("empty/");

    // Verify
    assertEquals(0, deleted);
    verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
  }

  // --- normalizePrefix tests ---

  @Test
  void normalizePrefix_appendsSlashWhenMissing() {
    assertEquals("batch-jobs/foo/inputs/", MinioHandler.normalizePrefix("batch-jobs/foo/inputs"));
  }

  @Test
  void normalizePrefix_leavesTrailingSlashAlone() {
    assertEquals("batch-jobs/foo/inputs/", MinioHandler.normalizePrefix("batch-jobs/foo/inputs/"));
  }

  // --- uploadDirectory tests ---

  @Test
  void uploadDirectory_walksAndUploadsWithRelativeKeys() throws Exception {
    File root = new File(tempDir, "inputs");
    File nested = new File(root, "sub");
    assertTrue(nested.mkdirs());
    Files.writeString(new File(root, "a.txt").toPath(), "a");
    Files.writeString(new File(nested, "b.txt").toPath(), "b");

    MinioHandler handler = createHandler();
    int count = handler.uploadDirectory(root, "prefix/");

    assertEquals(2, count);
    ArgumentCaptor<UploadObjectArgs> captor = ArgumentCaptor.forClass(UploadObjectArgs.class);
    verify(minioClient, times(2)).uploadObject(captor.capture());
    List<String> objects = captor.getAllValues().stream().map(UploadObjectArgs::object).sorted()
        .toList();
    assertEquals(List.of("prefix/a.txt", "prefix/sub/b.txt"), objects);
  }

  @Test
  void uploadDirectory_normalizesPrefixWithoutTrailingSlash() throws Exception {
    MinioHandler handler = createHandler();
    File root = new File(tempDir, "inputs");
    assertTrue(root.mkdirs());
    Files.writeString(new File(root, "a.txt").toPath(), "a");

    handler.uploadDirectory(root, "prefix");

    ArgumentCaptor<UploadObjectArgs> captor = ArgumentCaptor.forClass(UploadObjectArgs.class);
    verify(minioClient).uploadObject(captor.capture());
    assertEquals("prefix/a.txt", captor.getValue().object());
  }

  @Test
  void uploadDirectory_throwsOnUploadFailure() throws Exception {
    File root = new File(tempDir, "inputs");
    assertTrue(root.mkdirs());
    Files.writeString(new File(root, "a.txt").toPath(), "a");
    when(minioClient.uploadObject(any(UploadObjectArgs.class)))
        .thenThrow(new RuntimeException("upload kaboom"));

    MinioHandler handler = createHandler();
    assertThrows(IOException.class, () -> handler.uploadDirectory(root, "prefix/"));
  }

  // --- sentinel tests ---

  @Test
  void writeStagedSentinel_writesCorrectObjectPath() throws Exception {
    MinioHandler handler = createHandler();

    handler.writeStagedSentinel("batch-jobs/foo/inputs/", StagedState.STAGING, null);

    ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
    verify(minioClient).putObject(captor.capture());
    assertEquals("batch-jobs/foo/inputs/.josh-staged.json", captor.getValue().object());
    assertEquals("application/json", captor.getValue().contentType());
  }

  @Test
  void writeStagedSentinel_normalizesPrefixWithoutTrailingSlash() throws Exception {
    MinioHandler handler = createHandler();

    handler.writeStagedSentinel("batch-jobs/foo/inputs", StagedState.COMPLETE, null);

    ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
    verify(minioClient).putObject(captor.capture());
    assertEquals("batch-jobs/foo/inputs/.josh-staged.json", captor.getValue().object());
  }

  @Test
  void readStagedSentinel_parsesCompleteState() throws Exception {
    MinioHandler handler = createHandler();
    byte[] json = "{\"status\":\"complete\",\"completedAt\":\"2026-04-22T12:00:00Z\"}"
        .getBytes(StandardCharsets.UTF_8);
    GetObjectResponse mockResponse = new GetObjectResponse(
        Headers.of(), testBucket, "", "prefix/.josh-staged.json",
        new ByteArrayInputStream(json)
    );
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

    Optional<StagedStatus> status = handler.readStagedSentinel("prefix/");

    assertTrue(status.isPresent());
    assertEquals(StagedState.COMPLETE, status.get().state());
    assertEquals("2026-04-22T12:00:00Z", status.get().completedAt());
    assertTrue(status.get().startedAt() == null);
    assertTrue(status.get().message() == null);
  }

  @Test
  void readStagedSentinel_parsesErrorStateWithMessage() throws Exception {
    MinioHandler handler = createHandler();
    byte[] json = ("{\"status\":\"error\",\"failedAt\":\"2026-04-22T12:00:00Z\","
        + "\"message\":\"disk full\"}").getBytes(StandardCharsets.UTF_8);
    GetObjectResponse mockResponse = new GetObjectResponse(
        Headers.of(), testBucket, "", "prefix/.josh-staged.json",
        new ByteArrayInputStream(json)
    );
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

    Optional<StagedStatus> status = handler.readStagedSentinel("prefix/");

    assertTrue(status.isPresent());
    assertEquals(StagedState.ERROR, status.get().state());
    assertEquals("disk full", status.get().message());
    assertEquals("2026-04-22T12:00:00Z", status.get().failedAt());
  }

  @Test
  void readStagedSentinel_readsFromCorrectPath() throws Exception {
    MinioHandler handler = createHandler();
    byte[] json = "{\"status\":\"complete\"}".getBytes(StandardCharsets.UTF_8);
    GetObjectResponse mockResponse = new GetObjectResponse(
        Headers.of(), testBucket, "", "prefix/.josh-staged.json",
        new ByteArrayInputStream(json)
    );
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

    handler.readStagedSentinel("prefix");

    ArgumentCaptor<GetObjectArgs> captor = ArgumentCaptor.forClass(GetObjectArgs.class);
    verify(minioClient).getObject(captor.capture());
    assertEquals("prefix/.josh-staged.json", captor.getValue().object());
  }

  @Test
  void readStagedSentinel_returnsEmptyWhenMissing() throws Exception {
    MinioHandler handler = createHandler();
    when(minioClient.getObject(any(GetObjectArgs.class)))
        .thenThrow(new RuntimeException("NoSuchKey: no such object exists"));

    Optional<StagedStatus> status = handler.readStagedSentinel("prefix/");

    assertTrue(status.isEmpty());
  }

  @Test
  void readStagedSentinel_propagatesNonNotFoundError() throws Exception {
    MinioHandler handler = createHandler();
    when(minioClient.getObject(any(GetObjectArgs.class)))
        .thenThrow(new RuntimeException("permission denied"));

    assertThrows(IOException.class, () -> handler.readStagedSentinel("prefix/"));
  }
}
