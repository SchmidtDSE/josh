package org.joshsim.geo.external.readers;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.ExternalDataReader;
import org.joshsim.geo.external.ExternalSpatialDimensions;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;


public class GeotiffExternalDataReader implements ExternalDataReader {
  private final EngineValueFactory valueFactory;
  private GeoTiffStore store;
  private GridCoverageResource coverage;
  private String crsCode;
  private GridGeometry geometry;
  private BigDecimal minX;
  private BigDecimal maxX;
  private BigDecimal minY;
  private BigDecimal maxY;

  public GeotiffExternalDataReader(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  @Override
  public void open(String sourcePath) throws IOException {
    try {
      File file = new File(sourcePath);
      store = new GeoTiffStore(file);
      
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

      // Get CRS
      CoordinateReferenceSystem crs = geometry.getCoordinateReferenceSystem();
      crsCode = CRS.getIdentifier(crs);
      
    } catch (DataStoreException | FactoryException e) {
      throw new IOException("Failed to open GeoTIFF file: " + e.getMessage(), e);
    }
  }

  @Override
  public List<String> getVariableNames() throws IOException {
    List<String> names = new ArrayList<>();
    names.add("value"); // GeoTIFF typically has one band/variable
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
      int width = (int) geometry.getExtent().getSize().get(0);
      int height = (int) geometry.getExtent().getSize().get(1);
      
      // Calculate step sizes
      BigDecimal stepX = maxX.subtract(minX).divide(BigDecimal.valueOf(width - 1), 6, RoundingMode.HALF_UP);
      BigDecimal stepY = maxY.subtract(minY).divide(BigDecimal.valueOf(height - 1), 6, RoundingMode.HALF_UP);
      
      // Generate coordinate lists
      for (int i = 0; i < width; i++) {
        coordsX.add(minX.add(stepX.multiply(BigDecimal.valueOf(i))));
      }
      
      for (int i = 0; i < height; i++) {
        coordsY.add(minY.add(stepY.multiply(BigDecimal.valueOf(i))));
      }
      
      return new ExternalSpatialDimensions("x", "y", null, crsCode, coordsX, coordsY);
    } catch (Exception e) {
      throw new IOException("Failed to get spatial dimensions: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<EngineValue> readValueAt(String variableName, BigDecimal x, BigDecimal y, int timeStep) 
      throws IOException {
    try {
      // Create a direct position for the coordinates
      double[] coords = new double[] {x.doubleValue(), y.doubleValue()};
      DirectPosition position = geometry.getCoordinateReferenceSystem().getCoordinateSystem()
        .getFactory().createDirectPosition(coords);
      
      // Read the grid coverage
      GridCoverage data = coverage.read(null);
      double[] values = data.evaluate(position, new double[1]);
      
      if (values == null || values.length == 0 || Double.isNaN(values[0])) {
        return Optional.empty();
      }
      
      // Create engine value with the result
      BigDecimal value = BigDecimal.valueOf(values[0]).setScale(6, RoundingMode.HALF_UP);
      return Optional.of(valueFactory.build(value, new Units(null)));
      
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
}
