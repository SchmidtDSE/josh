/**
 * Strategy to write to geotiffs via netcdf-java.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joshsim.engine.geometry.HaversineUtil;


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
  private final HaversineUtil.HaversinePoint topLeft;
  private final BigDecimal gridWidthPixels;
  private final BigDecimal gridHeightPixels;
  private final BigDecimal patchWidth;

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

    topLeft = new HaversineUtil.HaversinePoint(
        BigDecimal.valueOf(dimensions.getMinLon()),
        BigDecimal.valueOf(dimensions.getMaxLat())
    );

    gridWidthPixels = BigDecimal.valueOf(dimensions.getGridWidthPixels());
    gridHeightPixels = BigDecimal.valueOf(dimensions.getGridHeightPixels());
    patchWidth = BigDecimal.valueOf(dimensions.getPatchWidthInMeters());
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
    try {
      // Create a grid coverage factory
      final GridCoverageFactory gcf = new GridCoverageFactory();

      // Create the grid for our data
      int effectiveHeight = dimensions.getGridHeightPixels() + 1;
      int effectiveWidth = dimensions.getGridWidthPixels() + 1;
      float[][] data = new float[effectiveHeight][effectiveWidth];

      // Initialize with NaN
      for (int y = 0; y <= dimensions.getGridHeightPixels(); y++) {
        for (int x = 0; x <= dimensions.getGridWidthPixels(); x++) {
          data[y][x] = Float.NaN;
        }
      }

      // Fill the grid with our data
      for (Map<String, String> record : records) {
        String lonStr = record.get("position.longitude");
        String latStr = record.get("position.latitude");
        String valStr = record.get(variable);

        // Skip records with missing coordinates or values
        if (lonStr == null || latStr == null || valStr == null) {
          continue;
        }

        BigDecimal lon = new BigDecimal(lonStr);
        BigDecimal lat = new BigDecimal(latStr);
        double value = Double.parseDouble(valStr);

        // Calculate position using Haversine distances
        HaversineUtil.HaversinePoint currentPoint = new HaversineUtil.HaversinePoint(
            lon,
            lat
        );

        // Get distances from top-left corner
        BigDecimal distanceWest = HaversineUtil.getDistance(
            topLeft,
            new HaversineUtil.HaversinePoint(lon, BigDecimal.valueOf(dimensions.getMaxLat()))
        );
        BigDecimal distanceSouth = HaversineUtil.getDistance(
            topLeft,
            new HaversineUtil.HaversinePoint(BigDecimal.valueOf(dimensions.getMinLon()), lat)
        );

        // Calculate grid position using percentages
        BigDecimal horizPercent = distanceWest.divide(
            dimensions.getGridWidthMeters(),
            RoundingMode.HALF_UP
        );
        int x = (int) Math.round(horizPercent.multiply(gridWidthPixels).doubleValue());

        BigDecimal vertPercent = distanceSouth.divide(
            dimensions.getGridHeightMeters(),
            RoundingMode.HALF_UP
        );
        int y = (int) Math.round(vertPercent.multiply(gridHeightPixels).longValue());

        // Check bounds
        boolean horizInBounds = x >= 0 && x <= dimensions.getGridWidthPixels();
        boolean vertInBounds = y >= 0 && y <= dimensions.getGridHeightPixels();
        if (horizInBounds && vertInBounds) {
          if (!Float.isNaN(data[y][x])) {
            System.err.println("Possible collision at: " + x + ", " + y);
          }
          data[y][x] = (float) value;
        } else {
          System.err.println("Out of bounds: " + x + ", " + y);
          System.err.println("  grid of " + gridWidthPixels + ", " + gridHeightPixels);
          System.err.println("  " + lon + ", " + lat + " for " + currentPoint.toString());
        }
      }

      // Create envelope for the world file
      ReferencedEnvelope envelope = new ReferencedEnvelope(
          dimensions.getMinLon(), dimensions.getMaxLon(),
          dimensions.getMinLat(), dimensions.getMaxLat(),
          DefaultGeographicCRS.WGS84
      );

      // Create the grid coverage
      GridCoverage2D coverage = gcf.create(
          "coverage",
          data,
          envelope
      );

      // Create GeoTIFF writer
      GeoTiffWriter writer = new GeoTiffWriter(outputStream);

      // Write the coverage
      writer.write(coverage, null);
      writer.dispose();

    } catch (Exception e) {
      throw new RuntimeException("Failed to write geotiff: " + e.getMessage(), e);
    }
  }

  @Override
  protected List<String> getRequiredVariables() {
    return List.of("position.latitude", "position.longitude", variable);
  }
}
