/**
 * Description of structures which provide external data inputs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.InputStream;


/**
 * Generic resource to read from inputs in an environment.
 */
public interface InputGetterStrategy {

  /**
   * Open an input source.
   *
   * @param identifier The URI or equivalent for the current environment.
   * @return The input stream for the requested resource.
   */
  InputStream open(String identifier);

}
