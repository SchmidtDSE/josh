/**
 * Strategy to write to an individual geotiff.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.referencing.util.j2d.AffineTransform2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.PixelInCell;


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
  private final GeotiffDimensions dimensions;

  /**
   * Constructs a GeotiffWriteStrategy with the specified variable.
   *
   * @param variable The variable to be written within this strategy.
   * @param dimensions The dimensions of the geotiff both in pixel space and in Earth-space.
   */
  public GeotiffWriteStrategy(String variable, GeotiffDimensions dimensions) {
    this.variable = variable;
    this.dimensions = dimensions;
  }

  /**
   *
   * @param records The records to be written.
   * @param outputStream The stream to which they should be written.
   */
  @Override
  protected void writeAll(List<Map<String, String>> records, OutputStream outputStream) {
    try {
      // Create temporary file
      File tempFile = File.createTempFile("geotiff", ".tif");

      // Create the grid coverage
      GridCoverageBuilder builder = new GridCoverageBuilder();
      setGridInBuilder(builder);

      // Fill grid with values
      RenderedImage targetImage = null;  // TODO
      for (Map<String, String> record : records) {
        double longitude = Double.parseDouble(record.get("position.longitude"));
        double latitude = Double.parseDouble(record.get("position.latitude"));
        String valueStr = record.get(variable);
        float value = valueStr != null ? Float.parseFloat(valueStr) : Float.NaN;
        // TODO - add value to target image using HaversineUtil if needed
      }

      // Set the values
      builder.setValues(targetImage);

      // TODO - Write to GeoTIFF

      // Copy temp file to output stream
      byte[] buffer = Files.readAllBytes(tempFile.toPath());
      outputStream.write(buffer);
      outputStream.flush();

      // Cleanup
      tempFile.delete();

    } catch (IOException e) {
      throw new RuntimeException("Failed to write GeoTIFF: " + e);
    }
  }

  /**
   * Establish the Earth-space location and size of this geotiff.
   *
   * <p>Establish the Earth-space location and size of this geotiff using values avialable on the
   * GeotiffDimensions object in dimensions.</p>
   *
   * @param builder The bulid in which to specify the grid.
   */
  private void setGridInBuilder(GridCoverageBuilder builder) {
    DirectPosition lower = new DirectPosition2D(dimensions.getMinLon(), dimensions.getMinLat());
    DirectPosition upper = new DirectPosition2D(dimensions.getMaxLon(), dimensions.getMaxLat());

    GridGeometry gridGeometry = new GridGeometry(
        new GeneralEnvelope(lower, upper),
        PixelInCell.CELL_CENTER,
        new AffineTransform2D(
            (dimensions.getMaxLon() - dimensions.getMinLon()) / dimensions.getGridWidthPixels(),
            0.0,
            0.0,
            (dimensions.getMaxLat() - dimensions.getMinLat()) / dimensions.getGridHeightPixels(),
            dimensions.getMinLon(),
            dimensions.getMinLat()
        ),
        CommonCRS.WGS84.geographic()
    );
    
    builder.setDomain(gridGeometry);
  }

  @Override
  protected List<String> getRequiredVariables() {
    return List.of("position.latitude", "position.longitude", variable);
  }

}
