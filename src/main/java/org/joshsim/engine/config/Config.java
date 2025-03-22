/**
 * Structures to house read-only configuration.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.config;

import org.joshsim.engine.value.EngineValue;

/**
 * Read-only configuration container.
 *
 * <p>Read-only configuration container which maps from string names to EngineValues.</p>
 */
public interface Config {
  /**
   * Gets a configuration value by name.
   *
   * @param name the name of the configuration value to retrieve
   * @return the associated engine value
   */
  EngineValue getValue(String name);
}
