package org.joshsim.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for configuration that retrieves values from multiple sources
 * in priority order: direct values → config file → environment variables.
 */
public abstract class HierarchyConfig {

  protected String configJsonFilePath;
  private JsonNode cachedJsonConfig;
  private final ObjectMapper mapper = new ObjectMapper();

  // Map to track sources of retrieved values
  private final Map<String, ValueSource> valueSources = new HashMap<>();

  /**
   * Retrieves a configuration value from available sources in priority order.
   * Uses conventions: config file key = key, env var = KEY
   *
   * @param key A unique identifier for this value (used for source tracking)
   * @param directValue Value passed directly (highest priority)
   * @param required Whether to throw an exception if not found
   * @return The value or null if not required and not found
   * @throws IllegalStateException if required value not found
   */
  protected String getValue(
        String key,
        String directValue,
        boolean required,
        String defaultValue
  ) {
    // 1. Direct value (e.g., command line argument)
    if (directValue != null && !directValue.isEmpty()) {
      valueSources.put(key, ValueSource.DIRECT);
      return directValue;
    }

    // 2. Config file - use key as is
    String fileValue = getValueFromJsonFile(key);
    if (fileValue != null && !fileValue.isEmpty()) {
      valueSources.put(key, ValueSource.CONFIG_FILE);
      return fileValue;
    }

    // 3. Environment variable - convert key to UPPER_CASE
    String envVarName = toEnvironmentVariableName(key);
    String envValue = getEnvValue(envVarName);
    if (envValue != null && !envValue.isEmpty()) {
      valueSources.put(key, ValueSource.ENVIRONMENT);
      return envValue;
    }

    valueSources.put(key, ValueSource.NOT_FOUND);
    String outValue = null;
    if (defaultValue != null) {
      outValue = defaultValue;
    }

    if (required && outValue == null) {
      throw new IllegalStateException(
          "Required configuration value '" + key + "' not found in any source");
    }

    return outValue;

  }

  /**
   * Converts a key to environment variable format.
   * Example: "minio_endpoint" -> "MINIO_ENDPOINT"
   */
  private String toEnvironmentVariableName(String key) {
    return key.toUpperCase();
  }

  /**
   * Gets a value from an environment variable.
   * Protected to allow overriding in tests.
   */
  protected String getEnvValue(String name) {
    return System.getenv(name);
  }

  /**
   * Gets the JSON configuration file as a JsonNode.
   */
  protected JsonNode getJsonConfig() {
    if (cachedJsonConfig == null) {
      loadJsonConfigFile();
    }
    return cachedJsonConfig;
  }

  /**
   * Gets a value from the JSON configuration file.
   */
  private String getValueFromJsonFile(String key) {
    try {
      JsonNode jsonConfig = getJsonConfig();
      if (jsonConfig != null && jsonConfig.has(key)) {
        JsonNode value = jsonConfig.get(key);
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
  private void loadJsonConfigFile() {
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

  /**
   * Returns a map of all value sources.
   *
   * @return An unmodifiable map of value keys to their sources
   */
  public Map<String, ValueSource> getSources() {
    return Collections.unmodifiableMap(valueSources);
  }
}
