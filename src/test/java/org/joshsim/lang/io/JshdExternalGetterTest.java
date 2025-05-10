
package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.precompute.JshdUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JshdExternalGetterTest {

    private JshdExternalGetter getter;
    private EngineValueFactory factory;
    
    @Mock
    private InputGetterStrategy mockInputStrategy;
    
    @BeforeEach
    void setUp() {
        factory = EngineValueFactory.getDefault();
        getter = new JshdExternalGetter(mockInputStrategy, factory);
    }

    @Test
    void testGetResource() throws IOException {
        // Given
        PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
        DoublePrecomputedGrid originalGrid = new DoublePrecomputedGrid(
            factory,
            extents,
            0L,
            2L,
            new Units("meters"),
            new double[3][3][3]
        );
        byte[] gridBytes = JshdUtil.serializeToBytes(originalGrid);
        
        when(mockInputStrategy.open("test.jshd"))
            .thenReturn(new ByteArrayInputStream(gridBytes));

        // When
        DoublePrecomputedGrid result = (DoublePrecomputedGrid) getter.getResource("test.jshd");

        // Then
        assertEquals(originalGrid.getMinTimestep(), result.getMinTimestep());
        assertEquals(originalGrid.getMaxTimestep(), result.getMaxTimestep());
        assertEquals(originalGrid.getUnits(), result.getUnits());
    }

    @Test
    void testGetResourceWithInvalidExtension() {
        assertThrows(IllegalArgumentException.class, () -> {
            getter.getResource("test.txt");
        });
    }
}
