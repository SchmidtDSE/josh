/**
 * Utility functions to help work with extents.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Utility to help work with extents.
 */
public class ExtentsUtil {

  /**
   * Adds coordinate extents to a PatchBuilderExtentsBuilder.
   *
   * @param builder the PatchBuilderExtentsBuilder to add coordinates to
   * @param target the coordinate string to parse
   * @param start true if these are start coordinates, false if end coordinates
   */
  public static void addExtents(PatchBuilderExtentsBuilder builder, String target, boolean start,
        EngineValueFactory valueFactory) {
    String[] pieces = target.split(",");

    EngineValue value1 = parseExtentComponent(pieces[0], valueFactory);
    EngineValue value2 = parseExtentComponent(pieces[1], valueFactory);
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
  public static EngineValue parseExtentComponent(String target, EngineValueFactory valueFactory) {
    String engineValStr = target.strip().replaceAll(" latitude", "").replaceAll(" longitude", "");
    String[] pieces = engineValStr.split(" ");
    return valueFactory.parseNumber(pieces[0], Units.of(pieces[1]));
  }

}
