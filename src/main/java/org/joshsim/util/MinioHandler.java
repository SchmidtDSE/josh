/**
 * Utility class for handling Minio operations.
 */

package org.joshsim.util;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import java.io.File;
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
    this.basePath = options.getObjectName(); // Base directory in the bucket
    this.output = output;

    // Ensure the bucket exists
    ensureBucketExists();
  }

  /**
   * Ensures the target bucket exists, creating it if needed.
   *
   * @throws Exception If bucket operations fail
   */
  private void ensureBucketExists() throws Exception {
    boolean bucketExists = minioClient.bucketExists(
        BucketExistsArgs.builder().bucket(bucketName).build());

    if (!bucketExists) {
      minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
      output.printInfo("Created bucket: " + bucketName);
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
      output.printError("Failed to upload " + failedFiles.size() + " files: " +
                       String.join(", ", failedFiles));
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
