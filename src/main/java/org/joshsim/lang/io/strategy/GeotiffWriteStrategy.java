/**
 * Strategy to write to an individual geotiff.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.geotiff.GeoTiffStore;


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

      // Create grid geometry
      GridExtent extent = new GridExtent(null, new double[]{minLon, minLat}, 
          new double[]{maxLon, maxLat}, new int[]{width, height});
      GridGeometry geometry = new GridGeometry(extent, null);

      // Create grid coverage
      GridCoverage coverage = new GridCoverage(variable, gridData, geometry);

      // Write to GeoTIFF using temporary file
      File tempFile = File.createTempFile("geotiff", ".tif");
      tempFile.deleteOnExit();
      
      try (GeoTiffStore store = new GeoTiffStore(null, new StorageConnector(tempFile))) {
        store.createCoverage(coverage);
        
        // Copy temp file to output stream
        Files.copy(tempFile.toPath(), outputStream);
        outputStream.flush();
      } finally {
        tempFile.delete();
      }
      
    } catch (Exception e) {
      throw new RuntimeException("Failed to write GeoTIFF: " + e.getMessage(), e);
    }
  }

  @Override
  protected List<String> getRequiredVariables() {
    return List.of("position.latitude", "position.longitude", variable);
  }
}
