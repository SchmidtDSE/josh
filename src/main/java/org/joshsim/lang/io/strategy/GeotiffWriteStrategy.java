/**
 * Strategy to write to an individual geotiff.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.util.j2d.AffineTransform2D;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.WritableGridCoverageResource;
import org.apache.sis.storage.geotiff.GeoTiffStoreProvider;
import org.joshsim.engine.geometry.HaversineUtil;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import java.awt.Dimension;

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
   * Write all pending records to the geotiff.
   *
   * @param records The records to be written.
   * @param outputStream The stream to which they should be written.
   */
  @Override
  protected void writeAll(List<Map<String, String>> records, OutputStream outputStream) {
    try {
      // Create the grid coverage
      GridCoverageBuilder builder = new GridCoverageBuilder();
      setGridInBuilder(builder);

      int bufferSize = dimensions.getGridWidthPixels() * dimensions.getGridHeightPixels() + 1;
      DataBuffer dataBuffer = new DataBufferDouble(bufferSize);
      for (Map<String, String> record : records) {
        double longitude = Double.valueOf(record.get("position.longitude"));
        double latitude = Double.valueOf(record.get("position.latitude"));
        String valueStr = record.get(variable);
        double value = valueStr != null ? Double.parseDouble(valueStr) : Double.NaN;

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

        // Calculate pixel position
        double percentX = distanceFromWest / totalWidthMeters;
        double percentY = distanceFromSouth / totalHeightMeters;
        double pixelX = percentX * dimensions.getGridWidthPixels();
        double pixelY = percentY * dimensions.getGridHeightPixels();

        int x = (int) Math.round(pixelX);
        int y = (int) Math.round(dimensions.getGridHeightPixels() - pixelY - 1);
        int index = y * dimensions.getGridWidthPixels() + x;
        try {
          dataBuffer.setElemDouble(index, value);
        } catch (Exception e) {
          System.out.println("Failed on " + x + ", " + y + " which is " + index);
        }
      }

      Dimension size = new Dimension(
          dimensions.getGridWidthPixels(),
          dimensions.getGridHeightPixels()
      );
      builder.setValues(dataBuffer, size);

      File tempFile = null;
      try {
        tempFile = File.createTempFile("geotiff", ".tif");
        tempFile.deleteOnExit();
      } catch (IOException e) {
        System.err.println("Exception details: " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException("Failed to make temporary file: " + e);
      }

      // Create GeoTIFF store and write
      StorageConnector connector = new StorageConnector(tempFile);
      connector.setOption(OptionKey.OPEN_OPTIONS, new OpenOption[]{StandardOpenOption.WRITE});
      try (DataStore store = new GeoTiffStoreProvider().open(connector)) {
        WritableGridCoverageResource resource = (WritableGridCoverageResource) store.findResource(
            "0"
        );
        resource.write(builder.build());
      } catch (DataStoreException e) {
        System.err.println("Exception details: " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException("Failed to write GeoTIFF: " + e);
      }

      // Copy temp file to output stream
      byte[] buffer = null;
      try {
        buffer = Files.readAllBytes(tempFile.toPath());
        outputStream.write(buffer);
        outputStream.flush();
        tempFile.delete();
      } catch (IOException e) {
        System.err.println("Exception details: " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Exception details: " + e.getMessage());
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

    // Calculate scaling factors from pixel space to geographic space
    double scaleX = (dimensions.getMaxLon() - dimensions.getMinLon()) / dimensions.getGridWidthPixels();
    double scaleY = (dimensions.getMaxLat() - dimensions.getMinLat()) / dimensions.getGridHeightPixels();
    
    // Create affine transform from pixel coordinates to geographic coordinates
    MathTransform transform = new AffineTransform2D(
        scaleX,
        0.0,
        0.0,
        -scaleY,
        dimensions.getMinLon(),
        dimensions.getMaxLat()
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