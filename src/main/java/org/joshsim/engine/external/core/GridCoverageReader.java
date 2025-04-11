package org.joshsim.engine.external.core;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.joshsim.engine.geometry.EngineGeometry;

/**
 * Abstract base class for grid coverage readers that provides common functionality
 * for different file formats, with the assumption that implementations will
 * handle specific formats.
 */
public abstract class GridCoverageReader {

  /**
   * Read a coverage from a file for the specified geometry.
   *
   * @param path Path to the file
   * @param geometry EngineGeometry defining the area of interest
   * @return GridCoverage2D containing the data within the geometry's bounds
   * @throws IOException if there is an error reading the file
   */
  public abstract GridCoverage2D getCoverageFromIo(String path, EngineGeometry geometry)
      throws IOException;

  /**
   * Transforms a grid coverage from its native CRS to the specified target CRS.
   *
   * @param coverage The source grid coverage
   * @param targetCrs The target coordinate reference system
   * @return A new grid coverage in the target CRS
   * @throws Exception if the transformation cannot be performed
   */
  public GridCoverage2D transformCoverage(
      GridCoverage2D coverage,
      CoordinateReferenceSystem targetCrs
  ) throws Exception {
    if (targetCrs == null) {
      return coverage; // No transformation needed
    }

    CoordinateReferenceSystem sourceCrs = coverage.getCoordinateReferenceSystem();

    // If source and target CRS are the same, no transformation needed
    if (CRS.equalsIgnoreMetadata(sourceCrs, targetCrs)) {
      return coverage;
    }

    // Use the resample operation with the transform
    Operations ops = new Operations(null);
    return (GridCoverage2D) ops.resample(coverage, targetCrs);
  }

  /**
   * Extracts values from a grid coverage and converts them to BigDecimal values.
   *
   * @param coverage The grid coverage to extract values from
   * @param geometry The geometry used for filtering points
   * @return A list of BigDecimal values
   */
  public List<BigDecimal> extractValuesFromCoverage(
      GridCoverage2D coverage, EngineGeometry geometry
  ) {
    List<BigDecimal> values = new ArrayList<>();

    // Get coverage bounds
    ReferencedEnvelope bounds = new ReferencedEnvelope(coverage.getEnvelope2D());

    // Calculate resolution
    double resX = bounds.getWidth() / coverage.getRenderedImage().getWidth();
    double resY = bounds.getHeight() / coverage.getRenderedImage().getHeight();

    // Iterate through cells that potentially intersect the geometry
    double minX = Math.max(bounds.getMinX(), geometry.getEnvelope().getMinimum(0));
    double maxX = Math.min(bounds.getMaxX(), geometry.getEnvelope().getMaximum(0));
    double minY = Math.max(bounds.getMinY(), geometry.getEnvelope().getMinimum(1));
    double maxY = Math.min(bounds.getMaxY(), geometry.getEnvelope().getMaximum(1));

    for (double y = minY + resY / 2; y < maxY; y += resY) {
      for (double x = minX + resX / 2; x < maxX; x += resX) {
        // Check if this point is within our geometry
        if (geometry == null || geometry.intersects(
                BigDecimal.valueOf(x),
                BigDecimal.valueOf(y))) {

          // Get value at this position
          Position2D position = new Position2D(coverage.getCoordinateReferenceSystem(), x, y);
          double[] result = coverage.evaluate((Position) position, new double[1]);

          // First band only
          double value = result[0];

          // Skip NaN or invalid values
          if (!Double.isNaN(value) && Double.isFinite(value)) {
            values.add(BigDecimal.valueOf(value));
          }
        }
      }
    }

    return values;
  }
}