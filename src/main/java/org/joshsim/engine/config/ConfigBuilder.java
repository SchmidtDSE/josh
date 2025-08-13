/**
 * Structures to help build Configs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Builder to construct a Config.
 *
 * <p>Builder for creating Config instances, providing methods to add configuration values by name
 * and build the final config.</p>
 */
public class ConfigBuilder {
  private final Map<String, EngineValue> values = new HashMap<>();

  /**
   * Adds a named value to the configuration being built.
   *
   * @param name the name of the configuration value
   * @param value the engine value to associate with the name
   * @return this builder for method chaining
   */
  public ConfigBuilder addValue(String name, EngineValue value) {
    values.put(name, value);
    return this;
  }

  /**
   * Builds and returns a Config based on the added values.
   *
   * @return a new Config instance
   */
  public Config build() {
    return new Config(values);
  }
}
