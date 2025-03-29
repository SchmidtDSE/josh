package org.joshsim.util;

import io.minio.MinioClient;
import picocli.CommandLine.Option;

/**
 * Configuration options for Minio operations.
 */
public class MinioOptions extends HierarchyConfig {
  
  // Value keys for source tracking (also used for JSON config and basis for ENV vars)
  private static final String MINIO_ENDPOINT = "minio_endpoint";
  private static final String MINIO_ACCESS_KEY = "minio_access_key";
  private static final String MINIO_SECRET_KEY = "minio_secret_key";
  
  // Direct command line options (Maybe suffix indicates they might be null/empty)
  @Option(names = "--minio-endpoint", description = "Minio server endpoint URL")
  private String minioEndpointMaybe;
  
  @Option(names = "--minio-access-key", description = "Minio access key")
  private String minioAccessKeyMaybe;
  
  @Option(names = "--minio-secret-key", description = "Minio secret key")
  private String minioSecretKeyMaybe;
  
  @Option(names = "--minio-bucket", description = "Minio bucket name")
  private String bucketNameMaybe;
  
  @Option(names = "--minio-object", description = "Base object name/path within bucket")
  private String objectNameMaybe;
  
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
    return getValue("minio_bucket", bucketNameMaybe, true, 'default');
  }
  
  /**
   * Gets the base object name/path within the bucket.
   */
  public String getObjectName() {
    return getValue("minio_object", objectNameMaybe, true, "");
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
    String endpoint = getMinioEndpoint();
    getMinioAccessKey();  // Values not used but sources recorded
    getMinioSecretKey();  // Values not used but sources recorded
    
    Map<String, ValueSource> sources = getSources();
    
    StringBuilder sb = new StringBuilder("MinioOptions:\n");
    sb.append("  Minio Endpoint: ").append(endpoint)
      .append(" (from ").append(sources.get(MINIO_ENDPOINT)).append(")\n");
    
    // Redact sensitive information
    sb.append("  Minio Access Key: [REDACTED]")
      .append(" (from ").append(sources.get(MINIO_ACCESS_KEY)).append(")\n");
    
    sb.append("  Minio Secret Key: [REDACTED]")
      .append(" (from ").append(sources.get(MINIO_SECRET_KEY)).append(")\n");
    
    // Add bucket and object name
    sb.append("  Bucket Name: ").append(getBucketName()).append("\n");
    sb.append("  Object Name: ").append(getObjectName());
    
    return sb.toString();
  }
}