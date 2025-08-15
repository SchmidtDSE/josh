/**
 * No-op implementation of ConfigGetter for compilation purposes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import org.joshsim.lang.bridge.ConfigGetter;


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
