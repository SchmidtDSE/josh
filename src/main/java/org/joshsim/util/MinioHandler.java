/**
 * Utility class for handling Minio operations.
 */

package org.joshsim.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Handles operations related to Minio, such as bucket management and file uploads.
 */
public class MinioHandler {

  /**
   * Basename of the sentinel file written by {@link #writeStagedSentinel} at the root of a
   * staged MinIO prefix. Downstream dispatchers read this to confirm that staging completed
   * cleanly before dispatching a batch job against the prefix.
   */
  public static final String STAGED_SENTINEL_FILENAME = ".josh-staged.json";

  private static final ObjectMapper JSON = new ObjectMapper();

  /** Lifecycle state of a staged MinIO prefix, serialized to {@code .josh-staged.json}. */
  public enum StagedState {
    STAGING, COMPLETE, ERROR;

    String jsonValue() {
      return name().toLowerCase();
    }

    static StagedState fromJson(String value) {
      if (value == null) {
        return null;
      }
      return switch (value) {
        case "staging" -> STAGING;
        case "complete" -> COMPLETE;
        case "error" -> ERROR;
        default -> null;
      };
    }
  }

  /**
   * Parsed contents of a {@code .josh-staged.json} sentinel. Only the timestamp matching the
   * current {@link #state()} is expected to be populated by the writer; all other fields may
   * be null.
   */
  public record StagedStatus(
      StagedState state,
      String startedAt,
      String completedAt,
      String failedAt,
      String message
  ) {}

  private final MinioClient minioClient;
  private final String bucketName;
  private final String basePath;
  private final OutputOptions output;

  /**
   * Creates a new MinioHandler from picocli MinioOptions.
   *
   * @param options The MinioOptions containing credentials and base path
   * @param output For logging information and errors
   * @throws Exception If Minio client creation fails
   */
  public MinioHandler(MinioOptions options, OutputOptions output) throws Exception {
    if (!options.isMinioOutput()) {
      throw new IllegalArgumentException("MinioOptions does not contain a valid Minio URL");
    }

    this.minioClient = options.getMinioClient();
    this.bucketName = options.getBucketName();
    this.basePath = options.getObjectPath();
    this.output = output;

    validateOrCreateBucket(options.isEnsureBucketExists());

  }


  /**
   * Ensures the target bucket exists, creating it if needed.
   *
   * @throws Exception If bucket operations fail
   */
  private void validateOrCreateBucket(boolean createIfNotExists) throws Exception {
    boolean bucketExists = minioClient.bucketExists(
        BucketExistsArgs.builder().bucket(bucketName).build()
    );

    if (!bucketExists) {
      if (createIfNotExists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        output.printInfo("Created bucket: " + bucketName);
      } else {
        throw new IllegalArgumentException("Bucket " + bucketName + " does not exist");
      }
    }
  }

  /**
   * Upload a single file to Minio.
   *
   * @param file The file to upload
   * @param relativePath The path within the base directory where the file should be stored
   * @return true if upload was successful
   */
  public boolean uploadFile(File file, String relativePath) {
    try {
      String objectName = constructObjectPath(relativePath);
      minioClient.uploadObject(
          UploadObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .filename(file.getAbsolutePath())
              .build());

      output.printInfo(
          "Uploaded " + file.getName() + " to minio://" + bucketName + "/" + objectName
      );
      return true;
    } catch (Exception e) {
      output.printError("Failed to upload " + file.getName() + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Upload multiple files to Minio.
   *
   * @param files The files to upload
   * @param baseDir The directory within the base path for these files
   * @return The number of files successfully uploaded
   */
  public int uploadFiles(Iterable<File> files, String baseDir) {
    int successCount = 0;
    List<String> failedFiles = new ArrayList<>();

    for (File file : files) {
      String relativePath = baseDir + "/" + file.getName();
      if (uploadFile(file, relativePath)) {
        successCount++;
      } else {
        failedFiles.add(file.getName());
      }
    }

    if (!failedFiles.isEmpty()) {
      output.printError(
          "Failed to upload " + failedFiles.size() + " files: " + String.join(", ", failedFiles)
      );
    }

    return successCount;
  }

  /**
   * Write a byte array directly to MinIO as a single object.
   *
   * <p>Intended for small payloads like JSON status files. For large data,
   * use {@link #uploadFile(File, String)} or the streaming
   * {@link org.joshsim.lang.io.MinioOutputStreamStrategy} instead.</p>
   *
   * @param data The bytes to write
   * @param relativePath The path within the base directory where the object should be stored
   * @param contentType The MIME type for the object (e.g., "application/json")
   * @throws Exception If the upload fails
   */
  public void putBytes(byte[] data, String relativePath, String contentType) throws Exception {
    String objectName = constructObjectPath(relativePath);
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(bucketName)
              .object(objectName)
              .stream(bais, data.length, -1)
              .contentType(contentType)
              .build()
      );
    }
    output.printInfo(
        "Put " + data.length + " bytes to minio://" + bucketName + "/" + objectName
    );
  }

  /**
   * Constructs the full object path by combining the base path with the relative path.
   *
   * @param relativePath The relative path to append to the base path
   * @return The full object path
   */
  private String constructObjectPath(String relativePath) {
    if (basePath == null || basePath.isEmpty()) {
      return relativePath;
    }

    String path = basePath;
    if (!path.endsWith("/")) {
      path += "/";
    }

    if (relativePath.startsWith("/")) {
      relativePath = relativePath.substring(1);
    }

    return path + relativePath;
  }

  /**
   * Download a file from MinIO to a local destination.
   *
   * @param objectPath The object path within the bucket (relative to base path)
   * @param destination The local file to write to
   * @throws Exception If the download fails
   */
  public void downloadFile(String objectPath, File destination) throws Exception {
    String fullPath = constructObjectPath(objectPath);
    try (InputStream stream = minioClient.getObject(
        GetObjectArgs.builder()
            .bucket(bucketName)
            .object(fullPath)
            .build());
        FileOutputStream fos = new FileOutputStream(destination)) {
      stream.transferTo(fos);
    }
    output.printInfo(
        "Downloaded minio://" + bucketName + "/" + fullPath + " to " + destination.getPath()
    );
  }

  /**
   * Open an input stream to a MinIO object.
   *
   * <p>Caller is responsible for closing the returned stream.</p>
   *
   * @param objectPath The object path within the bucket (relative to base path)
   * @return An InputStream for reading the object contents
   * @throws Exception If the stream cannot be opened
   */
  public InputStream downloadStream(String objectPath) throws Exception {
    String fullPath = constructObjectPath(objectPath);
    return minioClient.getObject(
        GetObjectArgs.builder()
            .bucket(bucketName)
            .object(fullPath)
            .build()
    );
  }

  /**
   * List all object keys under a given prefix.
   *
   * @param prefix The prefix to list under (relative to base path)
   * @return List of full object keys matching the prefix
   * @throws Exception If listing fails
   */
  public List<String> listObjects(String prefix) throws Exception {
    String fullPrefix = constructObjectPath(prefix);
    List<String> keys = new ArrayList<>();
    for (Result<Item> result : minioClient.listObjects(
        ListObjectsArgs.builder()
            .bucket(bucketName)
            .prefix(fullPrefix)
            .recursive(true)
            .build())) {
      keys.add(result.get().objectName());
    }
    return keys;
  }

  /**
   * Delete all objects under a given prefix.
   *
   * @param prefix The prefix to delete under (relative to base path)
   * @return The number of objects deleted
   * @throws Exception If deletion fails
   */
  public int deleteObjects(String prefix) throws Exception {
    List<String> keys = listObjects(prefix);
    for (String key : keys) {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(bucketName)
              .object(key)
              .build()
      );
    }
    if (!keys.isEmpty()) {
      output.printInfo("Deleted " + keys.size() + " objects under " + prefix);
    }
    return keys.size();
  }

  /**
   * Append a trailing slash to a MinIO object prefix if one is not already present.
   *
   * @param prefix The prefix to normalize.
   * @return The prefix with a guaranteed trailing slash.
   */
  public static String normalizePrefix(String prefix) {
    return prefix.endsWith("/") ? prefix : prefix + "/";
  }

  /**
   * Upload every regular file under {@code localDir} to {@code <prefix><relative-path>}.
   *
   * <p>Directory structure under {@code localDir} is preserved. The prefix is normalized
   * with {@link #normalizePrefix(String)} before use. Fails fast on the first upload error.</p>
   *
   * @param localDir The local directory to upload from.
   * @param prefix The MinIO object prefix to upload into.
   * @return The number of files uploaded.
   * @throws IOException If any upload fails or the directory cannot be walked.
   */
  public int uploadDirectory(File localDir, String prefix) throws IOException {
    String normalizedPrefix = normalizePrefix(prefix);
    Path basePath = localDir.toPath();
    int count = 0;
    try (Stream<Path> walker = Files.walk(basePath)) {
      List<Path> files = walker.filter(Files::isRegularFile).toList();
      for (Path file : files) {
        String relativePath = basePath.relativize(file).toString();
        String objectPath = normalizedPrefix + relativePath;
        if (!uploadFile(file.toFile(), objectPath)) {
          throw new IOException("Failed to upload " + file);
        }
        count++;
      }
    }
    return count;
  }

  /**
   * Write the {@link #STAGED_SENTINEL_FILENAME} sentinel at the root of the given prefix.
   *
   * <p>The JSON payload carries a {@code status} field (matching the shape used by
   * {@code JoshSimBatchHandler} for job status) plus the timestamp field corresponding to
   * the state, and an optional {@code message} field for {@link StagedState#ERROR}.</p>
   *
   * @param prefix The MinIO object prefix the sentinel describes.
   * @param state The lifecycle state to record.
   * @param messageOrNull Optional human-readable message (used for {@code ERROR}; may be null).
   * @throws IOException If writing the sentinel fails.
   */
  public void writeStagedSentinel(String prefix, StagedState state, String messageOrNull)
      throws IOException {
    String path = normalizePrefix(prefix) + STAGED_SENTINEL_FILENAME;
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("status", state.jsonValue());
    String now = Instant.now().toString();
    switch (state) {
      case STAGING -> fields.put("startedAt", now);
      case COMPLETE -> fields.put("completedAt", now);
      case ERROR -> fields.put("failedAt", now);
    }
    if (state == StagedState.ERROR && messageOrNull != null) {
      fields.put("message", messageOrNull);
    }
    byte[] bytes = JSON.writeValueAsBytes(fields);
    try {
      putBytes(bytes, path, "application/json");
    } catch (Exception e) {
      throw new IOException("Failed to write staged sentinel to " + path + ": "
          + e.getMessage(), e);
    }
  }

  /**
   * Read and parse the {@link #STAGED_SENTINEL_FILENAME} sentinel at the root of the given
   * prefix. Returns {@link Optional#empty()} if the sentinel does not exist.
   *
   * @param prefix The MinIO object prefix whose sentinel should be read.
   * @return The parsed sentinel, or empty if absent.
   * @throws IOException If reading fails for any reason other than missing object.
   */
  public Optional<StagedStatus> readStagedSentinel(String prefix) throws IOException {
    String path = normalizePrefix(prefix) + STAGED_SENTINEL_FILENAME;
    String json;
    try (InputStream stream = downloadStream(path)) {
      json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      if (isNotFoundError(e)) {
        return Optional.empty();
      }
      throw new IOException("Failed to read staged sentinel at " + path + ": "
          + e.getMessage(), e);
    }
    JsonNode root = JSON.readTree(json);
    return Optional.of(new StagedStatus(
        StagedState.fromJson(root.has("status") ? root.get("status").asText() : null),
        root.has("startedAt") ? root.get("startedAt").asText() : null,
        root.has("completedAt") ? root.get("completedAt").asText() : null,
        root.has("failedAt") ? root.get("failedAt").asText() : null,
        root.has("message") ? root.get("message").asText() : null
    ));
  }

  private static boolean isNotFoundError(Exception exception) {
    String message = exception.getMessage();
    if (message == null) {
      return false;
    }
    return message.contains("NoSuchKey")
        || message.contains("The specified key does not exist")
        || message.contains("Object does not exist");
  }

  /**
   * Gets the bucket name used by this handler.
   *
   * @return The bucket name
   */
  public String getBucketName() {
    return bucketName;
  }

  /**
   * Gets the base path used by this handler.
   *
   * @return The base path within the bucket
   */
  public String getBasePath() {
    return basePath;
  }
}
