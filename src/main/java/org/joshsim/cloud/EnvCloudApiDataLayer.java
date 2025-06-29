/**
 * An API data layer that uses the environment for logging and API key checks.
 */

package org.joshsim.cloud;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;

/**
 * Cloud API data layer for testing and local execution not exposed to public Internet.
 *
 * <p>Cloud API data layer which uses an environment variable to check allowed API keys and which
 * simply logs to standard out (System.out.println). If the API key environment variable is empty
 * or is not set, all API keys will be allowed. Otherwise, assumes JOSH_API_KEYS contains a list of
 * API keys as strings separated by commas.</p>
 */
public class EnvCloudApiDataLayer implements CloudApiDataLayer {

  private static final String API_KEY_ENV_VAR = "JOSH_API_KEYS";

  private final ApiKeyStringGetter apiKeyStringGetter;

  /**
   * Constructs an instance of the EnvCloudApiDataLayer.
   *
   * <p>Initializes the API key string getter to retrieve API keys from an environment variable
   * named "JOSH_API_KEYS".</p>
   */
  public EnvCloudApiDataLayer() {
    apiKeyStringGetter = () -> System.getenv(API_KEY_ENV_VAR);
  }

  /**
   * Constructs an instance of the EnvCloudApiDataLayer with a custom API key string getter.
   *
   * @param apiKeyStringGetter A functional interface for retrieving the API keys string,
   *     typically from an environment variable or another source.
   */
  public EnvCloudApiDataLayer(ApiKeyStringGetter apiKeyStringGetter) {
    this.apiKeyStringGetter = apiKeyStringGetter;
  }

  @Override
  public boolean apiKeyIsValid(String key) {
    String apiKeysEnv = apiKeyStringGetter.getApiKeysString();

    // If the environment variable is not set or is empty, allow all API keys.
    if (apiKeysEnv == null || apiKeysEnv.isBlank()) {
      return true;
    }

    // Check if the provided key is in the list of allowed API keys.
    String[] allowedKeys = apiKeysEnv.split(",");
    for (String allowedKey : allowedKeys) {
      if (allowedKey.trim().equals(key)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void log(String key, String type, long runtimeSeconds) {
    CompatibleStringJoiner logJoiner = CompatibilityLayerKeeper.get().createStringJoiner(", ");
    logJoiner.add(generateHash(key));
    logJoiner.add(type);
    logJoiner.add(String.valueOf(runtimeSeconds));
    System.out.println("[josh cloud log] " + logJoiner.toString());
  }

  /**
   * Logs error details securely with hashed API key.
   *
   * @param key The API key (will be hashed for security)
   * @param operation The operation that failed
   * @param exception The exception that occurred
   * @param additionalContext Optional additional context
   */
  public void logError(String key, String operation, Throwable exception, 
        String additionalContext) {
    CompatibleStringJoiner logJoiner = CompatibilityLayerKeeper.get().createStringJoiner(", ");
    logJoiner.add(generateHash(key));
    logJoiner.add("error_" + operation);
    logJoiner.add(exception.getClass().getSimpleName());
    logJoiner.add("\"" + sanitizeForLogging(exception.getMessage()) + "\"");
    
    if (additionalContext != null && !additionalContext.isEmpty()) {
      logJoiner.add("\"" + sanitizeForLogging(additionalContext) + "\"");
    }
    
    System.err.println("[josh cloud error] " + logJoiner.toString());
  }

  /**
   * Sanitizes a message for logging by removing potential sensitive information.
   *
   * @param message The message to sanitize
   * @return A sanitized version safe for logging
   */
  private String sanitizeForLogging(String message) {
    if (message == null) {
      return "null";
    }
    
    // Remove potential file paths and replace with generic placeholder
    String sanitized = message.replaceAll("[/\\\\][^\\s]*", "[PATH]");
    
    // Remove potential API keys or tokens (simple pattern)
    sanitized = sanitized.replaceAll(
        "(?i)api[_\\s-]?key[_\\s-]?[:=]?\\s*[a-zA-Z0-9+/=]{10,}", "[API-KEY]");
    sanitized = sanitized.replaceAll("(?i)token[_\\s-]?[:=]?\\s*[a-zA-Z0-9+/=]{10,}", "[TOKEN]");
    
    // Truncate if too long
    if (sanitized.length() > 200) {
      sanitized = sanitized.substring(0, 197) + "...";
    }
    
    return sanitized;
  }

  /**
   * Dependency injection point for testing for getting the environment variable.
   */
  public interface ApiKeyStringGetter {

    /**
     * Get the API keys string.
     *
     * @return Comma separated list of allowed API keys.
     */
    String getApiKeysString();

  }


  /**
   * Generates an MD5 hash for a given API key.
   *
   * @param message The message to be hashed.
   * @return The MD5 hash of the message as a hexadecimal string.
   * @throws RuntimeException If the MD5 algorithm is not available.
   */
  private String generateHash(String message) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hashBytes = md.digest(message.getBytes());

      StringBuilder stringBuilder = new StringBuilder();
      for (byte b : hashBytes) {
        stringBuilder.append(String.format("%02x", b));
      }
      return stringBuilder.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Could not generate MD5 hash: ", e);
    }
  }

}
