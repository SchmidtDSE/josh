/**
 * Singleton pattern for MinioClient to enable connection pooling.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import io.minio.MinioClient;

/**
 * Provides a singleton MinioClient instance for connection pooling efficiency.
 *
 * <p>This class implements a thread-safe singleton pattern to ensure only one MinioClient
 * is created per endpoint. The client is recreated if the endpoint changes. This approach
 * improves performance by reusing HTTP connections across multiple operations.</p>
 */
public class MinioClientSingleton {

  private static MinioClient instance;
  private static String currentEndpoint;

  /**
   * Private constructor to prevent instantiation.
   */
  private MinioClientSingleton() {
    // Singleton pattern - no instantiation allowed
  }

  /**
   * Gets or creates a MinioClient instance using the provided options.
   *
   * <p>If a client already exists with the same endpoint, it is reused. If the endpoint
   * has changed, a new client is created using MinioOptions.getMinioClient().</p>
   *
   * @param options The MinioOptions containing endpoint and credentials
   * @return A MinioClient instance configured with the specified options
   * @throws IllegalArgumentException if MinioOptions does not contain valid configuration
   */
  public static synchronized MinioClient getInstance(MinioOptions options) {
    if (!options.isMinioOutput()) {
      throw new IllegalArgumentException("MinioOptions does not contain a valid Minio endpoint");
    }

    String endpoint = options.getMinioEndpoint();

    // Create new client if endpoint changed
    if (instance == null || !endpoint.equals(currentEndpoint)) {
      instance = options.getMinioClient();
      currentEndpoint = endpoint;
    }

    return instance;
  }

  /**
   * Resets the singleton instance. Primarily used for testing.
   *
   * <p>This method clears the cached client, forcing creation of a new instance
   * on the next call to getInstance().</p>
   */
  public static synchronized void reset() {
    instance = null;
    currentEndpoint = null;
  }
}
