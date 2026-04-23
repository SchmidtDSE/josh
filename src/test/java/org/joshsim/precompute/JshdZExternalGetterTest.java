package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.lang.io.InputGetterStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class JshdZExternalGetterTest {

  private JshdZExternalGetter getter;
  private ValueSupportFactory factory;

  @Mock
  private InputGetterStrategy mockInputStrategy;

  @BeforeEach
  void setUp() {
    factory = new ValueSupportFactory();
    getter = new JshdZExternalGetter(mockInputStrategy, factory);
  }

  @Test
  void testGetResource() throws IOException {
    PatchBuilderExtentsBuilder extentsBuilder = new PatchBuilderExtentsBuilder();
    extentsBuilder.setTopLeftX(BigDecimal.ZERO);
    extentsBuilder.setTopLeftY(BigDecimal.ZERO);
    extentsBuilder.setBottomRightX(BigDecimal.TWO);
    extentsBuilder.setBottomRightY(BigDecimal.TWO);
    PatchBuilderExtents extents = extentsBuilder.build();

    DoublePrecomputedGrid original = new DoublePrecomputedGrid(
        factory, extents, 0L, 2L, Units.of("meters"), new double[3][3][3]
    );

    // Compress the grid as a .jshdz byte stream
    XzGridSerializationStrategy strategy = new XzGridSerializationStrategy(
        new BinaryGridSerializationStrategy(factory)
    );
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    strategy.serialize(original, out);

    when(mockInputStrategy.open("test.jshdz"))
        .thenReturn(new ByteArrayInputStream(out.toByteArray()));

    DoublePrecomputedGrid result = (DoublePrecomputedGrid) getter.getResource("test.jshdz");

    assertEquals(original.getMinTimestep(), result.getMinTimestep());
    assertEquals(original.getMaxTimestep(), result.getMaxTimestep());
    assertEquals(original.getUnits(), result.getUnits());
    assertEquals(original.getMinX(), result.getMinX());
    assertEquals(original.getMaxX(), result.getMaxX());
  }

  @Test
  void testGetResourceRejectsNonJshdz() {
    assertThrows(IllegalArgumentException.class, () -> getter.getResource("test.jshd"));
  }

  @Test
  void testGetResourceRejectsNoExtension() {
    assertThrows(IllegalArgumentException.class, () -> getter.getResource("test"));
  }

}
