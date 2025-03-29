package org.joshsim.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

/**
 * Abstract base class for configuration that retrieves values from multiple sources
 * in priority order: direct values → config file → environment variables.
 */
public abstract class Config {

  protected String configJsonFilePath;
  private JsonNode cachedJsonConfig;
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Retrieves a configuration value from available sources in priority order.
   *
   * @param directValue Value passed directly (highest priority)
   * @param configKey Key to look for in config file
   * @param envVarName Environment variable name
   * @param required Whether to throw an exception if not found
   * @return The value or null if not required and not found
   * @throws IllegalStateException if required value not found
   */
  protected String getValue(
        String directValue,
        String configKey,
        String envVarName,
        boolean required
  ) {
    // 1. Direct value (e.g., command line argument)
    if (directValue != null && !directValue.isEmpty()) {
      return directValue;
    }

    // 2. Config file
    String fileValue = getValueFromJsonFile(configKey);
    if (fileValue != null && !fileValue.isEmpty()) {
      return fileValue;
    }

    // 3. Environment variable
    String envValue = getEnvVar(envVarName);
    if (envValue != null && !envValue.isEmpty()) {
      return envValue;
    }

    if (required) {
      throw new IllegalStateException(
          "Required configuration value '" + configKey + "' not found in any source");
    }

    return null;
  }

  /**
   * Gets a value from an environment variable.
   */
  private String getEnvVar(String name) {
    return System.getenv(name);
  }

  /**
   * Gets a value from the JSON configuration file.
   */
  private String getValueFromJsonFile(String key) {
    try {
      if (cachedJsonConfig == null) {
        loadJsonConfigFile();
      }

      if (cachedJsonConfig != null && cachedJsonConfig.has(key)) {
        JsonNode value = cachedJsonConfig.get(key);
        return value.isTextual() ? value.asText() : null;
      }
    } catch (Exception e) {
      // Silently continue to the next source
    }
    return null;
  }

  /**
   * Loads configuration from the JSON file.
   */
  private void loadConfigJsonFile() {
    if (configJsonFilePath == null) {
      return;
    }
    
    File file = new File(configJsonFilePath);
    if (file.exists() && file.canRead()) {
      try {
        cachedJsonConfig = mapper.readTree(file);
      } catch (IOException e) {
        cachedJsonConfig = null;
      }
    }
  }

  /**
   * Updates the configuration file path and clears the cache.
   */
  protected void setConfigJsonFilePath(String path) {
    this.configJsonFilePath = path;
    this.cachedJsonConfig = null;
  }
}