/**
 * Utility class for handling Minio operations.
 */

package org.joshsim.util;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.UploadObjectArgs;
import io.minio.messages.Item;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles operations related to Minio, such as bucket management and file uploads.
 */
public class MinioHandler {
  private final MinioClient minioClient;
  private final String bucketName;
  private final String basePath;
  private final OutputOptions output;

  /**
   * Creates a new MinioHandler.
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
   * Download a single object from MinIO to a local file.
   *
   * @param objectPath The full object path in the bucket
   * @param destination The local file to write to
   * @return true if download was successful
   */
  public boolean downloadFile(String objectPath, File destination) {
    try {
      Files.createDirectories(destination.getParentFile().toPath());

      try (InputStream stream = minioClient.getObject(
          GetObjectArgs.builder()
              .bucket(bucketName)
              .object(objectPath)
              .build())) {
        Files.copy(stream, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

      output.printInfo(
          "Downloaded minio://" + bucketName + "/" + objectPath + " to " + destination.getPath()
      );
      return true;
    } catch (Exception e) {
      output.printError("Failed to download " + objectPath + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Download all objects under a prefix to a local directory.
   *
   * <p>Objects are downloaded preserving their relative path structure beneath the prefix.
   * For example, an object at "job-123/input/simulation.josh" with prefix "job-123/input/"
   * would be downloaded to localDir/simulation.josh.</p>
   *
   * @param prefix The object prefix to list and download (should end with /)
   * @param localDir The local directory to download files into
   * @return The number of files successfully downloaded
   */
  public int downloadDirectory(String prefix, File localDir) {
    int successCount = 0;
    List<String> failedFiles = new ArrayList<>();

    String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";

    try {
      Iterable<Result<Item>> results = minioClient.listObjects(
          ListObjectsArgs.builder()
              .bucket(bucketName)
              .prefix(normalizedPrefix)
              .recursive(true)
              .build()
      );

      for (Result<Item> result : results) {
        Item item = result.get();
        String objectName = item.objectName();

        // Skip directory markers
        if (objectName.endsWith("/")) {
          continue;
        }

        // Compute relative path beneath the prefix
        String relativePath = objectName.substring(normalizedPrefix.length());
        File destination = new File(localDir, relativePath);

        if (downloadFile(objectName, destination)) {
          successCount++;
        } else {
          failedFiles.add(objectName);
        }
      }
    } catch (Exception e) {
      output.printError("Failed to list objects under " + prefix + ": " + e.getMessage());
    }

    if (!failedFiles.isEmpty()) {
      output.printError(
          "Failed to download " + failedFiles.size() + " files: "
              + String.join(", ", failedFiles)
      );
    }

    return successCount;
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
