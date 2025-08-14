/**
 * Fragment containing a ConfigBuilder for accumulating configuration values.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.jshc;

import org.joshsim.engine.config.ConfigBuilder;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * Fragment representing a configuration builder.
 *
 * <p>This fragment contains a ConfigBuilder instance that accumulates
 * configuration key-value pairs as they are parsed from a .jshc file.</p>
 */
public class JshcConfigBuilderFragment extends JshcFragment {

  private final ConfigBuilder configBuilder;

  /**
   * Creates a new config builder fragment.
   *
   * @param configBuilder The ConfigBuilder instance
   */
  public JshcConfigBuilderFragment(ConfigBuilder configBuilder) {
    this.configBuilder = configBuilder;
  }

  @Override
  public ConfigBuilder getConfigBuilder() {
    return configBuilder;
  }

  @Override
  public FragmentType getFragmentType() {
    // Using PROGRAM as the closest existing type for a builder fragment
    return FragmentType.PROGRAM;
  }
}