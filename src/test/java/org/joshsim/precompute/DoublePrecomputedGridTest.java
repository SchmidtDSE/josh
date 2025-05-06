
package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class DoublePrecomputedGridTest {

    @Mock
    private EngineValueFactory mockFactory;
    @Mock
    private PatchBuilderExtents mockExtents;
    @Mock
    private EngineValue mockEngineValue;

    private DoublePrecomputedGrid grid;
    private final Units testUnits = new Units("meters");
    private final long minTimestep = 0;
    private final long maxTimestep = 10;

    @BeforeEach
    void setUp() {
        when(mockExtents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
        when(mockExtents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
        when(mockExtents.getBottomRightX()).thenReturn(BigDecimal.TEN);
        when(mockExtents.getBottomRightY()).thenReturn(BigDecimal.TEN);

        double[][][] innerValues = new double[11][11][11];
        innerValues[3][2][1] = 45.0;

        grid = new DoublePrecomputedGrid(
            mockFactory,
            mockExtents,
            minTimestep,
            maxTimestep,
            testUnits,
            innerValues
        );
    }

    @Test
    void testGetAt() {
        // Given
        long x = 1;
        long y = 2;
        long timestep = 3;
        double expectedValue = 45.0;
        when(mockFactory.build(BigDecimal.valueOf(expectedValue), testUnits))
            .thenReturn(mockEngineValue);

        // When
        EngineValue result = grid.getAt(x, y, timestep);

        // Then
        assertEquals(mockEngineValue, result);
    }

    @Test
    void testGridDimensions() {
        // Given
        PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
        when(extents.getTopLeftX()).thenReturn(BigDecimal.valueOf(-5));
        when(extents.getTopLeftY()).thenReturn(BigDecimal.valueOf(-5));
        when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(5));
        when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(5));

        // When
        DoublePrecomputedGrid testGrid = new DoublePrecomputedGrid(
            mockFactory,
            extents,
            minTimestep,
            maxTimestep,
            testUnits
        );

        // Then
        assertEquals(true, testGrid.isCompatible(extents, minTimestep, maxTimestep));
    }
}
