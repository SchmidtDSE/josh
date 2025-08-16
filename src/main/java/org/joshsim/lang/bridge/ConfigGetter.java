/**
 * Interface for configuration resource getters.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.Optional;
import org.joshsim.engine.config.Config;


/**
 * Description of strategy for loading configuration resources.
 *
 * <p>Description of strategy for accessing configuration data that can be used as
 * inputs into the simulation.</p>
 */
public interface ConfigGetter {

  /**
   * Load a configuration resource.
   *
   * @param name The name of the configuration resource to be loaded.
   * @return The configuration data, or empty if the resource doesn't exist.
   */
  Optional<Config> getConfig(String name);

}
