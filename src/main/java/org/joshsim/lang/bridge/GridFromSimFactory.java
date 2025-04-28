/**
 * Utility to seed a PatchBuilder from a simulation entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.math.BigDecimal;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.geometry.PatchSet;
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

    GridInfoExtractor extractor = new GridInfoExtractor(simulation, valueFactory);
    String inputCrs = extractor.getInputCrs();
    String targetCrs = extractor.getTargetCrs();
    String startStr = extractor.getStartStr();
    String endStr = extractor.getEndStr();
    String patchName = extractor.getPatchName();

    PatchBuilderExtents extents = buildExtents(startStr, endStr);
    EngineValue sizeValueRaw = extractor.getSize();
    BigDecimal sizeValuePrimitive = sizeValueRaw.getAsDecimal();

    EngineGeometryFactory geometryFactory = bridge.getGeometryFactory();

    String sizeUnits = sizeValueRaw.getUnits().toString();
    boolean posDegrees = startStr.contains("degrees");
    boolean sizeMeters = (
        sizeUnits.equals("m") ||
        sizeUnits.equals("meter") ||
        sizeUnits.equals("meters")
    );
    boolean posSizeMismatch = posDegrees && sizeMeters;
    boolean supportsEarthSpace = geometryFactory.supportsEarthSpace();
    boolean requiresCountConversion = posSizeMismatch && !supportsEarthSpace;
    if (requiresCountConversion) {
      extents = convertToMeters(extents, sizeValuePrimitive);
      sizeValuePrimitive = BigDecimal.valueOf(1);
    }

    PatchBuilder builder = geometryFactory.getPatchBuilder(
        inputCrs,
        targetCrs,
        extents,
        sizeValuePrimitive,
        bridge.getPrototype(patchName)
    );
    return builder.build();
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

  /**
   * Convert a set of extents from degrees to meters.
   *
   * <p>Convert a set of extents from degrees to coordinates expressed in cell / patch counts via
   * conversion to meters using Haverzine where the upper left corner is 0, 0 and the bottom right
   * is positive. This is done using HaversineUtil.</p>
   *
   * @param extents Original extents expressed in degrees which should be converted to meters and
   *     then cell counts.
   * @param sizeMeters Size of each cell / patch in meters where each patch is a square.
   */
  private PatchBuilderExtents convertToMeters(PatchBuilderExtents extents, BigDecimal sizeMeters) {
    
  }

}
