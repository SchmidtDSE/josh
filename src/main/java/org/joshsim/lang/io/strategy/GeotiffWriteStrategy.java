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
   * Write all of the records to the temporary file before copying to output stream.
   *
   * <p>Write all of the records to the temporary file before copying to output stream using cloud
   * optimized geotiff format where possible. This will be done through GDAL.</p>
   *
   * @param records The records to be written where each element of the records list corresponds to
   *     a pixel. This will have properties position.longitude and position.latitude as well as a
   *     value given by the attribute "variable" on this object. All should be converted to double.
   * @param outputStream The stream to which to write this geotiff after it is written to the
   *     temporary file.
   */
  @Override
  protected void writeAll(List<Map<String, String>> records, OutputStream outputStream) {
    try {
      // Create temp file
      File tempFile = File.createTempFile("geotiff", ".tif");
      tempFile.deleteOnExit();

      org.gdal.gdal.gdal.AllRegister();
      
      // Create driver and dataset
      org.gdal.gdal.Driver driver = org.gdal.gdal.gdal.GetDriverByName("GTiff");
      org.gdal.gdal.Dataset dataset = driver.Create(
          tempFile.getAbsolutePath(),
          dimensions.getGridWidthPixels(),
          dimensions.getGridHeightPixels(),
          1,  // One band for single variable
          org.gdal.gdalconst.gdalconstConstants.GDT_Float64
      );
      
      // Set geotransform
      double[] geoTransform = new double[6];
      geoTransform[0] = dimensions.getMinLon(); // top left x
      geoTransform[1] = (dimensions.getMaxLon() - dimensions.getMinLon()) / dimensions.getGridWidthPixels(); // pixel width
      geoTransform[2] = 0.0; // rotation (0 = north up)
      geoTransform[3] = dimensions.getMaxLat(); // top left y 
      geoTransform[4] = 0.0; // rotation (0 = north up)
      geoTransform[5] = (dimensions.getMinLat() - dimensions.getMaxLat()) / dimensions.getGridHeightPixels(); // pixel height
      dataset.SetGeoTransform(geoTransform);
      
      // Set projection to WGS84
      dataset.SetProjection("EPSG:4326");
      
      // Create band and write data
      org.gdal.gdal.Band band = dataset.GetRasterBand(1);
      double[] data = new double[dimensions.getGridWidthPixels() * dimensions.getGridHeightPixels()];
      
      // Initialize with nodata
      java.util.Arrays.fill(data, -9999.0);
      
      // Fill data array from records
      for (Map<String, String> record : records) {
        double lon = Double.parseDouble(record.get("position.longitude"));
        double lat = Double.parseDouble(record.get("position.latitude"));
        double value = Double.parseDouble(record.get(variable));
        
        // Convert geo coordinates to pixel coordinates
        int x = (int)((lon - dimensions.getMinLon()) / geoTransform[1]);
        int y = (int)((lat - dimensions.getMaxLat()) / geoTransform[5]);
        
        if (x >= 0 && x < dimensions.getGridWidthPixels() && 
            y >= 0 && y < dimensions.getGridHeightPixels()) {
          data[y * dimensions.getGridWidthPixels() + x] = value;
        }
      }
      
      band.WriteRaster(0, 0, dimensions.getGridWidthPixels(), dimensions.getGridHeightPixels(), data);
      band.SetNoDataValue(-9999.0);
      
      // Close the dataset to flush changes
      dataset.delete();

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