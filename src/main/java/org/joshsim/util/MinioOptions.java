package org.joshsim.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import java.io.File;
import java.io.IOException;
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

  @Option(names = "--credentials-json", description = "Path to JSON credentials file")
  private File credentialsFile;

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


  /**
   * Returns true if the output path is a Minio URL.
   *
   * @return true if the output path is a Minio URL
   */
  public boolean isMinioOutput() {
    return output != null && output.startsWith("minio://");
  }

  /**
   * Returns the Minio/S3 endpoint.
   *
   * @return the Minio/S3 endpoint
   */
  public String getMinioEndpoint() {
    return minioEndpoint;
  }

  /**
   * Gets credentials from the highest priority available source.
   *
   * @return String array with [accessKey, secretKey]
   * @throws IllegalStateException if no valid credentials could be found
   */
  private String[] getCredentials() {
    // Check command line arguments first
    if (minioKey != null && minioSecret != null) {
      return new String[] { minioKey, minioSecret };
    }

    // Check environment variables
    String envKey = System.getenv("MINIO_ACCESS_KEY");
    String envSecret = System.getenv("MINIO_SECRET_KEY");

    if (envKey != null && !envKey.isEmpty() && envSecret != null && !envSecret.isEmpty()) {
      return new String[] { envKey, envSecret };
    }

    // Check credentials JSON file
    if (credentialsFile != null && credentialsFile.exists()) {
      try {
        String[] fileCredentials = readCredentialsFromFile();
        if (fileCredentials != null) {
          return fileCredentials;
        }
      } catch (IOException e) {
        throw new IllegalStateException("Error reading credentials file: " + e.getMessage(), e);
      }
    }

    // If we get here, no valid credentials were found
    throw new IllegalStateException(
      "No valid Minio credentials found. Please provide credentials via command line arguments, "
      + "environment variables (MINIO_ACCESS_KEY, MINIO_SECRET_KEY), "
      + "or a credentials JSON file (--credentials-json).");
  }

  /**
   * Reads credentials from the JSON file.
   *
   * @return String array with [accessKey, secretKey], or null if invalid
   * @throws IOException if file cannot be read or parsed
   */
  private String[] readCredentialsFromFile() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(credentialsFile);

    JsonNode keyNode = root.path("minio_access_key");
    JsonNode secretNode = root.path("minio_secret_key");

    if (!keyNode.isMissingNode() && !secretNode.isMissingNode()) {
      String key = keyNode.asText();
      String secret = secretNode.asText();

      if (!key.isEmpty() && !secret.isEmpty()) {
        return new String[] { key, secret };
      }
    }

    return null;
  }

  /**
   * Returns the built Minio client using credentials from the highest priority available source:
   * 1. Command line arguments
   * 2. Environment variables
   * 3. Credentials JSON file
   *
   * @return the Minio client
   * @throws IllegalStateException if no valid credentials could be found
   */
  public MinioClient getMinioClient() {
    // Try to get credentials from various sources
    String[] credentials = getCredentials();

    // Build and return the client
    return MinioClient.builder()
        .endpoint(minioEndpoint)
        .credentials(credentials[0], credentials[1])
        .build();
  }
}
