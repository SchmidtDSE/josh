/**
 * Strategy to write to an individual geotiff.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.File;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.joshsim.engine.geometry.PatchBuilderExtents;


/**
 * Strategy to write a single geotiff.
 *
 * <p>Strategy to write a single band geotiff where there is one geotiff per timestep, variable,
 * and replicate combination. There is one GeotiffWriteStrategy instantiated per output geotiff.
 * This is written via Apache SIS and will convert position.latitude, position.longitude, and
 * the variable specified to doubles from Strings before writing. Note that position.latitude and
 * position.longitude refer to the center of the patch where the patch is the equivalent to the
 * pixel in the geotiff.</p>
 */
public class GeotiffWriteStrategy extends PendingRecordWriteStrategy {

  private final String variable;
  private final PatchBuilderExtents extents;
  private final BigDecimal width;

  /**
   * Constructs a GeotiffWriteStrategy with the specified variable.
   *
   * @param variable The variable to be written within this strategy.
   * @param extents The size of the grid to be written where values are expressed in degrees on
   *     Earth-space.
   * @param width The width and height of each patch to be written (equivalent to each pixel in the
   *     geotiff
   */
  public GeotiffWriteStrategy(String variable, PatchBuilderExtents extents, BigDecimal width) {
    this.variable = variable;
    this.extents = extents;
    this.width = width;
  }

  /**
   *
   * @param records The records to be written.
   * @param outputStream The stream to which they should be written.
   */
  @Override
  protected void writeAll(List<Map<String, String>> records, OutputStream outputStream) {

  }

  @Override
  protected List<String> getRequiredVariables() {
    return List.of("position.latitude", "position.longitude", variable);
  }
}
