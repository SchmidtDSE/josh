/**
 * No-op implementation of ConfigGetter for compilation purposes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import org.joshsim.engine.config.Config;


/**
 * No-op implementation of ConfigGetter that throws exceptions.
 * This is a placeholder until proper implementation is provided.
 */
public class NoOpConfigGetter implements ConfigGetter {

  @Override
  public Config getConfig(String name) {
    throw new UnsupportedOperationException("Configuration support not yet implemented");
  }

}
