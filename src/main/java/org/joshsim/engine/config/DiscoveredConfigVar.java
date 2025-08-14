/**
 * Represents a discovered configuration variable with optional default value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable class representing a configuration variable discovered in Josh scripts.
 *
 * <p>This class encapsulates a config variable name and its optional default value.
 * It provides formatted output suitable for discovery commands, showing defaults
 * in parentheses when present (e.g., "testVar1(5m)" or "testVar2").</p>
 */
public final class DiscoveredConfigVar {
  private final String name;
  private final Optional<String> defaultValue;

  /**
   * Creates a discovered config variable without a default value.
   *
   * @param name The name of the config variable
   * @throws IllegalArgumentException if name is null or empty
   */
  public DiscoveredConfigVar(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Config variable name cannot be null or empty");
    }
    this.name = name.trim();
    this.defaultValue = Optional.empty();
  }

  /**
   * Creates a discovered config variable with a default value.
   *
   * @param name The name of the config variable
   * @param defaultValue The default value for the config variable
   * @throws IllegalArgumentException if name is null or empty, or if defaultValue is null
   */
  public DiscoveredConfigVar(String name, String defaultValue) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Config variable name cannot be null or empty");
    }
    if (defaultValue == null) {
      throw new IllegalArgumentException("Default value cannot be null");
    }
    this.name = name.trim();
    this.defaultValue = Optional.of(defaultValue.trim());
  }

  /**
   * Gets the name of the config variable.
   *
   * @return The config variable name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the default value if present.
   *
   * @return Optional containing the default value, or empty if no default
   */
  public Optional<String> getDefaultValue() {
    return defaultValue;
  }

  /**
   * Returns a formatted description of the config variable.
   *
   * <p>Variables with defaults are formatted as "name(default)" while variables
   * without defaults are formatted as just "name".</p>
   *
   * @return Formatted description string
   */
  public String describe() {
    return defaultValue.map(def -> name + "(" + def + ")").orElse(name);
  }

  /**
   * Returns a string representation including the class name prefix.
   *
   * @return String representation for debugging
   */
  @Override
  public String toString() {
    return "DiscoveredConfigVar: " + describe();
  }

  /**
   * Checks equality based on name and default value.
   *
   * @param obj The object to compare with
   * @return true if objects are equal, false otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    DiscoveredConfigVar other = (DiscoveredConfigVar) obj;
    return Objects.equals(name, other.name) && Objects.equals(defaultValue, other.defaultValue);
  }

  /**
   * Returns hash code based on name and default value.
   *
   * @return Hash code for this object
   */
  @Override
  public int hashCode() {
    return Objects.hash(name, defaultValue);
  }
}