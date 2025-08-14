/**
 * Base class for fragments used in configuration variable discovery.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.discover;

import java.math.BigDecimal;
import java.util.Set;
import org.joshsim.engine.config.DiscoveredConfigVar;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * Abstract base class for configuration discovery fragments.
 *
 * <p>This fragment type is used during configuration variable discovery to collect
 * and manage information about config variables found in Josh scripts, including
 * their names and optional default values.</p>
 */
public abstract class ConfigDiscoverabilityFragment {

  /**
   * Gets the name from this fragment if it represents a single named value.
   *
   * @return The name string
   * @throws RuntimeException if this fragment type does not contain a name
   */
  public String getName() {
    throw new RuntimeException("This fragment does not have a name.");
  }

  /**
   * Gets the number from this fragment if it represents a numeric value.
   *
   * @return The number as BigDecimal
   * @throws RuntimeException if this fragment type does not contain a number
   */
  public BigDecimal getNumber() {
    throw new RuntimeException("This fragment does not have a number.");
  }

  /**
   * Gets the units string from this fragment if it represents a value with units.
   *
   * @return The units string
   * @throws RuntimeException if this fragment type does not contain units
   */
  public String getUnits() {
    throw new RuntimeException("This fragment does not have units.");
  }

  /**
   * Gets the engine value from this fragment if it represents an engine value.
   *
   * @return The engine value
   * @throws RuntimeException if this fragment type does not contain an engine value
   */
  public EngineValue getEngineValue() {
    throw new RuntimeException("This fragment does not have an engine value.");
  }

  /**
   * Gets the set of discovered config variables from this fragment.
   *
   * @return Set of discovered config variables
   * @throws RuntimeException if this fragment type does not contain discovered variables
   */
  public Set<DiscoveredConfigVar> getDiscoveredConfigVars() {
    throw new RuntimeException("This fragment does not have discovered config variables.");
  }

  /**
   * Gets the type of this fragment.
   *
   * @return The fragment type
   */
  public abstract FragmentType getFragmentType();
}