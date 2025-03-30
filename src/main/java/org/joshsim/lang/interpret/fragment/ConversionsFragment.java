
/**
 * Fragment containing multiple unit conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment;

import java.util.List;
import org.joshsim.engine.value.converter.Conversion;

/**
 * Fragment representing a collection of unit conversions.
 *
 * <p>This class maintains an iterable collection of Conversion objects that define
 * how to convert between different units.</p>
 */
public class ConversionsFragment extends Fragment {

  private final Iterable<Conversion> conversions;

  /**
   * Creates a new fragment containing multiple conversions.
   *
   * @param conversions The collection of conversions to store
   */
  public ConversionsFragment(Iterable<Conversion> conversions) {
    this.conversions = conversions;
  }

  public Iterable<Conversion> getConversions() {
    return conversions;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.CONVERSIONS;
  }

}
