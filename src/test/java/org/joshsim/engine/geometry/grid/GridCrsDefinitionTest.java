package org.joshsim.engine.geometry.grid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GridCrsDefinition} class.
 */
class GridCrsDefinitionTest {
  private GridCrsDefinition definition;
  private String name;
  private String baseCrsCode;
  private PatchBuilderExtents extents;
  private BigDecimal cellSize;
  private String cellSizeUnits;

  @BeforeEach
  void setUp() {
    name = "TestGrid";
    baseCrsCode = "EPSG:4326";
    extents = new PatchBuilderExtents(
        new BigDecimal("-116.0"), // topLeftX (west longitude)
        new BigDecimal("35.0"),   // topLeftY (north latitude)
        new BigDecimal("-115.0"), // bottomRightX (east longitude)
        new BigDecimal("34.0"));  // bottomRightY (south latitude)
    cellSize = new BigDecimal("30");
    cellSizeUnits = "m";

    definition = new GridCrsDefinition(name, baseCrsCode, extents, cellSize, cellSizeUnits);
  }

  @Test
  void constructorValidParametersCreatesInstance() {
    // Verify basic state
    assertEquals(name, definition.getName());
    assertEquals(baseCrsCode, definition.getBaseCrsCode());
    assertEquals(extents, definition.getExtents());
    assertEquals(cellSize, definition.getCellSize());
  }

  @Test
  void constructorNullNameThrowsException() {
    assertThrows(NullPointerException.class, () ->
        new GridCrsDefinition(null, baseCrsCode, extents, cellSize, cellSizeUnits));
  }

  @Test
  void constructorNullBaseCrsCodeThrowsException() {
    assertThrows(NullPointerException.class, () ->
        new GridCrsDefinition(name, null, extents, cellSize, cellSizeUnits));
  }

  @Test
  void constructorNullExtentsThrowsException() {
    assertThrows(NullPointerException.class, () ->
        new GridCrsDefinition(name, baseCrsCode, null, cellSize, cellSizeUnits));
  }

  @Test
  void constructorNullCellSizeThrowsException() {
    assertThrows(NullPointerException.class, () ->
        new GridCrsDefinition(name, baseCrsCode, extents, null, cellSizeUnits));
  }

  @Test
  void constructorNullCellSizeUnitsThrowsException() {
    assertThrows(NullPointerException.class, () ->
        new GridCrsDefinition(name, baseCrsCode, extents, cellSize, null));
  }

  @Test
  void constructorZeroCellSizeThrowsException() {
    assertThrows(IllegalArgumentException.class, () ->
        new GridCrsDefinition(name, baseCrsCode, extents, BigDecimal.ZERO, cellSizeUnits));
  }

  @Test
  void constructorNegativeCellSizeThrowsException() {
    assertThrows(IllegalArgumentException.class, () ->
        new GridCrsDefinition(name, baseCrsCode, extents, new BigDecimal("-1"), cellSizeUnits));
  }

  @Test
  void getCellSizeUnitAlwaysReturnMeters() {
    assertEquals("m", definition.getCellSizeUnit());

    // Create with different units, still returns meters
    GridCrsDefinition definition2 = new GridCrsDefinition(
        name, baseCrsCode, extents, cellSize, "degrees");
    assertEquals("m", definition2.getCellSizeUnit());
  }

  @Test
  void getBaseCrsCodeReturnsCorrectValue() {
    assertEquals("EPSG:4326", definition.getBaseCrsCode());

    // Try with a different CRS code
    GridCrsDefinition definition2 = new GridCrsDefinition(
        name, "EPSG:32611", extents, cellSize, cellSizeUnits);
    assertEquals("EPSG:32611", definition2.getBaseCrsCode());
  }

  @Test
  void gridToCrsCoordinatesOriginCellReturnsTopLeft() {
    BigDecimal[] expected = new BigDecimal[] {
        extents.getTopLeftX(),
        extents.getTopLeftY()
    };

    BigDecimal[] actual = definition.gridToCrsCoordinates(
        BigDecimal.ZERO, BigDecimal.ZERO);

    assertArrayEquals(expected, actual);
  }

  @Test
  void gridToCrsCoordinatesOneCellReturnsCellSizeAwayFromOrigin() {
    BigDecimal[] expected = new BigDecimal[] {
        extents.getTopLeftX().add(cellSize),
        extents.getTopLeftY().add(cellSize)
    };

    BigDecimal[] actual = definition.gridToCrsCoordinates(
        BigDecimal.ONE, BigDecimal.ONE);

    assertArrayEquals(expected, actual);
  }

  @Test
  void crsToGridCoordinatesTopLeftReturnsOriginCell() {
    BigDecimal[] expected = new BigDecimal[] {
        BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP),
        BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP)
    };

    BigDecimal[] actual = definition.crsToGridCoordinates(
        extents.getTopLeftX(), extents.getTopLeftY());

    assertEquals(expected[0], actual[0]);
    assertEquals(expected[1], actual[1]);
  }

  @Test
  void crsToGridCoordinatesCellSizeAwayFromOriginReturnsOneCell() {
    BigDecimal[] expected = new BigDecimal[] {
        BigDecimal.ONE.setScale(10, RoundingMode.HALF_UP),
        BigDecimal.ONE.setScale(10, RoundingMode.HALF_UP)
    };

    BigDecimal[] actual = definition.crsToGridCoordinates(
        extents.getTopLeftX().add(cellSize),
        extents.getTopLeftY().add(cellSize));

    assertEquals(expected[0], actual[0]);
    assertEquals(expected[1], actual[1]);
  }

  @Test
  void gridToCrsAndBackShouldBeIdentity() {
    BigDecimal gridX = new BigDecimal("5.5");
    BigDecimal gridY = new BigDecimal("8.75");

    BigDecimal[] crsCoords = definition.gridToCrsCoordinates(gridX, gridY);
    BigDecimal[] gridCoords = definition.crsToGridCoordinates(crsCoords[0], crsCoords[1]);

    assertEquals(0, gridX.compareTo(gridCoords[0]));
    assertEquals(0, gridY.compareTo(gridCoords[1]));
  }

  @Test
  void crsToGridAndBackShouldBeIdentity() {
    BigDecimal gridX = new BigDecimal("5.5");
    BigDecimal gridY = new BigDecimal("8.75");
    BigDecimal epsilon = new BigDecimal("0.0000000001"); // Tolerance threshold

    BigDecimal[] crsCoords = definition.gridToCrsCoordinates(gridX, gridY);
    BigDecimal[] gridCoords = definition.crsToGridCoordinates(crsCoords[0], crsCoords[1]);

    assertTrue(gridX.subtract(gridCoords[0]).abs().compareTo(epsilon) < 0,
        "X coordinates should be equal within tolerance");
    assertTrue(gridY.subtract(gridCoords[1]).abs().compareTo(epsilon) < 0,
        "Y coordinates should be equal within tolerance");
  }

  @Test
  void toStringFormatIsCorrect() {
    String expected = String.format(
        "GridCrsDefinition[name=%s, extents=(%s,%s to %s,%s), cellSize=%s %s]",
        name,
        extents.getTopLeftX(), extents.getTopLeftY(),
        extents.getBottomRightX(), extents.getBottomRightY(),
        cellSize, cellSizeUnits);

    assertEquals(expected, definition.toString());
  }
}
