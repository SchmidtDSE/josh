/**
 * Utility to seed a PatchBuilder from a simulation entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.math.BigDecimal;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.ExtentsUtil;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.precompute.ExtentsTransformer;


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
    return build(simulation, inputCrs);
  }

  /**
   * Builds a PatchSet from a simulation entity using the provided EngineBridge.
   *
   * @param simulation the simulation entity used to build the PatchSet
   * @param inputCrs code of CRS to use in parsing input dataset.
   * @return the built PatchSet
   */
  public PatchSet build(MutableEntity simulation, String inputCrs) {
    GridInfoExtractor extractor = new GridInfoExtractor(simulation, valueFactory);
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
        sizeUnits.equals("m")
        || sizeUnits.equals("meter")
        || sizeUnits.equals("meters")
    );
    boolean posSizeMismatch = posDegrees && sizeMeters;
    boolean supportsEarthSpace = geometryFactory.supportsEarthSpace();
    boolean requiresCountConversion = posSizeMismatch && !supportsEarthSpace;
    if (requiresCountConversion) {
      extents = ExtentsTransformer.transformToGrid(extents, sizeValuePrimitive);
      sizeValuePrimitive = BigDecimal.valueOf(1);
    }

    String sizeValueUnits = "m";

    GridCrsDefinition gridCrsDefinition = new GridCrsDefinition(
        inputCrs,
        inputCrs,
        extents,
        sizeValuePrimitive,
        sizeValueUnits
    );

    PatchBuilder builder = geometryFactory.getPatchBuilder(
        gridCrsDefinition,
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
  public PatchBuilderExtents buildExtents(String startStr, String endStr) {
    PatchBuilderExtentsBuilder builder = new PatchBuilderExtentsBuilder();
    ExtentsUtil.addExtents(builder, startStr, true, valueFactory);
    ExtentsUtil.addExtents(builder, endStr, false, valueFactory);
    return builder.build();
  }

}
