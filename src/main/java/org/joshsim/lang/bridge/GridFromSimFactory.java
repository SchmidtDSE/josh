/**
 * Utility to seed a PatchBuilder from a simulation entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;



/**
 * Factory building a PatchSet from a simulation and bridge.
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

    valueFactory = EngineValueFactory.getDefault();
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
   * Builds a PatchSet from a simulation entity using the provided EngineBridge.
   *
   * @param simulation the simulation entity used to build the PatchSet
   * @return the built PatchSet
   */
  public PatchSet build(MutableEntity simulation) {
    simulation.startSubstep("constant");

    final Optional<EngineValue> inputCrsMaybe = simulation.getAttributeValue("grid.inputCrs");
    // final Optional<EngineValue> targetCrsMaybe = simulation.getAttributeValue("grid.targetCrs");
    final Optional<EngineValue> startStrMaybe = simulation.getAttributeValue("grid.low");
    final Optional<EngineValue> endStrMaybe = simulation.getAttributeValue("grid.high");
    final Optional<EngineValue> patchNameMaybe = simulation.getAttributeValue("grid.patch");
    final Optional<EngineValue> sizeMaybe = simulation.getAttributeValue("grid.size");

    simulation.endSubstep();

    String inputCrs = getOrDefault(inputCrsMaybe, "");
    // String targetCrs = getOrDefault(targetCrsMaybe, "");
    String startStr = getOrDefault(startStrMaybe, "1 count latitude, 1 count longitude");
    String endStr = getOrDefault(endStrMaybe, "10 count latitude, 10 count longitude");
    String patchName = getOrDefault(patchNameMaybe, "Default");

    PatchBuilderExtents extents = buildExtents(startStr, endStr);
    EngineValue sizeValueRaw = sizeMaybe.orElse(valueFactory.build(1, Units.COUNT));
    BigDecimal sizeValuePrimitive = sizeValueRaw.getAsDecimal();

    // TODO: properly parse units for cell size
    String sizeValueUnits = "m";

    GridCrsDefinition gridCrsDefinition = new GridCrsDefinition(
        "GRID", inputCrs, extents, sizeValuePrimitive, sizeValueUnits
    );

    PatchBuilder builder = bridge.getGeometryFactory().getPatchBuilder(
        gridCrsDefinition,
        bridge.getPrototype(patchName)
    );
    return builder.build();
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
   * @return PatchBuilderExtents object containing the parsed coordinates
   */
  private PatchBuilderExtents buildExtents(String startStr, String endStr) {
    PatchBuilderExtentsBuilder builder = new PatchBuilderExtentsBuilder();
    addExtents(builder, startStr, true);
    addExtents(builder, endStr, false);
    return builder.build();
  }

  /**
   * Adds coordinate extents to a PatchBuilderExtentsBuilder.
   *
   * @param builder the PatchBuilderExtentsBuilder to add coordinates to
   * @param target the coordinate string to parse
   * @param start true if these are start coordinates, false if end coordinates
   */
  private void addExtents(PatchBuilderExtentsBuilder builder, String target, boolean start) {
    String[] pieces = target.split(",");

    EngineValue value1 = parseExtentComponent(pieces[0]);
    EngineValue value2 = parseExtentComponent(pieces[1]);
    boolean latitudeFirst = pieces[0].contains("latitude");

    EngineValue latitude = latitudeFirst ? value1 : value2;
    EngineValue longitude = latitudeFirst ? value2 : value1;

    if (start) {
      builder.setTopLeftX(longitude.getAsDecimal());
      builder.setTopLeftY(latitude.getAsDecimal());
    } else {
      builder.setBottomRightX(longitude.getAsDecimal());
      builder.setBottomRightY(latitude.getAsDecimal());
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
