
/**
 * Utility to facilitate API key checking.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import io.undertow.server.handlers.form.FormData;

/**
 * Utility to support API key checking.
 */
public class ApiKeyUtil {

  /**
   * Check if an API key is valid.
   *
   * @param formData FormData from which the API key should be checked.
   * @param apiInternalLayer Layer through which API keys read from the form should be checked.
   * @returns True if the API key is found and valid. False otherwise.
   */
  public static ApiCheckResult checkApiKey(FormData formData,
        CloudApiDataLayer apiInternalLayer) {

    if (!formData.contains("apiKey")) {
      return new ApiCheckResult("", false);
    }

    String apiKey = formData.getFirst("apiKey").getValue();
    if (apiKey == null) {
      apiKey = "";
    }

    if (apiInternalLayer.apiKeyIsValid(apiKey)) {
      return new ApiCheckResult(apiKey, true);
    } else {
      return new ApiCheckResult(apiKey, false);
    }
  }

  /**
   * A class representing the result of an API key check.
   */
  public static class ApiCheckResult {
    private final String apiKey;
    private final boolean keyIsValid;

    /**
     * Constructs an ApiCheckResult with the specified API key and validity status.
     *
     * @param apiKey the API key to be checked
     * @param keyIsValid true if the API key is valid, false otherwise
     */
    public ApiCheckResult(String apiKey, boolean keyIsValid) {
      this.apiKey = apiKey;
      this.keyIsValid = keyIsValid;
    }

    /**
     * Returns the API key associated with this result.
     *
     * @return the API key
     */
    public String getApiKey() {
      return apiKey;
    }

    /**
     * Returns the validity status of the API key.
     *
     * @return true if the API key is valid, false otherwise.
     */
    public boolean getKeyIsValid() {
      return keyIsValid;
    }
  }
}
