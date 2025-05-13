
package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GridCombinerTest {
  private EngineGeometryFactory geometryFactory;
  private EngineValueFactory valueFactory;
  private GridCombiner gridCombiner;
  private final Units testUnits = Units.of("meters");

  private DoublePrecomputedGrid leftGrid;
  private DoublePrecomputedGrid rightGrid;

  @BeforeEach
  void setUp() {
    geometryFactory = new GridGeometryFactory();
    valueFactory = EngineValueFactory.getDefault();
    gridCombiner = new GridCombiner(geometryFactory);

    // Create extents for left grid (0,0 to 2,2)
    PatchBuilderExtents leftExtents = new PatchBuilderExtentsBuilder()
        .setTopLeftX(BigDecimal.ZERO)
        .setTopLeftY(BigDecimal.valueOf(2))
        .setBottomRightX(BigDecimal.valueOf(2))
        .setBottomRightY(BigDecimal.ZERO)
        .build();

    // Create extents for right grid (1,1 to 3,3)
    PatchBuilderExtents rightExtents = new PatchBuilderExtentsBuilder()
        .setTopLeftX(BigDecimal.ONE)
        .setTopLeftY(BigDecimal.valueOf(3))
        .setBottomRightX(BigDecimal.valueOf(3))
        .setBottomRightY(BigDecimal.ONE)
        .build();

    // Create left grid and populate with values 1-4
    leftGrid = new DoublePrecomputedGridBuilder()
        .setEngineValueFactory(valueFactory)
        .setExtents(leftExtents)
        .setTimestepRange(0, 2)
        .setUnits(testUnits)
        .build();

    // Create right grid and populate with values 5-8
    rightGrid = new DoublePrecomputedGridBuilder()
        .setEngineValueFactory(valueFactory)
        .setExtents(rightExtents)
        .setTimestepRange(1, 3)
        .setUnits(testUnits)
        .build();

    // Populate left grid
    for (long x = 0; x <= 2; x++) {
      for (long y = 0; y <= 2; y++) {
        for (long t = 0; t <= 2; t++) {
          leftGrid.setAt(x, y, t, 1.0);
        }
      }
    }

    // Populate right grid with higher values
    for (long x = 1; x <= 3; x++) {
      for (long y = 1; y <= 3; y++) {
        for (long t = 1; t <= 3; t++) {
          rightGrid.setAt(x, y, t, 2.0);
        }
      }
    }
  }

  @Test
  void testCombineBasicProperties() {
    DataGridLayer combined = gridCombiner.combine(leftGrid, rightGrid);

    assertEquals(0L, combined.getMinX());
    assertEquals(3L, combined.getMaxX());
    assertEquals(0L, combined.getMinY());
    assertEquals(3L, combined.getMaxY());
    assertEquals(0L, combined.getMinTimestep());
    assertEquals(3L, combined.getMaxTimestep());
    assertEquals(testUnits, combined.getUnits());
  }

  @Test
  void testCombineValues() {
    DataGridLayer combined = gridCombiner.combine(leftGrid, rightGrid);

    // Test a point that should come from the left grid
    EngineGeometry geometry1 = geometryFactory.createPoint(BigDecimal.ZERO, BigDecimal.ZERO);
    EngineValue leftValue = combined.getAt(new GeoKey(Optional.of(geometry1), ""), 0);
    assertEquals(1.0, leftValue.getAsDecimal().doubleValue(), 0.001);

    // Test a point that should be overwritten by the right grid
    EngineGeometry geometry2 = geometryFactory.createPoint(BigDecimal.TWO, BigDecimal.TWO);
    EngineValue rightValue = combined.getAt(new GeoKey(Optional.of(geometry2), ""), 2);
    assertEquals(2.0, rightValue.getAsDecimal().doubleValue(), 0.001);
  }

  @Test
  void testCombineWithDifferentUnits() {
    // Create a new right grid with different units
    PatchBuilderExtents rightExtents = new PatchBuilderExtentsBuilder()
        .setTopLeftX(BigDecimal.ONE)
        .setTopLeftY(BigDecimal.valueOf(3))
        .setBottomRightX(BigDecimal.valueOf(3))
        .setBottomRightY(BigDecimal.ONE)
        .build();

    DoublePrecomputedGrid differentUnitsGrid = new DoublePrecomputedGridBuilder()
        .setEngineValueFactory(valueFactory)
        .setExtents(rightExtents)
        .setTimestepRange(1, 3)
        .setUnits(Units.of("feet"))
        .build();

    assertThrows(IllegalArgumentException.class, () -> {
      gridCombiner.combine(leftGrid, differentUnitsGrid);
    });
  }
}
