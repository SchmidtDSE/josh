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
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.geotiff


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

  @Override
  protected void writeAll(List<Map<String, String>> records, OutputStream outputStream) {
    try {
      // Create temp file
      File tempFile = File.createTempFile("geotiff", ".tif");
      tempFile.deleteOnExit();

      // Create data array
      float[] data = new float[dimensions.getGridWidthPixels() * dimensions.getGridHeightPixels()];
      // Initialize with NaN
      for (int i = 0; i < data.length; i++) {
        data[i] = Float.NaN;
      }

      // Fill data array from records
      for (Map<String, String> record : records) {
        double longitude = Double.parseDouble(record.get("position.longitude"));
        double latitude = Double.parseDouble(record.get("position.latitude"));
        String valueStr = record.get(variable);
        float value = valueStr != null ? Float.parseFloat(valueStr) : Float.NaN;

        // Calculate grid position
        int x = (int) ((longitude - dimensions.getMinLon()) /
            (dimensions.getMaxLon() - dimensions.getMinLon()) *
            dimensions.getGridWidthPixels());
        int y = dimensions.getGridHeightPixels() - 1 - (int) ((latitude - dimensions.getMinLat()) /
            (dimensions.getMaxLat() - dimensions.getMinLat()) *
            dimensions.getGridHeightPixels());

        if (x >= 0 && x < dimensions.getGridWidthPixels() &&
            y >= 0 && y < dimensions.getGridHeightPixels()) {
          data[y * dimensions.getGridWidthPixels() + x] = value;
        }
      }

      // Create the GeoTIFF writer
      GeotiffWriter writer = new GeotiffWriter(tempFile.getAbsolutePath());

      // Convert data to Array
      Array dataArray = Array.factory(DataType.FLOAT,
          new int[]{dimensions.getGridHeightPixels(), dimensions.getGridWidthPixels()},
          data);

      // Write the data
      writer.writeGrid(dimensions.getMinLat(), dimensions.getMinLon(),
          (dimensions.getMaxLat() - dimensions.getMinLat()) / dimensions.getGridHeightPixels(),
          (dimensions.getMaxLon() - dimensions.getMinLon()) / dimensions.getGridWidthPixels(),
          dataArray);

      writer.close();

      // Copy temp file to output stream
      try (OutputStream out = outputStream) {
        byte[] buffer = new byte[8192];
        java.io.FileInputStream fis = new java.io.FileInputStream(tempFile);
        int length;
        while ((length = fis.read(buffer)) > 0) {
          out.write(buffer, 0, length);
        }
        fis.close();
        out.flush();
        tempFile.delete();
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to write GeoTIFF: " + e);
    }
  }

  @Override
  protected List<String> getRequiredVariables() {
    return List.of("position.latitude", "position.longitude", variable);
  }
}