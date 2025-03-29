package org.joshsim.util;

import io.minio.MinioClient;
import picocli.CommandLine.Option;

/**
 * Configuration options for Minio operations.
 */
public class MinioOptions extends HierarchyConfig {
  
  // Direct command line options
  @Option(names = "--minio-endpoint", description = "Minio server endpoint URL")
  private String minioEndpoint;
  
  @Option(names = "--minio-access-key", description = "Minio access key")
  private String minioAccessKey;
  
  @Option(names = "--minio-secret-key", description = "Minio secret key")
  private String minioSecretKey;
  
  @Option(names = "--minio-bucket", description = "Minio bucket name")
  private String bucketName;
  
  @Option(names = "--minio-object", description = "Base object name/path within bucket")
  private String objectName;
  
  // Expected credential keys and environment variable names
  private static final String MINIO_ENDPOINT_KEY = "minio_server_endpoint";
  private static final String MINIO_ACCESS_KEY_KEY = "minio_access_key";
  private static final String MINIO_SECRET_KEY_KEY = "minio_secret_key";
  
  private static final String MINIO_ENDPOINT_ENV = "MINIO_SERVER_ENDPOINT";
  private static final String MINIO_ACCESS_KEY_ENV = "MINIO_ACCESS_KEY";
  private static final String MINIO_SECRET_KEY_ENV = "MINIO_SECRET_KEY";
  
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
   * Gets the Minio endpoint URL.
   */
  public String getMinioEndpoint() {
    return getCredential(minioEndpoint, MINIO_ENDPOINT_KEY, MINIO_ENDPOINT_ENV, true);
  }
  
  /**
   * Gets the Minio access key.
   */
  private String getMinioAccessKey() {
    return getCredential(minioAccessKey, MINIO_ACCESS_KEY_KEY, MINIO_ACCESS_KEY_ENV, true);
  }
  
  /**
   * Gets the Minio secret key.
   */
  private String getMinioSecretKey() {
    return getCredential(minioSecretKey, MINIO_SECRET_KEY_KEY, MINIO_SECRET_KEY_ENV, true);
  }
  
  /**
   * Gets the bucket name.
   */
  public String getBucketName() {
    return bucketName != null && !bucketName.isEmpty() ? bucketName : "default";
  }
  
  /**
   * Gets the base object name/path within the bucket.
   */
  public String getObjectName() {
    return objectName != null ? objectName : "";
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
}