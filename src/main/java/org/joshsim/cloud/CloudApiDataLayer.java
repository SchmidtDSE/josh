/**
 * Description of structures which can be used to run internal API service logic.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;


/**
 * Description of a facade which handles API service internal logic.
 *
 * Description of a facade which handles API service internal logic such as checking for API
 * validity and limited logging of user requests.
 */
public interface CloudApiDataLayer {

  /**
   * Check if an API key is valid.
   *
   * @param key The key to be checked where it must match a known key exactly case-sensitive.
   * @returns True if the API key is valid and known or false otherwise.
   */
  boolean apiKeyIsValid(String key);

  /**
   * Log that an API key was used for an operation.
   *
   * @param key The key which was used to execute this operation and which should be associated in
   *     the logs.
   * @param type The type of operation completed like "distribute" for a leader node and "simulate"
   *     for a worker node.
   * @param runtimeSeconds The duration of the operation in seconds which should be reported.
   */
  void log(String key, String type, long runtimeSeconds);
  
}
