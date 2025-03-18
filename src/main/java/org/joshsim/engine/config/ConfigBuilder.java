/**
 * Sturctures to help build Configs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import org.joshsim.engine.value.EngineValue;


/**
 * Builder to construct a Config.
 * 
 * <p>Builder for creating Config instances, providiong methods to add configuration values by name
 * and build the final config.</p>
 */
public interface ConfigBuilder {
  /**
   * Adds a named value to the configuration being built.
   *
   * @param name the name of the configuration value
   * @param value the engine value to associate with the name
   * @return this builder for method chaining
   */
  ConfigBuilder addValue(String name, EngineValue value);
  
  /**
   * Builds and returns a Config based on the added values.
   *
   * @return a new Config instance
   */
  Config build();
}
