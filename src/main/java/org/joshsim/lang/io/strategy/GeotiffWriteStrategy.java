/**
 * Strategy to write to an individual geotiff.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.referencing.util.j2d.AffineTransform2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;


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
  private final double lonExtents;
  private final double latExtents;

  /**
   * Constructs a GeotiffWriteStrategy with the specified variable.
   *
   * @param variable The variable to be written within this strategy.
   * @param dimensions The dimensions of the geotiff both in pixel space and in Earth-space.
   */
  public GeotiffWriteStrategy(String variable, GeotiffDimensions dimensions) {
    this.variable = variable;
    this.dimensions = dimensions;
    lonExtents = dimensions.getMaxLon() - dimensions.getMinLon();
    latExtents = dimensions.getMaxLat() - dimensions.getMinLat();
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
      BufferedImage targetImage = new BufferedImage(
          dimensions.getGridWidthPixels(),
          dimensions.getGridHeightPixels(),
          BufferedImage.TYPE_FLOAT
      );
      WritableRaster raster = targetImage.getRaster();

      for (Map<String, String> record : records) {
        double longitude = Double.valueOf(record.get("position.longitude"));
        double latitude = Double.valueOf(record.get("position.latitude"));
        String valueStr = record.get(variable);
        float value = valueStr != null ? Float.parseFloat(valueStr) : Float.NaN;

        // Calculate pixel coordinates using HaversineUtil and dimensions
        double lonDistance = longitude - dimensions.getMinLon();
        double latDistance = latitude - dimensions.getMinLat();
        double pixelX = lonDistance / lonExtents * dimensions.getGridWidthPixels();
        double pixelY = latDistance / latExtents * dimensions.getGridHeightPixels();

        // Add value to target image
        int x = (int) Math.round(pixelX);
        int y = (int) Math.round(dimensions.getGridHeightPixels() - pixelY);
        raster.setSample(x, y, 0, value);
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
    GridExtent extent = new GridExtent(
        dimensions.getGridWidthPixels(),
        dimensions.getGridHeightPixels()
    );

    MathTransform transform = new AffineTransform2D(
        (dimensions.getMaxLon() - dimensions.getMinLon()) / dimensions.getGridWidthPixels(),
        0.0,
        0.0,
        (dimensions.getMaxLat() - dimensions.getMinLat()) / dimensions.getGridHeightPixels(),
        dimensions.getMinLon(),
        dimensions.getMinLat()
    );

    GridGeometry gridGeometry = new GridGeometry(
        extent,
        PixelInCell.CELL_CENTER,
        transform,
        CommonCRS.WGS84.geographic()
    );

    builder.setDomain(gridGeometry);
  }

  @Override
  protected List<String> getRequiredVariables() {
    return List.of("position.latitude", "position.longitude", variable);
  }

}