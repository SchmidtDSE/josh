package org.joshsim.util;

import io.minio.MinioClient;
import java.util.Map;
import picocli.CommandLine.Option;

/**
 * Configuration options for Minio operations.
 */
public class MinioOptions extends HierarchyConfig {

  // Value keys for source tracking (also used for JSON config and basis for ENV vars)
  private static final String MINIO_ENDPOINT = "minio_endpoint";
  private static final String MINIO_ACCESS_KEY = "minio_access_key";
  private static final String MINIO_SECRET_KEY = "minio_secret_key";
  private static final String MINIO_BUCKET_NAME = "minio_bucket";

  // Direct command line options (Maybe suffix indicates they might be null/empty)
  @Option(names = "--minio-endpoint", description = "Minio server endpoint URL")
  private String minioEndpointMaybe;

  @Option(names = "--minio-access-key", description = "Minio access key")
  private String minioAccessKeyMaybe;

  @Option(names = "--minio-secret-key", description = "Minio secret key")
  private String minioSecretKeyMaybe;

  @Option(names = "--minio-bucket", description = "Minio bucket name")
  private String bucketNameMaybe;

  @Option(names = "--minio-path", description = "Base object name/path within bucket")
  private String objectPathMaybe;

  @Option(
      names = "--ensure-bucket-exists",
      description = "Ensure the bucket exists before uploading"
  )
  private boolean ensureBucketExists = false;

  /**
   * Sets the path to the JSON configuration file.
   *
   * @param path the path to the JSON configuration file
   */
  @Option(names = "--config-file", description = "Path to JSON configuration file")
  public void setConfigFile(String path) {
    setConfigJsonFilePath(path);
  }

  /**
   * Checks if Minio output is configured with a valid endpoint.
   */
  public boolean isMinioOutput() {
    try {
      String endpoint = getMinioEndpoint();
      return endpoint != null && !endpoint.isEmpty();
    } catch (IllegalStateException e) {
      return false;
    }
  }

  /**
   * Determines whether the bucket should be created if it doesnt exist.
   *
   * @return true if bucket creation is enforced, false otherwise
   */
  public boolean isEnsureBucketExists() {
    return ensureBucketExists;
  }

  /**
   * Gets the Minio endpoint URL.
   */
  public String getMinioEndpoint() {
    return getValue(MINIO_ENDPOINT, minioEndpointMaybe, true, null);
  }

  /**
   * Gets the Minio access key.
   */
  private String getMinioAccessKey() {
    return getValue(MINIO_ACCESS_KEY, minioAccessKeyMaybe, true, null);
  }

  /**
   * Gets the Minio secret key.
   */
  private String getMinioSecretKey() {
    return getValue(MINIO_SECRET_KEY, minioSecretKeyMaybe, true, null);
  }

  /**
   * Gets the bucket name.
   */
  public String getBucketName() {
    return getValue(MINIO_BUCKET_NAME, bucketNameMaybe, false, null);
  }

  /**
   * Gets the base object name/path within the bucket.
   */
  public String getObjectPath() {
    return getValue("minio_object_path", objectPathMaybe, true, "");
  }

  /**
   * Gets the complete object name by combining base path and filename.
   * Follows the pattern: [minio-path]/[filename]
   *
   * @param filename The name of the file being processed
   * @return The complete object name to use in MinIO
   */
  public String getObjectName(String filename) {
    String basePath = getObjectPath();
    basePath = basePath.endsWith("/") ? basePath : basePath + "/";
    return basePath + "/" + filename;
  }

  /**
   * Gets the complete object name by combining base path and filename,
   * including extra subdirectories.
   * Follows the pattern: [minio-path]/[subDirectories]/[filename]
   *
   * @param subDirectories The subdirectories to include in the object name
   * @param filename The name of the file being processed
   * @return The complete object name to use in MinIO
   */
  public String getObjectName(String subDirectories, String filename) {
    String basePath = getObjectPath();
    basePath = basePath.endsWith("/") ? basePath : basePath + "/";
    String completePath = basePath + subDirectories;
    completePath = completePath.endsWith("/") ? completePath : completePath + "/";
    return completePath + filename;
  }

  /**
   * Returns the built Minio client using credentials from highest priority source.
   */
  public MinioClient getMinioClient() {
    return MinioClient.builder()
      .endpoint(getMinioEndpoint())
      .credentials(getMinioAccessKey(), getMinioSecretKey())
      .build();
  }

  @Override
  public String toString() {
    // Force retrieval of all values to populate the sources
    getMinioAccessKey();  // Values not used but sources recorded
    getMinioSecretKey();  // Values not used but sources recorded
    getBucketName();      // Values not used but sources recorded
    String endpoint = getMinioEndpoint();

    Map<String, ValueSource> sources = getSources();

    StringBuilder sb = new StringBuilder("MinioOptions:\n");
    sb.append("  Minio Endpoint: ").append(endpoint)
      .append(" (from ").append(sources.get(MINIO_ENDPOINT)).append(")\n");

    sb.append("  Minio Bucket").append(endpoint)
      .append(" (from ").append(sources.get(MINIO_BUCKET_NAME)).append(")\n");

    // Redact sensitive information
    sb.append("  Minio Access Key: [REDACTED]")
      .append(" (from ").append(sources.get(MINIO_ACCESS_KEY)).append(")\n");

    sb.append("  Minio Secret Key: [REDACTED]")
      .append(" (from ").append(sources.get(MINIO_SECRET_KEY)).append(")\n");

    // Add object name
    sb.append("  Object Path: ").append(getObjectPath());

    return sb.toString();
  }
}
