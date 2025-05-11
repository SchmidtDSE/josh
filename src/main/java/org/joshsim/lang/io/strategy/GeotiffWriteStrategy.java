/**
 * Strategy to write to an individual geotiff.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.geometry.GeneralGridGeometry;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.geotiff.GeoTiffStoreProvider;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.referencing.CRS;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.operation.transform.AffineTransform2D;
import org.apache.sis.coverage.grid.GridExtent;


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
  private final PatchBuilderExtents extents;
  private final BigDecimal width;

  /**
   * Constructs a GeotiffWriteStrategy with the specified variable.
   *
   * @param variable The variable to be written within this strategy.
   * @param extents The size of the grid to be written where values are expressed in degrees on
   *     Earth-space.
   * @param width The width and height of each patch to be written (equivalent to each pixel in the
   *     geotiff
   */
  public GeotiffWriteStrategy(String variable, PatchBuilderExtents extents, BigDecimal width) {
    this.variable = variable;
    this.extents = extents;
    this.width = width;
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

      // Process data from records
      double minLon = Double.MAX_VALUE;
      double maxLon = -Double.MAX_VALUE;
      double minLat = Double.MAX_VALUE;
      double maxLat = -Double.MAX_VALUE;

      // Find bounds
      for (Map<String, String> record : records) {
        double lon = Double.parseDouble(record.get("position.longitude"));
        double lat = Double.parseDouble(record.get("position.latitude"));
        minLon = Math.min(minLon, lon);
        maxLon = Math.max(maxLon, lon);
        minLat = Math.min(minLat, lat);
        maxLat = Math.max(maxLat, lat);
      }

      // Create store
      StorageConnector connector = new StorageConnector(tempFile);
      GeoTiffStore store = new GeoTiffStoreProvider().open(connector);

      // Create grid data
      int width = (int) Math.ceil((maxLon - minLon) / this.width.doubleValue());
      int height = (int) Math.ceil((maxLat - minLat) / this.width.doubleValue());
      float[][] gridData = new float[height][width];

      // Fill grid with values
      for (Map<String, String> record : records) {
        double lon = Double.parseDouble(record.get("position.longitude"));
        double lat = Double.parseDouble(record.get("position.latitude"));
        String valueStr = record.get(variable);
        float value = valueStr != null ? Float.parseFloat(valueStr) : 0f;

        int x = (int) ((lon - minLon) / this.width.doubleValue());
        int y = height - 1 - (int) ((lat - minLat) / this.width.doubleValue());

        if (x >= 0 && x < width && y >= 0 && y < height) {
          gridData[y][x] = value;
        }
      }

      // Create grid coverage using Apache SIS GridCoverage builder
      GridCoverageBuilder builder = new GridCoverageBuilder();
      builder.setName(variable);
      builder.setCoordinateReferenceSystem(CRS.forCode("EPSG:4326"));
      builder.setDomain(new GeneralGridGeometry(
          new GridExtent(width, height),
          new AffineTransform2D(
              this.width.doubleValue(), 0.0,
              0.0, -this.width.doubleValue(),
              minLon, maxLat
          ),
          CommonCRS.WGS84.geographic()
      ));
      builder.setValues(gridData);

      GridCoverage coverage = builder.build();

      // Write to GeoTIFF
      store.write(coverage);
      store.close();

      // Copy temp file to output stream
      byte[] buffer = Files.readAllBytes(tempFile.toPath());
      outputStream.write(buffer);
      outputStream.flush();

      // Cleanup
      tempFile.delete();

    } catch (IOException | DataStoreException | FactoryException e) {
      throw new RuntimeException("Failed to write GeoTIFF", e);
    }
  }

  @Override
  protected List<String> getRequiredVariables() {
    return List.of("position.latitude", "position.longitude", variable);
  }
}