/**
 * No-op implementation of ConfigGetter for compilation purposes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import java.util.Optional;
import org.joshsim.lang.bridge.ConfigGetter;


/**
 * No-op implementation of ConfigGetter that returns empty Optional.
 * This is a placeholder until proper implementation is provided.
 */
public class NoOpConfigGetter implements ConfigGetter {

  @Override
  public Optional<Config> getConfig(String name) {
    // Always return empty since configuration support is not implemented
    return Optional.empty();
  }

}
