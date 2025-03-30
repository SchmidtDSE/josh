
/**
 * Fragment representing a complete program.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment;

/**
 * Fragment representing a potentially completed program.
 *
 * <p>This class represents a fragment that contains a complete program that can be
 * executed in the simulation environment.</p>
 */
public class ProgramFragment extends Fragment {

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.PROGRAM;
  }

}
