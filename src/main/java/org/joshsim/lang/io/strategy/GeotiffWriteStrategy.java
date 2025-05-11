/**
 * Strategy to write to an individual geotiff.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridCoverage;
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
import org.joshsim.engine.geometry.HaversineUtil;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.storage.geotiff.GeoTiffStoreProvider;
import org.apache.sis.storage.GridCoverageResource;

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
  private final double totalWidthMeters;
  private final double totalHeightMeters;

  /**
   * Constructs a GeotiffWriteStrategy with the specified variable.
   *
   * @param variable The variable to be written within this strategy.
   * @param dimensions The dimensions of the geotiff both in pixel space and in Earth-space.
   */
  public GeotiffWriteStrategy(String variable, GeotiffDimensions dimensions) {
    this.variable = variable;
    this.dimensions = dimensions;

    // Calculate total width and height in meters using Haversine
    HaversineUtil.HaversinePoint west = new HaversineUtil.HaversinePoint(
        BigDecimal.valueOf(dimensions.getMinLon()),
        BigDecimal.valueOf(dimensions.getMinLat())
    );
    HaversineUtil.HaversinePoint east = new HaversineUtil.HaversinePoint(
        BigDecimal.valueOf(dimensions.getMaxLon()),
        BigDecimal.valueOf(dimensions.getMinLat())
    );
    HaversineUtil.HaversinePoint south = new HaversineUtil.HaversinePoint(
        BigDecimal.valueOf(dimensions.getMinLon()),
        BigDecimal.valueOf(dimensions.getMinLat())
    );
    HaversineUtil.HaversinePoint north = new HaversineUtil.HaversinePoint(
        BigDecimal.valueOf(dimensions.getMinLon()),
        BigDecimal.valueOf(dimensions.getMaxLat())
    );

    totalWidthMeters = HaversineUtil.getDistance(west, east).doubleValue();
    totalHeightMeters = HaversineUtil.getDistance(south, north).doubleValue();
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

      // Create raster directly
      WritableRaster raster = Raster.createBandedRaster(
          DataBuffer.TYPE_FLOAT,
          dimensions.getGridWidthPixels(),
          dimensions.getGridHeightPixels(),
          1, // single band
          null
      );

      for (Map<String, String> record : records) {
        double longitude = Double.valueOf(record.get("position.longitude"));
        double latitude = Double.valueOf(record.get("position.latitude"));
        String valueStr = record.get(variable);
        float value = valueStr != null ? Float.parseFloat(valueStr) : Float.NaN;

        // Calculate distances using Haversine
        HaversineUtil.HaversinePoint currentPoint = new HaversineUtil.HaversinePoint(
          BigDecimal.valueOf(longitude),
          BigDecimal.valueOf(latitude)
        );
        HaversineUtil.HaversinePoint westPoint = new HaversineUtil.HaversinePoint(
            BigDecimal.valueOf(dimensions.getMinLon()),
            BigDecimal.valueOf(latitude)
        );
        HaversineUtil.HaversinePoint southPoint = new HaversineUtil.HaversinePoint(
            BigDecimal.valueOf(longitude),
            BigDecimal.valueOf(dimensions.getMinLat())
        );

        double distanceFromWest = HaversineUtil.getDistance(
            westPoint,
            currentPoint
        ).doubleValue();
        double distanceFromSouth = HaversineUtil.getDistance(
            southPoint,
            currentPoint
        ).doubleValue();

        double pixelX = (distanceFromWest / totalWidthMeters) * dimensions.getGridWidthPixels();
        double pixelY = (distanceFromSouth / totalHeightMeters) * dimensions.getGridHeightPixels();

        // Add value to target image
        int x = (int) Math.round(pixelX);
        int y = (int) Math.round(dimensions.getGridHeightPixels() - pixelY);
        raster.setSample(x, y, 0, value);
      }

      // Set the values
      builder.setValues(raster);

      // Write to GeoTIFF using Apache SIS
      GridCoverage coverage = builder.build();
      
      // Create GeoTIFF store and write
      StorageConnector connector = new StorageConnector(tempFile);
      GeoTiffStore store = new GeoTiffStore(null, new GeoTiffStoreProvider(), connector, false);
      GridCoverageResource resource = store.createResource(GridCoverageResource.class);
      resource.write(coverage);
      store.close();

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