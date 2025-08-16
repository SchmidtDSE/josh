/**
 * Fragment containing a single configuration value for discovery purposes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.discover;

import java.math.BigDecimal;
import java.util.Optional;
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
  private final Optional<String> name;
  private final Optional<BigDecimal> number;
  private final Optional<String> units;
  private final Optional<EngineValue> engineValue;

  /**
   * Creates a new fragment for a simple name without additional value information.
   *
   * @param name The name string
   */
  public ConfigValueFragment(String name) {
    this.name = Optional.of(name.trim());
    this.number = Optional.empty();
    this.units = Optional.empty();
    this.engineValue = Optional.empty();
  }

  /**
   * Creates a new fragment for a value with number and units.
   *
   * @param number The numeric value
   * @param units The units string
   */
  public ConfigValueFragment(BigDecimal number, String units) {
    this.name = Optional.empty();
    this.number = Optional.of(number);
    this.units = Optional.of(units != null ? units.trim() : "");
    this.engineValue = Optional.empty();
  }

  /**
   * Creates a new fragment for an engine value.
   *
   * @param engineValue The engine value
   */
  public ConfigValueFragment(EngineValue engineValue) {
    this.name = Optional.empty();
    this.number = Optional.empty();
    this.units = Optional.empty();
    this.engineValue = Optional.of(engineValue);
  }

  /**
   * Gets the name from this fragment.
   *
   * @return The name string
   * @throws RuntimeException if this fragment does not contain a name
   */
  @Override
  public String getName() {
    return name.orElseThrow(() -> new RuntimeException("This fragment does not have a name."));
  }

  /**
   * Gets the number from this fragment.
   *
   * @return The number as BigDecimal
   * @throws RuntimeException if this fragment does not contain a number
   */
  @Override
  public BigDecimal getNumber() {
    return number.orElseThrow(() -> new RuntimeException("This fragment does not have a number."));
  }

  /**
   * Gets the units string from this fragment.
   *
   * @return The units string
   * @throws RuntimeException if this fragment does not contain units
   */
  @Override
  public String getUnits() {
    return units.orElseThrow(() -> new RuntimeException("This fragment does not have units."));
  }

  /**
   * Gets the engine value from this fragment.
   *
   * @return The engine value
   * @throws RuntimeException if this fragment does not contain an engine value
   */
  @Override
  public EngineValue getEngineValue() {
    return engineValue.orElseThrow(
        () -> new RuntimeException("This fragment does not have an engine value."));
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
