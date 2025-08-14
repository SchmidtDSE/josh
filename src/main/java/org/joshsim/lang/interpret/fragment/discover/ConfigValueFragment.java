/**
 * Fragment containing a single configuration value for discovery purposes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.discover;

import java.math.BigDecimal;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * Fragment implementation that holds information about a single config value.
 *
 * <p>This fragment type is used during configuration discovery to hold parsed
 * information about a single config value expression, such as a default value
 * that includes both a number and units.</p>
 */
public class ConfigValueFragment extends ConfigDiscoverabilityFragment {
  private final String name;
  private final BigDecimal number;
  private final String units;
  private final EngineValue engineValue;

  /**
   * Creates a new fragment for a simple name without additional value information.
   *
   * @param name The name string
   * @throws IllegalArgumentException if name is null or empty
   */
  public ConfigValueFragment(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Name cannot be null or empty");
    }
    this.name = name.trim();
    this.number = null;
    this.units = null;
    this.engineValue = null;
  }

  /**
   * Creates a new fragment for a value with number and units.
   *
   * @param number The numeric value
   * @param units The units string
   * @throws IllegalArgumentException if number is null
   */
  public ConfigValueFragment(BigDecimal number, String units) {
    if (number == null) {
      throw new IllegalArgumentException("Number cannot be null");
    }
    this.name = null;
    this.number = number;
    this.units = units != null ? units.trim() : "";
    this.engineValue = null;
  }

  /**
   * Creates a new fragment for an engine value.
   *
   * @param engineValue The engine value
   * @throws IllegalArgumentException if engineValue is null
   */
  public ConfigValueFragment(EngineValue engineValue) {
    if (engineValue == null) {
      throw new IllegalArgumentException("Engine value cannot be null");
    }
    this.name = null;
    this.number = null;
    this.units = null;
    this.engineValue = engineValue;
  }

  /**
   * Gets the name from this fragment.
   *
   * @return The name string
   * @throws RuntimeException if this fragment does not contain a name
   */
  @Override
  public String getName() {
    if (name == null) {
      throw new RuntimeException("This fragment does not have a name.");
    }
    return name;
  }

  /**
   * Gets the number from this fragment.
   *
   * @return The number as BigDecimal
   * @throws RuntimeException if this fragment does not contain a number
   */
  @Override
  public BigDecimal getNumber() {
    if (number == null) {
      throw new RuntimeException("This fragment does not have a number.");
    }
    return number;
  }

  /**
   * Gets the units string from this fragment.
   *
   * @return The units string
   * @throws RuntimeException if this fragment does not contain units
   */
  @Override
  public String getUnits() {
    if (number == null) {
      throw new RuntimeException("This fragment does not have units.");
    }
    return units;
  }

  /**
   * Gets the engine value from this fragment.
   *
   * @return The engine value
   * @throws RuntimeException if this fragment does not contain an engine value
   */
  @Override
  public EngineValue getEngineValue() {
    if (engineValue == null) {
      throw new RuntimeException("This fragment does not have an engine value.");
    }
    return engineValue;
  }

  /**
   * Gets the type of this fragment.
   *
   * @return CONFIG_VALUE fragment type
   */
  @Override
  public FragmentType getFragmentType() {
    return FragmentType.CONFIG_VALUE;
  }
}
