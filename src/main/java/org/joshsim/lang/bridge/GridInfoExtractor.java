
/**
 * Logic for gathering information from a simulation definition needed to construct a grid.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Extractor of grid-related information from simulation configurations.
 *
 * <p>Utility to extract metadata from simulations required for constructing a grid including the
 * coordinate reference systems, grid boundaries, patch names, and grid size information.</p>
 */
public class GridInfoExtractor {

  private final Optional<EngineValue> inputCrsMaybe;
  private final Optional<EngineValue> targetCrsMaybe;
  private final Optional<EngineValue> startStrMaybe;
  private final Optional<EngineValue> endStrMaybe;
  private final Optional<EngineValue> patchNameMaybe;
  private final Optional<EngineValue> sizeMaybe;
  private final EngineValueFactory valueFactory;

  /**
   * Creates a new GridInfoExtractor by extracting grid information from a simulation.
   *
   * @param simulation The mutable entity containing simulation configuration
   * @param valueFactory Factory for creating engine values
   */
  public GridInfoExtractor(MutableEntity simulation, EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;

    simulation.startSubstep("constant");

    inputCrsMaybe = simulation.getAttributeValue("grid.inputCrs");
    targetCrsMaybe = simulation.getAttributeValue("grid.targetCrs");
    startStrMaybe = simulation.getAttributeValue("grid.low");
    endStrMaybe = simulation.getAttributeValue("grid.high");
    patchNameMaybe = simulation.getAttributeValue("grid.patch");
    sizeMaybe = simulation.getAttributeValue("grid.size");

    simulation.endSubstep();
  }

  /**
   * Gets the optional input coordinate reference system.
   *
   * @return Optional containing the input CRS if defined
   */
  public Optional<EngineValue> getInputCrsMaybe() {
    return inputCrsMaybe;
  }

  /**
   * Gets the optional target coordinate reference system.
   *
   * @return Optional containing the target CRS if defined
   */
  public Optional<EngineValue> getTargetCrsMaybe() {
    return targetCrsMaybe;
  }

  /**
   * Gets the optional starting coordinates of the grid.
   *
   * @return Optional containing the starting point if defined
   */
  public Optional<EngineValue> getStartStrMaybe() {
    return startStrMaybe;
  }

  /**
   * Gets the optional ending coordinates of the grid.
   *
   * @return Optional containing the ending point if defined
   */
  public Optional<EngineValue> getEndStrMaybe() {
    return endStrMaybe;
  }

  /**
   * Gets the optional patch name for the grid.
   *
   * @return Optional containing the patch name if defined
   */
  public Optional<EngineValue> getPatchNameMaybe() {
    return patchNameMaybe;
  }

  /**
   * Gets the optional size of the grid.
   *
   * @return Optional containing the grid size if defined
   */
  public Optional<EngineValue> getSizeMaybe() {
    return sizeMaybe;
  }

  /**
   * Gets the input coordinate reference system or default empty string.
   *
   * @return The input CRS string or empty string if not defined
   */
  public String getInputCrs() {
    return getOrDefault(getInputCrsMaybe(), "");
  }

  /**
   * Gets the target coordinate reference system or default empty string.
   *
   * @return The target CRS string or empty string if not defined
   */
  public String getTargetCrs() {
    return getOrDefault(getTargetCrsMaybe(), "");
  }

  /**
   * Gets the starting coordinates string or default value.
   *
   * @return The starting coordinates string or default value if not defined
   */
  public String getStartStr() {
    return getOrDefault(getStartStrMaybe(), "1 count latitude, 1 count longitude");
  }

  /**
   * Gets the ending coordinates string or default value.
   *
   * @return The ending coordinates string or default value if not defined
   */
  public String getEndStr() {
    return getOrDefault(getEndStrMaybe(), "10 count latitude, 10 count longitude");
  }

  /**
   * Gets the patch name or default value.
   *
   * @return The patch name or "Default" if not defined
   */
  public String getPatchName() {
    return getOrDefault(getPatchNameMaybe(), "Default");
  }

  /**
   * Gets the grid size or default value of 1 count.
   *
   * @return The grid size or default EngineValue of 1 count if not defined
   */
  public EngineValue getSize() {
    return getSizeMaybe().orElse(valueFactory.build(1, Units.COUNT));
  }

  /**
   * Returns the string value of an optional EngineValue or a default value if empty.
   *
   * @param target the optional EngineValue to check
   * @param defaultVal the default value to return if target is empty
   * @return the string value from the EngineValue or the default value
   */
  private String getOrDefault(Optional<EngineValue> target, String defaultVal) {
    if (target.isEmpty()) {
      return defaultVal;
    } else {
      return target.get().getAsString();
    }
  }

}
