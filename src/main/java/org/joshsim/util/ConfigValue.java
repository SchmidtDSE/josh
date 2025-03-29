package org.joshsim.util;

/**
 * Container class for a configuration value with its source.
 */
public class ConfigValue {
  private final String value;
  private final ValueSource source;

  /**
   * Constructs a ConfigValue with the specified value and source.
   *
   * @param value The configuration value.
   * @param source The source of the configuration value.
   */
  public ConfigValue(String value, ValueSource source) {
    this.value = value;
    this.source = source;
  }

  public String getValue() {
    return value;
  }

  public ValueSource getSource() {
    return source;
  }

  @Override
  public String toString() {
    return value + " (from " + source + ")";
  }
}