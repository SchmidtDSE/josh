/**
 * Fragment containing a set of discovered configuration variables.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.discover;

import java.util.Set;
import org.joshsim.engine.config.DiscoveredConfigVar;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * Fragment implementation that holds a set of discovered configuration variables.
 *
 * <p>This fragment type is used to aggregate multiple discovered config variables
 * during the discovery process, typically as the result of visiting multiple
 * config expressions in a Josh script.</p>
 */
public class ConfigVarSetFragment extends ConfigDiscoverabilityFragment {
  private final Set<DiscoveredConfigVar> discoveredVars;

  /**
   * Creates a new fragment containing the specified set of discovered config variables.
   *
   * @param discoveredVars The set of discovered config variables
   */
  public ConfigVarSetFragment(Set<DiscoveredConfigVar> discoveredVars) {
    this.discoveredVars = discoveredVars;
  }

  @Override
  public Set<DiscoveredConfigVar> getDiscoveredConfigVars() {
    return discoveredVars;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.CONFIG_VAR_SET;
  }
}
