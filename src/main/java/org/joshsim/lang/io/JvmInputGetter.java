/**
 * Logic for using non-sandboxed inputs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.InputStream;
import java.net.URI;


/**
 * Abstract base class providing template method pattern for non-sandboxed input access.
 *
 * <p>Strategy which loads streams from regular resources through the JVM's utilities for working
 * with the native file system and with the network outside of the sandbox. This class uses the
 * template method pattern to allow different strategies for loading files while maintaining
 * consistent URI handling.</p>
 */
public abstract class JvmInputGetter implements InputGetterStrategy {

  @Override
  public final InputStream open(String identifier) {
    if (identifier.contains(":")) {
      URI uri = URI.create(identifier);
      return loadFromUri(uri);
    } else {
      return readImpliedPath(identifier);
    }
  }

  @Override
  public final boolean exists(String identifier) {
    if (identifier.contains(":")) {
      // For URIs, we would need to try to open it to check existence
      // For now, assume URIs exist (they might fail when actually opened)
      return true;
    } else {
      return checkImpliedPathExists(identifier);
    }
  }

  /**
   * Load this resource from a URI.
   *
   * @param uri The URI from which to load the resource.
   * @return The input stream found at the given URI.
   * @throws RuntimeException raised if error encountered in opening the input stream.
   */
  protected final InputStream loadFromUri(URI uri) {
    try {
      return uri.toURL().openStream();
    } catch (Exception e) {
      throw new RuntimeException("Failed to open stream from URI: " + uri, e);
    }
  }

  /**
   * Template method for reading implied path identifiers (non-URI).
   *
   * @param identifier The identifier to load from the implementation-specific strategy.
   * @return The input stream for the requested resource.
   * @throws RuntimeException raised if error encountered in opening the input stream.
   */
  protected abstract InputStream readImpliedPath(String identifier);

  /**
   * Template method for checking existence of implied path identifiers (non-URI).
   *
   * @param identifier The identifier to check for existence.
   * @return True if the resource exists and can be opened, false otherwise.
   */
  protected abstract boolean checkImpliedPathExists(String identifier);

}
