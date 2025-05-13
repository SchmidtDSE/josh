package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.io.InputGetterStrategy;
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
    PatchBuilderExtentsBuilder extentsBuilder = new PatchBuilderExtentsBuilder();
    extentsBuilder.setTopLeftX(BigDecimal.ZERO);
    extentsBuilder.setTopLeftY(BigDecimal.ZERO);
    extentsBuilder.setBottomRightX(BigDecimal.TWO);
    extentsBuilder.setBottomRightY(BigDecimal.TWO);
    PatchBuilderExtents extents = extentsBuilder.build();
    DoublePrecomputedGrid originalGrid = new DoublePrecomputedGrid(
        factory,
        extents,
        0L,
        2L,
        Units.of("meters"),
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
