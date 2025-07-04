/**
 * Logic to read from geotiffs and COGs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.geo.external.readers;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.storage.geotiff.GeoTiffStoreProvider;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalDataReader;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


/**
 * Strategy which supports reading geotiffs and COGs into simulations.
 *
 * <p>Strategy which supports reading geotiffs and COGs into simulations where the variable is
 * interpreted as the band index.</p>
 */
public class GeotiffExternalDataReader implements ExternalDataReader {

  private final EngineValueFactory valueFactory;
  private final Units units;

  private GeoTiffStore store;
  private GridCoverageResource coverage;
  private String crsCode;
  private GridGeometry geometry;
  private BigDecimal minX;
  private BigDecimal maxX;
  private BigDecimal minY;
  private BigDecimal maxY;

  /**
   * Create a new external data reader for geotiffs and COGs.
   *
   * @param valueFactory The value factory to use in building values returned from this reader.
   * @param units The units to use for the values returned from this reader.
   */
  public GeotiffExternalDataReader(EngineValueFactory valueFactory, Units units) {
    this.valueFactory = valueFactory;
    this.units = units;
  }

  @Override
  public void open(String sourcePath) throws IOException {
    try {
      openUnsafe(sourcePath);
    } catch (DataStoreException | FactoryException e) {
      throw new IOException("Failed to open GeoTIFF file: " + e.getMessage(), e);
    }
  }

  @Override
  public List<String> getVariableNames() throws IOException {
    List<String> names = new ArrayList<>();
    try {
      GridCoverage data = coverage.read(null);
      int numBands = data.getSampleDimensions().size();
      for (int i = 0; i < numBands; i++) {
        names.add(String.valueOf(i));
      }
    } catch (DataStoreException e) {
      throw new IOException("Failed to get band count: " + e.getMessage(), e);
    }
    return names;
  }

  @Override
  public String getCrsCode() {
    return crsCode;
  }

  @Override
  public void setCrsCode(String crsCode) {
    this.crsCode = crsCode;
  }

  @Override
  public Optional<Integer> getTimeDimensionSize() {
    return Optional.empty(); // GeoTIFF typically doesn't have time dimension
  }

  @Override
  public ExternalSpatialDimensions getSpatialDimensions() throws IOException {
    try {
      // Create coordinate lists
      List<BigDecimal> coordsX = new ArrayList<>();
      List<BigDecimal> coordsY = new ArrayList<>();

      // Get grid size
      long width = geometry.getExtent().getSize(0);
      long height = geometry.getExtent().getSize(1);

      // Calculate step sizes
      BigDecimal stepX = maxX.subtract(minX)
          .divide(BigDecimal.valueOf(width - 1), 6, RoundingMode.HALF_UP);
      BigDecimal stepY = maxY.subtract(minY)
          .divide(BigDecimal.valueOf(height - 1), 6, RoundingMode.HALF_UP);

      // Generate coordinate lists
      for (int i = 0; i < width; i++) {
        coordsX.add(minX.add(stepX.multiply(BigDecimal.valueOf(i))));
      }

      // For GeoTIFF, Y coordinates are often from top to bottom (inverted)
      // Generate Y coordinates from maxY to minY
      for (int i = 0; i < height; i++) {
        coordsY.add(maxY.subtract(stepY.multiply(BigDecimal.valueOf(i))));
      }

      return new ExternalSpatialDimensions("x", "y", null, crsCode, coordsX, coordsY);
    } catch (Exception e) {
      throw new IOException("Failed to get spatial dimensions: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<EngineValue> readValueAt(String variableName, BigDecimal x, BigDecimal y,
      int timeStep) throws IOException {
    try {
      // Parse band index from variable name
      int bandIndex;
      try {
        bandIndex = Integer.parseInt(variableName);
      } catch (NumberFormatException e) {
        throw new IOException("Invalid band index: variable name must be a valid integer", e);
      }

      // Use NetCDF-style coordinate lookup approach
      ExternalSpatialDimensions dimensions = getSpatialDimensions();
      
      // Find indices of closest X and Y coordinates using NetCDF pattern
      int indexX = dimensions.findClosestIndexX(x);
      int indexY = dimensions.findClosestIndexY(y);

      if (indexX < 0 || indexY < 0) {
        return Optional.empty();
      }

      // Read the coverage data directly using pixel coordinates
      GridCoverage data = coverage.read(null);
      RenderedImage renderedImage = data.render(null);
      Raster raster = renderedImage.getData();

      // Check bounds against actual raster size
      int maxX = raster.getWidth();
      int maxY = raster.getHeight();
      
      if (indexX >= maxX || indexY >= maxY) {
        return Optional.empty();
      }

      // Read the pixel value directly
      double[] values = new double[data.getSampleDimensions().size()];
      raster.getPixel(indexX, indexY, values);
      
      if (bandIndex >= values.length) {
        return Optional.empty();
      }
      
      double value = values[bandIndex];

      if (Double.isNaN(value)) {
        return Optional.empty();
      }

      // Create engine value with the result
      BigDecimal valueWrapped = BigDecimal.valueOf(value)
          .setScale(6, RoundingMode.HALF_UP);
      EngineValue result = valueFactory.build(valueWrapped, units);
      return Optional.of(result);

    } catch (DataStoreException e) {
      throw new IOException("Failed to read value: " + e.getMessage(), e);
    }
  }

  @Override
  public void setDimensions(String dimensionX, String dimensionY, Optional<String> timeDimension) {
    // GeoTIFF dimensions are implicit in the file structure
  }

  @Override
  public boolean canHandle(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return false;
    }
    String lowerPath = filePath.toLowerCase();
    return lowerPath.endsWith(".tif") || lowerPath.endsWith(".tiff");
  }

  @Override
  public void close() throws Exception {
    if (store != null) {
      store.close();
      store = null;
      coverage = null;
      geometry = null;
    }
  }

  /**
   * Open this reader using the specified source path, throwing exceptions.
   *
   * @param sourcePath The path to the GeoTIFF file to open. If it contains a colon, it will be
   *     treated as a formal URI. Otherwise, it will be treated as a local file path.
   * @throws DataStoreException Thrown if there is an issue initalizing the data store.
   * @throws FactoryException Thrown if there is an issue initalizing the SIS factory.
   * @throws IOException Thrown if there is an issue reading the GeoTIFF file.
   */
  private void openUnsafe(String sourcePath) throws DataStoreException, FactoryException,
        IOException {
    boolean useFullUri = sourcePath.contains(":");
    if (useFullUri) {
      URI fullUri = URI.create(sourcePath);
      StorageConnector connector = new StorageConnector(fullUri);
      GeoTiffStoreProvider provider = new GeoTiffStoreProvider();
      store = new GeoTiffStore(null, provider, connector, true);
    } else {
      File file = new File(sourcePath);
      StorageConnector connector = new StorageConnector(file);
      GeoTiffStoreProvider provider = new GeoTiffStoreProvider();
      store = new GeoTiffStore(null, provider, connector, true);
    }

    // Get the first grid coverage resource
    for (Resource resource : store.components()) {
      if (resource instanceof GridCoverageResource) {
        coverage = (GridCoverageResource) resource;
        break;
      }
    }

    if (coverage == null) {
      throw new IOException("No grid coverage found in GeoTIFF file");
    }

    // Initialize geometry and bounds
    geometry = coverage.getGridGeometry();
    DirectPosition lower = geometry.getEnvelope().getLowerCorner();
    DirectPosition upper = geometry.getEnvelope().getUpperCorner();

    minX = BigDecimal.valueOf(lower.getOrdinate(0)).setScale(6, RoundingMode.HALF_UP);
    maxX = BigDecimal.valueOf(upper.getOrdinate(0)).setScale(6, RoundingMode.HALF_UP);
    minY = BigDecimal.valueOf(lower.getOrdinate(1)).setScale(6, RoundingMode.HALF_UP);
    maxY = BigDecimal.valueOf(upper.getOrdinate(1)).setScale(6, RoundingMode.HALF_UP);
  }

}
