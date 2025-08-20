/**
 * JoshFragment containing a single unit conversion.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.josh;

import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * JoshFragment representing a single unit conversion operation.
 *
 * <p>This class wraps a single Conversion object that defines how to convert between units.</p>
 */
public class ConversionFragment extends JoshFragment {

  private final Conversion conversion;

  /**
   * Creates a new fragment around a unit conversion.
   *
   * @param conversion The conversion operation to wrap
   */
  public ConversionFragment(Conversion conversion) {
    this.conversion = conversion;
  }

  public Conversion getConversion() {
    return conversion;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.CONVERSION;
  }

}
