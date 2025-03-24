/**
 * A Builder for Grids.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.value.EngineValue;

/**
 * This class is responsible for building grid structures.
 * It creates a rectangular grid of patches based on geographic coordinates.
 */
public class GridBuilder {
  private BigDecimal topLeftLatitude;
  private BigDecimal topLeftLongitude;
  private BigDecimal bottomRightLatitude;
  private BigDecimal bottomRightLongitude;
  private EngineValue cellWidth;

  /**
   * Sets the top-left coordinates of the grid.
   *
   * @param latitude The top-left latitude
   * @param longitude The top-left longitude
   * @return this builder for method chaining
   */
  public GridBuilder setTopLeft(BigDecimal latitude, BigDecimal longitude) {
    this.topLeftLatitude = latitude;
    this.topLeftLongitude = longitude;
    return this;
  }

  /**
   * Sets the bottom-right coordinates of the grid.
   *
   * @param latitude The bottom-right latitude
   * @param longitude The bottom-right longitude
   * @return this builder for method chaining
   */
  public GridBuilder setBottomRight(BigDecimal latitude, BigDecimal longitude) {
    this.bottomRightLatitude = latitude;
    this.bottomRightLongitude = longitude;
    return this;
  }

  /**
   * Sets the width of each cell in the grid.
   *
   * @param cellWidth The width of each cell
   * @return this builder for method chaining
   */
  public GridBuilder setCellWidth(EngineValue cellWidth) {
    this.cellWidth = cellWidth;
    return this;
  }

  /**
   * Builds and returns a Grid based on the provided specifications.
   *
   * @return a new Grid instance
   * @throws IllegalStateException if any required parameters are missing
   */
  public Grid build() {
    validateParameters();

    List<Patch> patches = createPatches();
    return new Grid(patches, cellWidth);
  }

  private void validateParameters() {
    if (topLeftLatitude == null || topLeftLongitude == null) {
      throw new IllegalStateException("Top-left coordinates not specified");
    }

    if (bottomRightLatitude == null || bottomRightLongitude == null) {
      throw new IllegalStateException("Bottom-right coordinates not specified");
    }

    if (cellWidth == null) {
      throw new IllegalStateException("Cell width not specified");
    }

    if (topLeftLatitude.compareTo(bottomRightLatitude) <= 0) {
      throw new IllegalArgumentException(
        "Top-left latitude must be greater than bottom-right latitude");
    }

    if (topLeftLongitude.compareTo(bottomRightLongitude) >= 0) {
      throw new IllegalArgumentException(
        "Top-left longitude must be less than bottom-right longitude");
    }
  }

  private List<Patch> createPatches() {
    List<Patch> patches = new ArrayList<>();

    // Convert cell width to degrees (assuming it's in the same units as lat/long)
    double cellWidthDegrees = cellWidth.getAsDecimal().doubleValue();

    // Calculate the number of cells in each direction
    BigDecimal latDiff = topLeftLatitude.subtract(bottomRightLatitude);
    BigDecimal lonDiff = bottomRightLongitude.subtract(topLeftLongitude);

    int latCells = latDiff.divide(
        BigDecimal.valueOf(cellWidthDegrees), 0, RoundingMode.CEILING
    ).intValue();

    int lonCells = lonDiff.divide(
        BigDecimal.valueOf(cellWidthDegrees), 0, RoundingMode.CEILING
    ).intValue();

    // Create a patch for each cell
    for (int latIdx = 0; latIdx < latCells; latIdx++) {
      for (int lonIdx = 0; lonIdx < lonCells; lonIdx++) {
        BigDecimal cellTopLeftLat = topLeftLatitude.subtract(
            BigDecimal.valueOf(latIdx * cellWidthDegrees));
        BigDecimal cellTopLeftLon = topLeftLongitude.add(
            BigDecimal.valueOf(lonIdx * cellWidthDegrees));

        BigDecimal cellBottomRightLat = cellTopLeftLat.subtract(
            BigDecimal.valueOf(cellWidthDegrees));
        BigDecimal cellBottomRightLon = cellTopLeftLon.add(
            BigDecimal.valueOf(cellWidthDegrees));

        // Ensure we don't exceed the grid boundaries
        cellBottomRightLat = cellBottomRightLat.max(bottomRightLatitude);
        cellBottomRightLon = cellBottomRightLon.min(bottomRightLongitude);

        // Create the geometry for this cell
        Geometry cellGeometry = GeometryFactory.createSquare(
            cellTopLeftLat, cellTopLeftLon, cellBottomRightLat, cellBottomRightLon
        );

        // Create a patch with this geometry
        Patch patch = new Patch(cellGeometry);
        patches.add(patch);
      }
    }

    return patches;
  }

}
