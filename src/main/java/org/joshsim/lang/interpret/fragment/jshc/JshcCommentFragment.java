/**
 * Fragment representing a comment or empty line in a .jshc configuration file.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.jshc;

import org.joshsim.engine.config.ConfigBuilder;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * Fragment representing a comment or empty line in a configuration file.
 *
 * <p>This fragment represents non-configuration content (comments and empty lines)
 * in .jshc files. It provides an empty ConfigBuilder to maintain consistency
 * with the visitor pattern while clearly indicating the fragment type.</p>
 */
public class JshcCommentFragment extends JshcFragment {

  private static final ConfigBuilder EMPTY_BUILDER = new ConfigBuilder();

  /**
   * Creates a new comment fragment.
   */
  public JshcCommentFragment() {
    // Comments and empty lines don't need any state
  }

  @Override
  public ConfigBuilder getConfigBuilder() {
    return EMPTY_BUILDER;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.COMMENT;
  }
}