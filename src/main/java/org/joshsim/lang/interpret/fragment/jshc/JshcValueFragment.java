/**
 * Fragment containing a parsed configuration value (number + optional units).
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.jshc;

import java.math.BigDecimal;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * Fragment representing a parsed configuration value.
 *
 * <p>This fragment contains the components of a configuration value:
 * a numeric value and optional units string.</p>
 */
public class JshcValueFragment extends JshcFragment {

  private final BigDecimal number;
  private final String units;
  private final EngineValue engineValue;

  /**
   * Creates a new value fragment.
   *
   * @param number The numeric value
   * @param units The units string (may be empty)
   * @param engineValue The constructed EngineValue
   */
  public JshcValueFragment(BigDecimal number, String units, EngineValue engineValue) {
    this.number = number;
    this.units = units;
    this.engineValue = engineValue;
  }

  @Override
  public BigDecimal getNumber() {
    return number;
  }

  @Override
  public String getUnits() {
    return units;
  }

  @Override
  public EngineValue getEngineValue() {
    return engineValue;
  }

  @Override
  public FragmentType getFragmentType() {
    // Using CONVERSION as the closest existing type for a value fragment
    return FragmentType.CONVERSION;
  }
}