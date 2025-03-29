package org.joshsim.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

/**
 * Abstract base class for configuration that retrieves credentials from multiple sources
 * in priority order: command line arguments → credentials file → environment variables.
 */
public abstract class Config {

  protected String credentialsFilePath;
  private JsonNode cachedCredentials;
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Retrieves a credential value from available sources in priority order.
   *
   * @param directValue Value passed directly (highest priority)
   * @param credentialKey Key to look for in credentials file
   * @param envVarName Environment variable name
   * @param required Whether to throw an exception if not found
   * @return The credential value or null if not required and not found
   * @throws IllegalStateException if required credential not found
   */
  protected String getCredential(
        String directValue,
        String credentialKey,
        String envVarName,
        boolean required
  ) {
    // 1. Direct value (e.g., command line argument)
    if (directValue != null && !directValue.isEmpty()) {
      return directValue;
    }

    // 2. Credentials file
    String fileValue = getCredentialFromFile(credentialKey);
    if (fileValue != null && !fileValue.isEmpty()) {
      return fileValue;
    }

    // 3. Environment variable
    String envValue = System.getenv(envVarName);
    if (envValue != null && !envValue.isEmpty()) {
      return envValue;
    }

    if (required) {
      throw new IllegalStateException(
          "Required credential '" + credentialKey + "' not found in any source");
    }

    return null;
  }

  /**
   * Gets a credential from the credentials file.
   */
  private String getCredentialFromFile(String key) {
    try {
      if (cachedCredentials == null) {
        loadCredentialsFile();
      }

      if (cachedCredentials != null && cachedCredentials.has(key)) {
        JsonNode value = cachedCredentials.get(key);
        return value.isTextual() ? value.asText() : null;
      }
    } catch (Exception e) {
      // Silently continue to the next source
    }
    return null;
  }

  /**
   * Loads credentials from the JSON file.
   */
  private void loadCredentialsFile() {
    File file = new File(credentialsFilePath);
    if (file.exists() && file.canRead()) {
      try {
        cachedCredentials = mapper.readTree(file);
      } catch (IOException e) {
        cachedCredentials = null;
      }
    }
  }

  /**
   * Updates the credentials file path and clears the cache.
   */
  protected void setCredentialsFilePath(String path) {
    this.credentialsFilePath = path;
    this.cachedCredentials = null;
  }
}
