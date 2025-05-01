/**
 * An API data layer that uses the environment for logging and API key checks.
 */

package org.joshsim.cloud;


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
    logJoiner.add(key);
    logJoiner.add(type);
    logJoiner.add(String.valueOf(runtimeSeconds));
    System.out.println("[josh cloud log] " + logJoiner.toString());
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

}
