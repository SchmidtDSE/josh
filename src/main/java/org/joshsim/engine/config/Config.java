/**
 * Structures to house read-only configuration.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import java.util.Map;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Read-only configuration container.
 *
 * <p>Read-only configuration container which maps from string names to EngineValues.</p>
 */
public class Config {
  private final Map<String, EngineValue> values;

  /**
   * Creates a new Config with the provided values.
   *
   * @param values the configuration values to store
   */
  public Config(Map<String, EngineValue> values) {
    this.values = Map.copyOf(values);
  }

  /**
   * Gets a configuration value by name.
   *
   * @param name the name of the configuration value to retrieve
   * @return the associated engine value
   */
  public EngineValue getValue(String name) {
    return values.get(name);
  }

  /**
   * Checks if a configuration value exists.
   *
   * @param name the name of the configuration value to check
   * @return true if the value exists, false otherwise
   */
  public boolean hasValue(String name) {
    if (name == null) {
      return false;
    }
    return values.containsKey(name);
  }
}
