/**
 * JoshFragment representing a complete program.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.josh;

import java.util.Optional;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.interpret.fragment.FragmentType;
import org.joshsim.lang.interpret.fragment.ProgramBuilder;

/**
 * JoshFragment representing a potentially completed program.
 *
 * <p>This class represents a fragment that contains a complete program that can be
 * executed in the simulation environment.</p>
 */
public class ProgramFragment extends JoshFragment {

  private final ProgramBuilder builder;
  private Optional<JoshProgram> builtProgram;

  /**
   * Create a fragment which builds programs.
   *
   * @param builder The builder to decorate wuch that this can be used for building programs.
   */
  public ProgramFragment(ProgramBuilder builder) {
    this.builder = builder;
    builtProgram = Optional.empty();
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.PROGRAM;
  }

  @Override
  public JoshProgram getProgram() {
    if (builtProgram.isEmpty()) {
      builtProgram = Optional.of(builder.build());
    }

    return builtProgram.get();
  }

}
