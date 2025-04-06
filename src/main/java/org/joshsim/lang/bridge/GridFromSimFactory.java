/**
 * Utility to seed a GridBuilder from a simulation entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.Grid;
import org.joshsim.engine.geometry.GridBuilder;
import org.joshsim.engine.geometry.GridBuilderExtents;
import org.joshsim.engine.geometry.GridBuilderExtentsBuilder;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Factory building a Grid from a simulation and bridge.
 */
public class GridFromSimFactory {

  private final EngineBridge bridge;
  private final EngineValueFactory valueFactory;
  /**
   * Constructs a GridFromSimFactory with the specified EngineBridge.
   *
   * @param bridge the EngineBridge used for converting units
   */
  public GridFromSimFactory(EngineBridge bridge) {
    this.bridge = bridge;
    
    valueFactory = new EngineValueFactory();
  }
  
  /**
   * Constructs a GridFromSimFactory with the specified EngineBridge and EngineValueFactory.
   *
   * @param bridge the EngineBridge used for converting units
   * @param valueFactory the EngineValueFactory used to create EngineValue instances
   */
  public GridFromSimFactory(EngineBridge bridge, EngineValueFactory valueFactory) {
    this.bridge = bridge;
    this.valueFactory = valueFactory;
  }

  /**
   * Builds a Grid from a simulation entity using the provided EngineBridge.
   *
   * @param simulation the simulation entity used to build the Grid
   * @param bridge the bridge to use to build the Grid
   * @return the built Grid
   */
  public Grid build(Entity simulation) {
    Optional<EngineValue> inputCrsMaybe = simulation.getAttributeValue("grid.inputCrs");
    Optional<EngineValue> targetCrsMaybe = simulation.getAttributeValue("grid.targetCrs");
    Optional<EngineValue> startStrMaybe = simulation.getAttributeValue("grid.start");
    Optional<EngineValue> endStrMaybe = simulation.getAttributeValue("grid.end");
    Optional<EngineValue> sizeMaybe = simulation.getAttributeValue("grid.size");

    String inputCrs = getOrDefault(inputCrsMaybe, "EPSG:4326");
    String targetCrs = getOrDefault(targetCrsMaybe, "EPSG:4326");
    String startStr = getOrDefault(startStrMaybe, "1 count latitude, 1 count longitude");
    String endStr = getOrDefault(endStrMaybe, "10 count latitude, 10 count longitude");

    EngineValue sizeValueRaw = sizeMaybe.orElse(valueFactory.build(1, Units.COUNT));
    EngineValue sizeValue = convertToExpectedUnits(sizeValueRaw);

    GridBuilderExtents extents = buildExtents(startStr, endStr);
    GridBuilder builder = new GridBuilder(inputCrs, targetCrs, extents, sizeValue.getAsDecimal());

    return builder.build();
  }

  private String getOrDefault(Optional<EngineValue> target, String defaultVal) {
    if (target.isEmpty()) {
      return defaultVal;
    } else {
      return target.get().getAsString();
    }
  }

  private GridBuilderExtents buildExtents(String startStr, String endStr) {
    GridBuilderExtentsBuilder builder = new GridBuilderExtentsBuilder();
    addExtents(builder, startStr, true);
    addExtents(builder, endStr, false);
    return builder.build();
  }

  private void addExtents(GridBuilderExtentsBuilder builder, String target, boolean start) {
    String[] pieces = target.split(",");
    
    EngineValue value1 = parseExtentComponent(pieces[0]);
    EngineValue value2 = parseExtentComponent(pieces[1]);
    boolean latitudeFirst = pieces[0].contains("latitude");

    EngineValue latitude = latitudeFirst ? value1 : value2;
    EngineValue longitude = latitudeFirst ? value2 : value1;

    EngineValue latitudeConverted = convertToExpectedUnits(latitude);
    EngineValue longitudeConverted = convertToExpectedUnits(longitude);

    if (start) {
      builder.setTopLeftX(longitudeConverted.getAsDecimal());
      builder.setTopLeftY(latitudeConverted.getAsDecimal());
    } else {
      builder.setBottomRightX(longitudeConverted.getAsDecimal());
      builder.setBottomRightY(latitudeConverted.getAsDecimal());
    }
  }

  private EngineValue parseExtentComponent(String target) {
    String engineValStr = target.strip().replaceAll(" latitude", "").replaceAll(" longitude", "");
    String[] pieces = engineValStr.split(" ");
    return valueFactory.build(new BigDecimal(pieces[0]), new Units(pieces[1]));
  }

  private EngineValue convertToExpectedUnits(EngineValue target) {
    if (target.getUnits().equals("count")) {
      return target;
    } else {
      return bridge.convert(target, new Units("meters"));
    }
  }
  
}
