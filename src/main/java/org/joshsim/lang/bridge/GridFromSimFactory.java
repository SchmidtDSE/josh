/**
 * Utility to seed a GridBuilder from a simulation entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.Grid;
import org.joshsim.engine.geometry.GridBuilder;
import org.joshsim.engine.geometry.GridBuilderExtents;
import org.joshsim.engine.geometry.GridBuilderExtentsBuilder;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


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
   * @return the built Grid
   */
  public Grid build(MutableEntity simulation) {
    simulation.startSubstep("constant");

    Optional<EngineValue> inputCrsMaybe = simulation.getAttributeValue("grid.inputCrs");
    Optional<EngineValue> targetCrsMaybe = simulation.getAttributeValue("grid.targetCrs");
    Optional<EngineValue> startStrMaybe = simulation.getAttributeValue("grid.low");
    Optional<EngineValue> endStrMaybe = simulation.getAttributeValue("grid.high");
    Optional<EngineValue> sizeMaybe = simulation.getAttributeValue("grid.size");
    Optional<EngineValue> patchNameMaybe = simulation.getAttributeValue("grid.patch");

    simulation.endSubstep();

    String inputCrs = getOrDefault(inputCrsMaybe, "EPSG:4326");
    String targetCrs = getOrDefault(targetCrsMaybe, "EPSG:4326");
    String startStr = getOrDefault(startStrMaybe, "1 count latitude, 1 count longitude");
    String endStr = getOrDefault(endStrMaybe, "10 count latitude, 10 count longitude");
    String patchName = getOrDefault(patchNameMaybe, "Default");

    EngineValue sizeValueRaw = sizeMaybe.orElse(valueFactory.build(1, Units.COUNT));
    EngineValue sizeValue = convertToExpectedUnits(sizeValueRaw, Units.METERS);

    GridBuilderExtents extents = buildExtents(startStr, endStr);
    try {
      GridBuilder builder = new GridBuilder(
          inputCrs,
          targetCrs,
          extents,
          sizeValue.getAsDecimal(),
          bridge.getPrototype(patchName)
      );

      return builder.build();
    } catch (FactoryException | TransformException e) {
      throw new RuntimeException("Error instantiating grid from script: " + e);
    }
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

  /**
   * Builds grid extents from start and end coordinate strings.
   *
   * @param startStr the starting coordinate string in format "X latitude, Y longitude"
   * @param endStr the ending coordinate string in format "X latitude, Y longitude"
   * @return GridBuilderExtents object containing the parsed coordinates
   */
  private GridBuilderExtents buildExtents(String startStr, String endStr) {
    GridBuilderExtentsBuilder builder = new GridBuilderExtentsBuilder();
    addExtents(builder, startStr, true);
    addExtents(builder, endStr, false);
    return builder.build();
  }

  /**
   * Adds coordinate extents to a GridBuilderExtentsBuilder.
   *
   * @param builder the GridBuilderExtentsBuilder to add coordinates to
   * @param target the coordinate string to parse
   * @param start true if these are start coordinates, false if end coordinates
   */
  private void addExtents(GridBuilderExtentsBuilder builder, String target, boolean start) {
    String[] pieces = target.split(",");

    EngineValue value1 = parseExtentComponent(pieces[0]);
    EngineValue value2 = parseExtentComponent(pieces[1]);
    boolean latitudeFirst = pieces[0].contains("latitude");

    EngineValue latitude = latitudeFirst ? value1 : value2;
    EngineValue longitude = latitudeFirst ? value2 : value1;

    EngineValue latitudeConverted = convertToExpectedUnits(latitude, Units.DEGREES);
    EngineValue longitudeConverted = convertToExpectedUnits(longitude, Units.DEGREES);

    boolean reversed = value1.getUnits().equals(Units.COUNT);

    if (start) {
      builder.setTopLeftX(longitudeConverted.getAsDecimal());

      if (reversed) {
        builder.setBottomRightY(latitudeConverted.getAsDecimal());
      } else {
        builder.setTopLeftY(latitudeConverted.getAsDecimal());
      }
    } else {
      builder.setBottomRightX(longitudeConverted.getAsDecimal());

      if (reversed) {
        builder.setTopLeftY(latitudeConverted.getAsDecimal());
      } else {
        builder.setBottomRightY(latitudeConverted.getAsDecimal());
      }

    }
  }

  /**
   * Parses a coordinate component string into an EngineValue.
   *
   * @param target the coordinate component string in format "X latitude/longitude"
   * @return EngineValue containing the parsed value and units
   */
  private EngineValue parseExtentComponent(String target) {
    String engineValStr = target.strip().replaceAll(" latitude", "").replaceAll(" longitude", "");
    String[] pieces = engineValStr.split(" ");
    return valueFactory.build(new BigDecimal(pieces[0]), new Units(pieces[1]));
  }

  /**
   * Converts an EngineValue to expected units (meters) if not already in count units.
   *
   * @param target the EngineValue to potentially convert
   * @param allowed The type of units other than count which is allowed.
   * @return the original EngineValue if in count units, otherwise converted to meters
   */
  private EngineValue convertToExpectedUnits(EngineValue target, Units allowed) {
    Units targetUnits = target.getUnits();
    if (targetUnits.equals(Units.COUNT) || targetUnits.equals(allowed)) {
      return target;
    } else {
      return bridge.convert(target, allowed);
    }
  }

}
