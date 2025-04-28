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


public class GridInfoExtractor {

  private final Optional<EngineValue> inputCrsMaybe;
  private final Optional<EngineValue> targetCrsMaybe;
  private final Optional<EngineValue> startStrMaybe;
  private final Optional<EngineValue> endStrMaybe;
  private final Optional<EngineValue> patchNameMaybe;
  private final Optional<EngineValue> sizeMaybe;
  private final EngineValueFactory valueFactory;

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

  public Optional<EngineValue> getInputCrsMaybe() {
    return inputCrsMaybe;
  }

  public Optional<EngineValue> getTargetCrsMaybe() {
    return targetCrsMaybe;
  }

  public Optional<EngineValue> getStartStrMaybe() {
    return startStrMaybe;
  }

  public Optional<EngineValue> getEndStrMaybe() {
    return endStrMaybe;
  }

  public Optional<EngineValue> getPatchNameMaybe() {
    return patchNameMaybe;
  }

  public Optional<EngineValue> getSizeMaybe() {
    return sizeMaybe;
  }

  public String getInputCrs() {
    return getOrDefault(getInputCrsMaybe(), "");
  }

  public String getTargetCrs() {
    return getOrDefault(getTargetCrsMaybe(), "");
  }

  public String getStartStr() {
    return getOrDefault(getStartStrMaybe(), "1 count latitude, 1 count longitude");
  }

  public String getEndStr() {
    return getOrDefault(getEndStrMaybe(), "10 count latitude, 10 count longitude");
  }

  public String getPatchName() {
    return getOrDefault(getPatchNameMaybe(), "Default");
  }

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
