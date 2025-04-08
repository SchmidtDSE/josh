package org.joshsim.engine.external.cog;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.checkerframework.checker.units.qual.s;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.joshsim.engine.value.type.DecimalScalar;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Reader for Cloud Optimized GeoTIFF (COG) files that extracts data
 * based on specified geometries.
 */
public class CogReader {

  /**
   * Read values from a COG file for the specified geometry.
   *
   * @param path Path to the COG file
   * @param geometry Geometry defining the area of interest
   * @return Distribution of values from the COG within the geometry
   * @throws IOException if there is an error reading the file
   */
  public static GridCoverage getCoverageFromDisk(
      String path,
      Geometry geometry
  ) throws IOException {
    try (DataStore store = DataStores.open(new File(path))) {
      // Get the first image in the GeoTIFF file
      // TODO: Hacky casting and assumption of first image
      Collection<? extends Resource> allImages = ((Aggregate) store).components();
      GridCoverageResource firstImage = (GridCoverageResource) allImages.iterator().next();

      CoordinateReferenceSystem cogCrs = firstImage
          .getGridGeometry()
          .getCoordinateReferenceSystem();


      // Create an envelope from the geometry bounds
      GeneralEnvelope areaOfInterest = geometry.getEnvelope();

      // Read data from the file for the specified area
      GridCoverage coverage = firstImage.read(new GridGeometry(areaOfInterest), null);
      return coverage;

    } catch (DataStoreException e) {
      throw new IOException("Failed to read COG file: " + path, e);
    }
  }

  /**
   * Transforms a grid coverage from its native CRS to the specified target CRS.
   *
   * @param coverage The source grid coverage
   * @param targetCrs The target coordinate reference system
   * @return A new grid coverage in the target CRS
   * @throws DataStoreException if the transformation cannot be performed
   */
  public static GridCoverage transformCoverage(
      GridCoverage coverage, 
      CoordinateReferenceSystem targetCrs
  ) throws DataStoreException {
      if (targetCrs == null) {
          return coverage; // No transformation needed
      }

      CoordinateReferenceSystem sourceCrs = coverage.getCoordinateReferenceSystem();
      
      // If source and target CRS are the same, no transformation needed
      if (CRS.equalsIgnoreMetadata(sourceCrs, targetCrs)) {
          return coverage;
      }
      
      try {
          // Transform the coverage to the target CRS
          return coverage.resample(targetCrs, transform, null);
      } catch (Exception e) {
          throw new DataStoreException("Failed to transform coverage to target CRS", e);
      }
  }

  /**
   * Extracts values from a grid coverage and converts them to EngineValue objects.
   *
   * @param coverage The grid coverage to extract values from
   * @param geometry The geometry used for filtering points (optional)
   * @return A list of EngineValue objects
   */
  public static List<BigDecimal> extractValuesFromCoverage(
      GridCoverage coverage, Geometry geometry
  ) {
    List<BigDecimal> values = new ArrayList<>();

    // Get the grid extent
    GridExtent extent = coverage.getGridGeometry().getExtent();
    long width = extent.getSize(0);
    long height = extent.getSize(1);

    // Get the CRS from the coverage
    CoordinateReferenceSystem coverageCrs = coverage.getCoordinateReferenceSystem();
    // Ensure consistent X,Y ordering (same as GridBuilder)
    AbstractCRS abstractCoverageCrs = AbstractCRS
        .castOrCopy(coverageCrs)
        .forConvention(AxesConvention.RIGHT_HANDED);

    // Create an evaluator to get pixel values
    GridCoverage.Evaluator evaluator = coverage.evaluator();

    try {
      // Iterate through grid cells
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          // Convert grid coordinates to CRS coordinates
          DirectPosition2D gridPos = new DirectPosition2D(abstractCoverageCrs, x, y);

          // Get direct position in the coverage's CRS
          DirectPosition2D worldPos = convertGridToWorld(coverage, gridPos);

          if (worldPos.getY() - geometry.getShape().getCenter().getY() < 4 &&
              worldPos.getY() - geometry.getShape().getCenter().getY() > -4 &&
              worldPos.getX() - geometry.getShape().getCenter().getX() < 4 &&
              worldPos.getX() - geometry.getShape().getCenter().getX() > -4) {
            worldPos.getX();
          }
          // Check if this point is within our geometry
          if (geometry == null || geometry.intersects(
                  BigDecimal.valueOf(worldPos.getX()),
                  BigDecimal.valueOf(worldPos.getY()))
              ) {

            // Apply the evaluator to get the value at this position
            double[] result = evaluator.apply(gridPos);

            // Get the first band value (assuming single-band data)
            double value = result[0];

            // Skip NaN or invalid values
            if (!Double.isNaN(value) && Double.isFinite(value)) {
              // Create a scalar value and add it to the list
              BigDecimal decimalValue = BigDecimal.valueOf(value);
              values.add(decimalValue);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error extracting values from coverage", e);
    }
    return values;
  }

  /**
   * Converts grid coordinates to world coordinates using the coverage's grid geometry.
   *
   * @param coverage The grid coverage
   * @param gridPos The grid position
   * @return The world position
   */
  private static DirectPosition2D convertGridToWorld(
      GridCoverage coverage,
      DirectPosition2D gridPos
  ) {
    try {
      // Get the transform from grid to CRS coordinates, specifying CELL_CENTER
      MathTransform gridToCrs = coverage.getGridGeometry()
          .getGridToCRS(PixelInCell.CELL_CENTER);

      // Create a new position for the result
      DirectPosition2D worldPos = new DirectPosition2D();

      // Apply the transformation
      gridToCrs.transform(gridPos, worldPos);

      return worldPos;
    } catch (TransformException e) {
      throw new RuntimeException("Failed to transform grid coordinates to world coordinates", e);
    }
  }
}
