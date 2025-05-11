/**
 * Strategy to write to an individual geotiff.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.awt.geom.Rectangle2D;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;


/**
 * Strategy to write a single geotiff.
 *
 * <p>Strategy to write a single band geotiff where there is one geotiff per timestep, variable,
 * and replicate combination. There is one GeotiffWriteStrategy instantiated per output geotiff.
 * This is written via Apache SIS and will convert position.latitude, position.longitude, and
 * the variable specified to doubles from Strings before writing.</p>
 */
public class GeotiffWriteStrategy extends PendingRecordWriteStrategy {

  private final String variable;

  /**
   * Constructs a GeotiffWriteStrategy with the specified variable.
   *
   * @param variable The variable to be written within this strategy.
   */
  public GeotiffWriteStrategy(String variable) {
    this.variable = variable;
  }

  @Override
  protected void writeAll(List<Map<String, String>> records, OutputStream outputStream) {
    try {
      // Create arrays for coordinates and values
      double[] lats = new double[records.size()];
      double[] lons = new double[records.size()];
      double[] values = new double[records.size()];

      // Parse coordinates and values from records
      for (int i = 0; i < records.size(); i++) {
        Map<String, String> record = records.get(i);
        lats[i] = Double.parseDouble(record.get("position.latitude"));
        lons[i] = Double.parseDouble(record.get("position.longitude"));
        values[i] = Double.parseDouble(record.getOrDefault(variable, "0.0"));
      }

      // Find extent of data
      double minLat = Double.MAX_VALUE;
      double maxLat = -Double.MAX_VALUE;
      double minLon = Double.MAX_VALUE;
      double maxLon = -Double.MAX_VALUE;
      
      for (int i = 0; i < records.size(); i++) {
        minLat = Math.min(minLat, lats[i]);
        maxLat = Math.max(maxLat, lats[i]);
        minLon = Math.min(minLon, lons[i]);
        maxLon = Math.max(maxLon, lons[i]);
      }

      // Calculate grid dimensions and resolution
      int width = 256;
      int height = 256;
      double lonRes = (maxLon - minLon) / width;
      double latRes = (maxLat - minLat) / height;

      // Create grid data
      float[][] gridData = new float[height][width];
      
      // Populate grid using nearest neighbor
      for (int i = 0; i < records.size(); i++) {
        int x = (int)((lons[i] - minLon) / lonRes);
        int y = height - 1 - (int)((lats[i] - minLat) / latRes);
        
        if (x >= 0 && x < width && y >= 0 && y < height) {
          gridData[y][x] = (float)values[i];
        }
      }

      // Create grid coverage
      GridCoverageFactory factory = new GridCoverageFactory();
      GridCoverage2D coverage = factory.create(
          variable,
          gridData,
          new Rectangle2D.Double(minLon, minLat, maxLon - minLon, maxLat - minLat)
      );

      // Write to GeoTIFF
      GeoTiffWriter writer = new GeoTiffWriter(outputStream);
      writer.write(coverage, null);
      writer.dispose();
      
    } catch (Exception e) {
      throw new RuntimeException("Failed to write GeoTIFF: " + e.getMessage(), e);
    }
  }

  @Override
  protected List<String> getRequiredVariables() {
    return List.of("position.latitude", "position.longitude", variable);
  }
}
