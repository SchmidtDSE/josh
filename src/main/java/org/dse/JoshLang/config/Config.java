/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.config;

import org.dse.JoshLang.core.value.EngineValue;

/**
 * Interface representing a configuration container.
 * Provides methods to retrieve configuration values by name.
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