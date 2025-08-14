/**
 * Base class for fragments used in parsing .jshc configuration files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.jshc;

import java.math.BigDecimal;
import org.joshsim.engine.config.ConfigBuilder;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * Base fragment class for .jshc (Josh configuration) file parsing.
 *
 * <p>This class serves as the base for fragments used in parsing configuration files
 * that contain variable definitions in the format "variableName = value units".</p>
 */
public abstract class JshcFragment {

  /**
   * Gets the numeric value from this fragment.
   *
   * @return The numeric value as BigDecimal
   * @throws RuntimeException if this fragment type does not contain a number
   */
  public BigDecimal getNumber() {
    throw new RuntimeException("This fragment does not have a number.");
  }

  /**
   * Gets the units string from this fragment.
   *
   * @return The units string
   * @throws RuntimeException if this fragment type does not contain units
   */
  public String getUnits() {
    throw new RuntimeException("This fragment does not have units.");
  }

  /**
   * Gets the engine value from this fragment.
   *
   * @return The EngineValue
   * @throws RuntimeException if this fragment type does not contain an EngineValue
   */
  public EngineValue getEngineValue() {
    throw new RuntimeException("This fragment does not have an EngineValue.");
  }

  /**
   * Gets the config builder from this fragment.
   *
   * @return The ConfigBuilder
   * @throws RuntimeException if this fragment type does not contain a ConfigBuilder
   */
  public ConfigBuilder getConfigBuilder() {
    throw new RuntimeException("This fragment does not have a ConfigBuilder.");
  }

  /**
   * Gets the type of this fragment.
   *
   * @return The fragment type
   */
  public abstract FragmentType getFragmentType();

  /**
   * Combines multiple ConfigBuilder instances into a single ConfigBuilder.
   *
   * <p>This static method provides a way to merge configuration builders,
   * similar to the pattern used in JoshFragment. Note: Since ConfigBuilder doesn't
   * expose its internal values, this method creates a new builder and assumes
   * the caller will handle the merging through the visitor pattern.</p>
   *
   * @param first The first ConfigBuilder
   * @param second The second ConfigBuilder
   * @return A new ConfigBuilder for combining (implementation deferred to usage)
   */
  public static ConfigBuilder combineConfigBuilders(ConfigBuilder first, ConfigBuilder second) {
    // Since ConfigBuilder doesn't expose its values, we return a new builder
    // The actual combining will be handled by the visitor when processing
    // multiple assignments in a single config file
    return new ConfigBuilder();
  }
}
