
/**
 * Fragment containing a single unit conversion.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment;

import org.joshsim.engine.value.Conversion;

/**
 * Fragment representing a single unit conversion operation.
 *
 * <p>This class wraps a single Conversion object that defines how to convert between units.</p>
 */
public class ConversionFragment extends Fragment {

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
