package org.joshsim.util;

import picocli.CommandLine.Option;

/**
 * Base options for commands that need Minio support.
  */
public class MinioOptions {

  @Option(names = "--output-dir", description = "Output destination (minio://bucket/path)")
  private String output;

  @Option(names = "--minio-key", description = "Minio/S3 access key")
  private String minioKey;

  @Option(names = "--minio-secret", description = "Minio/S3 secret key")
  private String minioSecret;

  @Option(names = "--minio-endpoint", description = "Minio endpoint URL",
          defaultValue = "http://localhost:9000")
  private String minioEndpoint;

  public boolean isMinioOutput() {
    return output != null && output.startsWith("minio://");
  }

  /**
   * Extracts and returns the bucket name from the output path if it is a Minio URL.
   *
   * @return the bucket name, or null if the output is not a Minio URL
   */
  public String getBucketName() {
    if (!isMinioOutput()) {
      return null;
    }
    String path = output.substring("minio://".length());
    int slashIndex = path.indexOf('/');
    return slashIndex > 0 ? path.substring(0, slashIndex) : path;
  }

  /**
   * Extracts and returns the object name from the output path if it is a Minio URL.
   *
   * @return the object name, or null if the output is not a Minio URL
   */
  public String getObjectName() {
    if (!isMinioOutput()) {
      return null;
    }
    String path = output.substring("minio://".length());
    int slashIndex = path.indexOf('/');
    return slashIndex > 0 ? path.substring(slashIndex + 1) : "";
  }
}
