/**
 * Test for building patches in grid space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the GridPatchBuilder class which creates patches in grid space.
 *
 * <p>These tests verify that the GridPatchBuilder correctly creates a grid of patches based on
 * provided extents, with proper validation and handling of edge cases.
 */
class GridPatchBuilderTest {

  @Test
  @DisplayName("Grid with standard extents should create the correct number of patches")
  void testBuildCreatesCorrectNumberOfPatches() {
    // Arrange
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(extents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(10));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(10));

    BigDecimal cellWidth = BigDecimal.valueOf(2);
    EntityPrototype prototype = mock(EntityPrototype.class);
    when(prototype.buildSpatial(any(Entity.class))).thenReturn(mock(MutableEntity.class));

    GridCrsDefinition gridCrsDefinition = new GridCrsDefinition(
        "GRID", "EPSG:4326", extents, cellWidth, "m");
    GridPatchBuilder builder = new GridPatchBuilder(gridCrsDefinition, prototype);

    // Act
    PatchSet patchSet = builder.build();

    // Assert
    List<MutableEntity> patches = patchSet.getPatches();
    assertNotNull(patches, "Patches list should not be null");
    assertEquals(
        25,
        patches.size(),
        "Should create 5x5=25 patches for 10x10 area with cell size 2"
    );
    assertEquals(gridCrsDefinition, patchSet.getGridCrsDefinition(),
        "Grid CRS definition should be preserved");
  }

  @Test
  @DisplayName("Grid with non-divisible extents should round up to include all areas")
  void testBuildWithNonDivisibleExtents() {
    // Arrange
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(extents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(7));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(7));

    BigDecimal cellWidth = BigDecimal.valueOf(3);
    EntityPrototype prototype = mock(EntityPrototype.class);
    when(prototype.buildSpatial(any(Entity.class))).thenReturn(mock(MutableEntity.class));

    GridCrsDefinition gridCrsDefinition = new GridCrsDefinition(
        "GRID", "EPSG:4326", extents, cellWidth, "m");
    GridPatchBuilder builder = new GridPatchBuilder(gridCrsDefinition, prototype);

    // Act
    PatchSet patchSet = builder.build();

    // Assert
    assertEquals(9, patchSet.getPatches().size(),
        "Should create 3x3=9 patches for 7x7 area with cell size 3");
  }

  @Test
  @DisplayName("Grid with negative coordinates should work correctly")
  void testBuildWithNegativeCoordinates() {
    // Arrange
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.valueOf(-5));
    when(extents.getTopLeftY()).thenReturn(BigDecimal.valueOf(-5));
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(5));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(5));

    BigDecimal cellWidth = BigDecimal.valueOf(2);
    EntityPrototype prototype = mock(EntityPrototype.class);
    when(prototype.buildSpatial(any(Entity.class))).thenReturn(mock(MutableEntity.class));

    GridCrsDefinition gridCrsDefinition = new GridCrsDefinition(
        "GRID", "EPSG:4326", extents, cellWidth, "m");
    GridPatchBuilder builder = new GridPatchBuilder(gridCrsDefinition, prototype);

    // Act
    PatchSet patchSet = builder.build();

    // Assert
    assertEquals(25, patchSet.getPatches().size(),
        "Should create 5x5=25 patches for -5 to 5 in both axes with cell size 2");
  }

  @Test
  @DisplayName("Invalid extents where topLeft is beyond bottomRight should fail validation")
  void testBuildValidatesExtents() {
    // Arrange
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.TEN);
    when(extents.getTopLeftY()).thenReturn(BigDecimal.TEN);
    when(extents.getBottomRightX()).thenReturn(BigDecimal.ONE);
    when(extents.getBottomRightY()).thenReturn(BigDecimal.ONE);

    BigDecimal cellWidth = BigDecimal.ONE;
    EntityPrototype prototype = mock(EntityPrototype.class);
    GridCrsDefinition gridCrsDefinition = new GridCrsDefinition(
        "GRID", "EPSG:4326", extents, cellWidth, "m");

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new GridPatchBuilder(gridCrsDefinition, prototype),
        "Should throw exception when topLeft is beyond bottomRight");
    assertEquals("Top left X must be less than bottom right X", exception.getMessage());
  }

  @Test
  @DisplayName("Extents with equal Y coordinates should fail validation")
  void testBuildValidatesCoordinatesY() {
    // Arrange
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(extents.getTopLeftY()).thenReturn(BigDecimal.TEN);
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(10));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.TEN);

    BigDecimal cellWidth = BigDecimal.ONE;
    EntityPrototype prototype = mock(EntityPrototype.class);
    GridCrsDefinition gridCrsDefinition = new GridCrsDefinition(
        "GRID", "EPSG:4326", extents, cellWidth, "m");

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new GridPatchBuilder(gridCrsDefinition, prototype),
        "Should throw exception when Y coordinates are equal");
    assertEquals("Top left Y must be less than bottom right Y", exception.getMessage());
  }

  @Test
  @DisplayName("Small area that results in just one patch should be handled correctly")
  void testBuildHandlesEmptyPatchArea() {
    // Arrange
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(extents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(1));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(1));

    BigDecimal cellWidth = BigDecimal.valueOf(2);
    EntityPrototype prototype = mock(EntityPrototype.class);
    when(prototype.buildSpatial(any(Entity.class))).thenReturn(mock(MutableEntity.class));

    GridCrsDefinition gridCrsDefinition = new GridCrsDefinition(
        "GRID", "EPSG:4326", extents, cellWidth, "m");
    GridPatchBuilder builder = new GridPatchBuilder(gridCrsDefinition, prototype);

    // Act
    PatchSet patchSet = builder.build();

    // Assert
    assertEquals(1, patchSet.getPatches().size(),
        "Should create 1 patch for area smaller than cell size");
  }

  @Test
  @DisplayName("Custom GridCRS with geographic coordinates should be properly constructed")
  void testGridWithCustomCrs() {
    // Arrange - Simulate coordinates like in the example: longitude 34-35, latitude -116 to -115
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.valueOf(34));
    when(extents.getTopLeftY()).thenReturn(BigDecimal.valueOf(-116));
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(35));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(-115));

    BigDecimal cellWidth = BigDecimal.valueOf(0.1); // 0.1 degree cells
    EntityPrototype prototype = mock(EntityPrototype.class);
    when(prototype.buildSpatial(any(Entity.class))).thenReturn(mock(MutableEntity.class));

    GridCrsDefinition gridCrsDefinition = new GridCrsDefinition(
        "GRID", "EPSG:4326", extents, cellWidth, "degrees");
    GridPatchBuilder builder = new GridPatchBuilder(gridCrsDefinition, prototype);

    // Act
    PatchSet patchSet = builder.build();

    // Assert
    assertEquals(100, patchSet.getPatches().size(),
        "Should create 10x10=100 patches for 1x1 degree area with 0.1 degree cells");
    assertEquals(cellWidth, patchSet.getSpacing(),
        "Cell width should be preserved in the PatchSet");
  }
}
