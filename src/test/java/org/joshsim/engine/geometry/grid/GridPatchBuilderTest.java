/**
 * Test for building patches in grid space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;
import org.junit.jupiter.api.Test;


/**
 * Test for building patches in grid space.
 */
class GridPatchBuilderTest {

  @Test
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

    GridPatchBuilder builder = new GridPatchBuilder(extents, cellWidth, prototype);

    // Act
    PatchSet patchSet = builder.build();

    // Assert
    assertEquals(25, patchSet.getPatches().size());
  }

  @Test
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

    GridPatchBuilder builder = new GridPatchBuilder(extents, cellWidth, prototype);

    // Act
    PatchSet patchSet = builder.build();

    // Assert
    assertEquals(9, patchSet.getPatches().size());
  }

  @Test
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

    GridPatchBuilder builder = new GridPatchBuilder(extents, cellWidth, prototype);

    // Act
    PatchSet patchSet = builder.build();

    // Assert
    assertEquals(25, patchSet.getPatches().size());
  }

  @Test
  void testBuildValidatesExtents() {
    // Arrange
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.TEN);
    when(extents.getTopLeftY()).thenReturn(BigDecimal.TEN);
    when(extents.getBottomRightX()).thenReturn(BigDecimal.ONE);
    when(extents.getBottomRightY()).thenReturn(BigDecimal.ONE);

    BigDecimal cellWidth = BigDecimal.ONE;
    EntityPrototype prototype = mock(EntityPrototype.class);

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new GridPatchBuilder(extents, cellWidth, prototype)
    );
    assertEquals("Top left X must be less than bottom right X", exception.getMessage());
  }

  @Test
  void testBuildHandlesEmptyPatchArea() {
    // Arrange
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(extents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(1));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(1));

    BigDecimal cellWidth = BigDecimal.valueOf(2);
    EntityPrototype prototype = mock(EntityPrototype.class);

    GridPatchBuilder builder = new GridPatchBuilder(extents, cellWidth, prototype);

    // Act
    PatchSet patchSet = builder.build();

    // Assert
    assertEquals(1, patchSet.getPatches().size());
  }
}
