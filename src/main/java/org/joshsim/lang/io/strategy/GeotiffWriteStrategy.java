/**
 * Strategy to write to geotiffs via netcdf-java.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


/**
 * Strategy for writing to an individiual geotiff.
 *
 * <p>A strategy to write records into a GeoTIFF format. This class processes records containing
 * geographical and variable data, and writes them as a rasterized GeoTIFF file with defined
 * dimensions. This is used once per individual geotifff output where Josh only writes one variable
 * per time step per replicate per geotiff.</p>
 */
public class GeotiffWriteStrategy extends PendingRecordWriteStrategy {
  private final String variable;
  private final GeotiffDimensions dimensions;

  /**
   * Constructs a new strategy for writing geotiff data.
   *
   * @param variable The variable name or identifier associated with this geotiff export strategy.
   * @param dimensions An instance of GeotiffDimensions containing the spatial and resolution
   *     parameters for the geotiff file.
   */
  public GeotiffWriteStrategy(String variable, GeotiffDimensions dimensions) {
    this.variable = variable;
    this.dimensions = dimensions;
  }

  /**
   * Write all of the records to the given output stream.
   *
   * @param records The records to be written where each element of the records list corresponds to
   *     a pixel. This will have properties position.longitude and position.latitude as well as a
   *     value given by the attribute "variable" on this object. All should be converted to double.
   * @param outputStream The stream to which to write this single geotiff.
   */
  @Override
  protected void writeAll(List<Map<String, String>> records, OutputStream outputStream) {
    
  }

  @Override
  protected List<String> getRequiredVariables() {
    return List.of("position.latitude", "position.longitude", variable);
  }
}