package org.joshsim.engine.external;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.type.DecimalScalar;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

/**
 * Reads Cloud Optimized GeoTIFF (COG) files and extracts values
 * that intersect with a spatial4j Shape.
 */
public class CogReader {
  private final EngineValueCaster caster;
  private final Units units;
  private final int bandIndex;  // Band/layer index to read
  private final int resourceIndex; // Which coverage resource to use if multiple exist

  /**
   * Creates a new CogReader with specific band configuration.
   *
   * @param caster The value caster for creating engine values
   * @param units The units of the values in the COG file
   * @param bandIndex The band/layer index to read from the GeoTIFF (0-based)
   * @param resourceIndex The resource index to use if multiple coverages exist
   */
  public CogReader(EngineValueCaster caster, Units units, int bandIndex, int resourceIndex) {
    this.caster = caster;
    this.units = units;
    this.bandIndex = bandIndex;
    this.resourceIndex = resourceIndex;
  }

  /**
   * Creates a new CogReader with default band (0) and resource (0) selection.
   *
   * @param caster The value caster for creating engine values
   * @param units The units of the values in the COG file
   */
  public CogReader(EngineValueCaster caster, Units units) {
    this(caster, units, 0, 0);
  }

  /**
   * Reads values from a COG file that intersect with the specified geometry.
   *
   * @param cogPath Path to the COG file
   * @param geometry The geometry defining the area to extract values from
   * @return A RealizedDistribution of values within the geometry
   * @throws IOException If an error occurs reading the COG file
   */
  public RealizedDistribution readValues(String cogPath, Geometry geometry) throws IOException {
    try (DataStore store = DataStores.open(new File(cogPath))) {
      return readFromStore(store, geometry, cogPath);
    } catch (DataStoreException e) {
      throw new IOException("Failed to read COG file: " + e.getMessage(), e);
    }
  }

  /**
   * Reads values from a COG file at a remote URL that intersect with the specified geometry.
   *
   * @param cogUrl URL of the COG file
   * @param geometry The geometry defining the area to extract values from
   * @return A RealizedDistribution of values within the geometry
   * @throws IOException If an error occurs reading the COG file
   */
  public RealizedDistribution readValues(URL cogUrl, Geometry geometry) throws IOException {
    try (DataStore store = DataStores.open(cogUrl)) {
      return readFromStore(store, geometry, cogUrl.toString());
    } catch (DataStoreException e) {
      throw new IOException("Failed to read COG file: " + e.getMessage(), e);
    }
  }

  /**
   * Common logic for reading values from a data store.
   *
   * @param store The data store containing grid coverage data
   * @param geometry The geometry defining the area to extract values from
   * @param sourceName Name of the source for error messages
   * @return A RealizedDistribution of values within the geometry
   * @throws IOException If an error occurs reading the data
   * @throws DataStoreException If an error occurs accessing the data store
   */
  private RealizedDistribution readFromStore(
      DataStore store,
      Geometry geometry,
      String sourceName
  ) throws IOException, DataStoreException {
    // Extract the envelope from the geometry shape's bounding box
    GeneralEnvelope envelope = shapeToEnvelope(geometry.getShape());

    // Get the specified grid coverage resource from the store
    GridCoverageResource resource = findGridCoverageResource(store, resourceIndex);
    if (resource == null) {
      throw new IOException("No grid coverage resource found at index " + resourceIndex +
                           " in " + sourceName);
    }

    // Read only the data within our envelope
    GridCoverage coverage = resource.read(new GridGeometry(envelope), null);

    // Extract values that intersect with the geometry
    List<EngineValue> values = extractValues(coverage, geometry, bandIndex);

    if (values.isEmpty()) {
      throw new IOException("No values found within the specified geometry");
    }

    return new RealizedDistribution(caster, values, units);
  }

  /**
   * Finds a specific grid coverage resource in a data store by index.
   *
   * @param store The data store to search
   * @param index The index of the resource to find (0-based)
   * @return The specified grid coverage resource
   * @throws DataStoreException If an error occurs accessing the data store
   * @throws IOException If the requested resource index is out of bounds
   */
  private GridCoverageResource findGridCoverageResource(
      DataStore store,
      int index
  ) throws DataStoreException, IOException {
    // For stores that contain multiple resources
    if (store instanceof Aggregate) {
      Aggregate aggregate = (Aggregate) store;
      List<GridCoverageResource> resources = new ArrayList<>();

      // Find all grid coverage resources
      for (Resource resource : aggregate.components()) {
        if (resource instanceof GridCoverageResource) {
          resources.add((GridCoverageResource) resource);
        }
      }

      // Provide detailed information about available resources
      if (resources.isEmpty()) {
        throw new IOException("No grid coverage resources found in the data store.");
      }

      // Validate requested index
      if (index < 0 || index >= resources.size()) {
        throw new IOException(String.format(
            "Resource index %d is out of bounds."
            + "The data store contains %d grid coverage resources (valid indices: 0-%d).",
            index, resources.size(), resources.size() - 1)
        );
      }

      // Return the requested resource by index
      return resources.get(index);
    } else if (store instanceof GridCoverageResource) {
      // If store is a direct resource but index is not 0, that's an error
      if (index != 0) {
        throw new IOException(
            "Resource index must be 0 for this data store which contains only a single resource.");
      }
      return (GridCoverageResource) store;
    }
    throw new IOException(
        "The data store does not contain any grid coverage resources. Format may be unsupported.");
  }

  /**
   * Converts a spatial4j Shape to a GeneralEnvelope used by Apache SIS.
   *
   * @param shape The spatial4j Shape to convert
   * @return A GeneralEnvelope representing the bounds of the shape
   */
  private GeneralEnvelope shapeToEnvelope(Shape shape) {
    Rectangle bbox = shape.getBoundingBox();

    // Create an envelope from the bounds
    GeneralEnvelope envelope = new GeneralEnvelope(2);
    envelope.setRange(0, bbox.getMinX(), bbox.getMaxX());
    envelope.setRange(1, bbox.getMinY(), bbox.getMaxY());

    return envelope;
  }

  /**
   * Extracts values from a GridCoverage that intersect with the specified geometry.
   *
   * @param coverage The grid coverage containing the data
   * @param geometry The geometry to extract values from
   * @param bandIndex The band/layer index to read (0-based)
   * @return A list of EngineValues representing the extracted values
   */
  private List<EngineValue> extractValues(GridCoverage coverage, Geometry geometry, int bandIndex) {
    List<EngineValue> values = new ArrayList<>();

    try {
      // Get the grid geometry and extent
      GridGeometry gridGeometry = coverage.getGridGeometry();
      GridExtent extent = gridGeometry.getExtent();

      // Get a rendered image of the coverage
      RenderedImage image = coverage.render(extent);

      // Check if the requested band exists
      int numBands = image.getSampleModel().getNumBands();
      if (bandIndex < 0 || bandIndex >= numBands) {
        throw new IllegalArgumentException(
            "Band index " + bandIndex + " is out of bounds. Image has " + numBands + " bands.");
      }

      // Get the bounds of the image, in terms of indices
      int minX = image.getMinX();
      int minY = image.getMinY();
      int width = image.getWidth();
      int height = image.getHeight();

      // Get the transform from grid to world coordinates
      MathTransform gridToWorld = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER);

      // Get raster data for efficient access
      Raster raster = image.getData();

      // Position object for coordinate transformation
      DirectPosition2D gridPos = new DirectPosition2D();
      DirectPosition worldPos = new DirectPosition2D();

      // Iterate through each pixel in the image
      for (int y = minY; y < minY + height; y++) {
        for (int x = minX; x < minX + width; x++) {
          // Convert grid coordinates to world coordinates using the transform
          gridPos.setLocation(x + 0.5, y + 0.5);  // Center of the cell
          worldPos = gridToWorld.transform(gridPos, worldPos);

          double worldX = worldPos.getOrdinate(0);
          double worldY = worldPos.getOrdinate(1);

          // Check if the point is within the geometry
          if (geometry.intersects(BigDecimal.valueOf(worldX), BigDecimal.valueOf(worldY))) {
            // Get the pixel value for the specified band
            double pixelValue = raster.getSampleDouble(x, y, bandIndex);

            // Only add non-NaN values
            if (!Double.isNaN(pixelValue)) {
              DecimalScalar value = new DecimalScalar(
                  caster,
                  BigDecimal.valueOf(pixelValue),
                  units
              );
              values.add(value);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error extracting values from coverage: " + e.getMessage(), e);
    }

    return values;
  }
}
