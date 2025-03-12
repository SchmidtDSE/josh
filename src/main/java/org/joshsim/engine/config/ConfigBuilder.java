/**
 * @license BSD-3-Clause
 */
package org.joshsim.engine.config;

import org.joshsim.engine.value.EngineValue;


/**
 * Builder interface for creating Config instances.
 * Provides methods to add configuration values and build the final config.
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