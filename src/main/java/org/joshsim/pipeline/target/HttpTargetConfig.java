/**
 * Configuration for an HTTP-based batch execution target.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * HTTP target configuration parsed from a target profile JSON file.
 *
 * <p>Holds the endpoint URL and API key for targets running the joshsim server
 * (Cloud Run, self-hosted, etc.). The endpoint should accept POST requests at
 * {@code /runBatch}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpTargetConfig {

  @JsonProperty("endpoint")
  private String endpoint;

  @JsonProperty("apiKey")
  private String apiKey;

  /**
   * Default constructor for Jackson deserialization.
   */
  public HttpTargetConfig() {
  }

  /**
   * Constructs an HttpTargetConfig with the given values.
   *
   * @param endpoint The HTTP endpoint URL for the joshsim server.
   * @param apiKey The API key for authentication.
   */
  public HttpTargetConfig(String endpoint, String apiKey) {
    this.endpoint = endpoint;
    this.apiKey = apiKey;
  }

  /**
   * Returns the HTTP endpoint URL.
   *
   * @return The endpoint URL.
   */
  public String getEndpoint() {
    return endpoint;
  }

  /**
   * Returns the API key for authentication.
   *
   * @return The API key.
   */
  public String getApiKey() {
    return apiKey;
  }
}
