/**
 * Strategy to write to geotiffs via netcdf-java.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import static org.gdal.gdal.gdal.AllRegister;
import static org.gdal.gdal.gdal.GetDriverByName;
import static org.gdal.gdalconst.gdalconstConstants.GDT_Float64;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;


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
    try {
      // Create a grid coverage factory
      final GridCoverageFactory gcf = new GridCoverageFactory();
      
      // Create the grid for our data
      float[][] data = new float[dimensions.getGridHeightPixels()][dimensions.getGridWidthPixels()];
      
      // Initialize with NaN
      for (int y = 0; y < dimensions.getGridHeightPixels(); y++) {
        for (int x = 0; x < dimensions.getGridWidthPixels(); x++) {
          data[y][x] = Float.NaN;
        }
      }
      
      // Fill the grid with our data
      for (Map<String, String> record : records) {
        double lon = Double.parseDouble(record.get("position.longitude"));
        double lat = Double.parseDouble(record.get("position.latitude"));
        double value = Double.parseDouble(record.get(variable));
        
        // Calculate position using Haversine distances
        HaversineUtil.HaversinePoint topLeft = new HaversineUtil.HaversinePoint(
            BigDecimal.valueOf(dimensions.getMinLon()),
            BigDecimal.valueOf(dimensions.getMaxLat())
        );
        HaversineUtil.HaversinePoint currentPoint = new HaversineUtil.HaversinePoint(
            BigDecimal.valueOf(lon),
            BigDecimal.valueOf(lat)
        );
        
        // Get distances from top-left corner
        BigDecimal distanceWest = HaversineUtil.getDistance(
            topLeft,
            new HaversineUtil.HaversinePoint(BigDecimal.valueOf(lon), BigDecimal.valueOf(dimensions.getMaxLat()))
        );
        BigDecimal distanceSouth = HaversineUtil.getDistance(
            topLeft,
            new HaversineUtil.HaversinePoint(BigDecimal.valueOf(dimensions.getMinLon()), BigDecimal.valueOf(lat))
        );
        
        // Calculate grid position using percentages
        int x = (int)(distanceWest.doubleValue() / dimensions.getWidthMeters().doubleValue() * dimensions.getGridWidthPixels());
        int y = (int)(distanceSouth.doubleValue() / dimensions.getHeightMeters().doubleValue() * dimensions.getGridHeightPixels());
        
        // Check bounds
        if (x >= 0 && x < dimensions.getGridWidthPixels() && 
            y >= 0 && y < dimensions.getGridHeightPixels()) {
          data[y][x] = (float)value;
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