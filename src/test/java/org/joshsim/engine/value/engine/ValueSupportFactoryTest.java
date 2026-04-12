package org.joshsim.engine.value.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.DecimalScalar;
import org.joshsim.engine.value.type.DoubleScalar;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.interpret.RecursiveValueResolver;
import org.joshsim.lang.interpret.RecursiveValueResolverFactory;
import org.joshsim.lang.interpret.TimedRecursiveValueResolverFactory;
import org.joshsim.lang.interpret.TimedValueResolver;
import org.joshsim.lang.interpret.ValueResolver;
import org.junit.jupiter.api.Test;


class ValueSupportFactoryTest {

  @Test
  void testParseNumberWithBigDecimal() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory(true);
    String input = "123.456";

    // Act
    EngineValue result = factory.parseNumber(input, Units.EMPTY);

    // Assert
    assertTrue(result instanceof DecimalScalar);
    assertEquals(new BigDecimal("123.456"), result.getAsDecimal());
    assertEquals(Units.EMPTY, result.getUnits());
  }

  @Test
  void testParseNumberWithDouble() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory(false);
    String input = "123.456";

    // Act
    EngineValue result = factory.parseNumber(input, Units.EMPTY);

    // Assert
    assertTrue(result instanceof DoubleScalar);
    assertEquals(123.456, result.getAsDouble());
    assertEquals(Units.EMPTY, result.getUnits());
  }

  @Test
  void testParseNumberWithInvalidInput() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory(true);
    String input = "invalid";

    // Act & Assert
    assertThrows(NumberFormatException.class, () -> factory.parseNumber(input, Units.EMPTY));
  }

  @Test
  void testParseNumberWithBigDecimalEdgeCase() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory(true);
    String input = "0";

    // Act
    EngineValue result = factory.parseNumber(input, Units.EMPTY);

    // Assert
    assertTrue(result instanceof DecimalScalar);
    assertEquals(BigDecimal.ZERO, result.getAsDecimal());
    assertEquals(Units.EMPTY, result.getUnits());
  }

  @Test
  void testParseNumberWithDoubleEdgeCase() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory(false);
    String input = "0";

    // Act
    EngineValue result = factory.parseNumber(input, Units.EMPTY);

    // Assert
    assertTrue(result instanceof DoubleScalar);
    assertEquals(0.0, result.getAsDouble());
    assertEquals(Units.EMPTY, result.getUnits());
  }

  @Test
  void testBuildForNumberWithBigDecimal() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory(true);
    double input = 123.456;

    // Act
    EngineValue result = factory.buildForNumber(input, Units.COUNT);

    // Assert
    assertTrue(result instanceof DecimalScalar);
    assertTrue(
        (new BigDecimal("123.456"))
            .subtract(result.getAsDecimal())
            .abs()
            .doubleValue() < 0.0001
    );
    assertEquals(Units.COUNT, result.getUnits());
  }

  @Test
  void testBuildForNumberWithDouble() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory(false);
    double input = 123.456;

    // Act
    EngineValue result = factory.buildForNumber(input, Units.COUNT);

    // Assert
    assertTrue(result instanceof DoubleScalar);
    assertEquals(123.456, result.getAsDouble(), 0.000001);
    assertEquals(Units.COUNT, result.getUnits());
  }

  @Test
  void testBuildValueResolverReturnsNonNull() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory();

    // Act
    ValueResolver resolver = factory.buildValueResolver("some.path");

    // Assert
    assertNotNull(resolver);
  }

  @Test
  void testBuildValueResolverReturnsValueResolverInterface() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory();

    // Act
    ValueResolver resolver = factory.buildValueResolver("some.path");

    // Assert
    assertTrue(resolver instanceof ValueResolver);
  }

  @Test
  void testBuildValueResolverHasCorrectPath() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory();

    // Act
    ValueResolver resolver = factory.buildValueResolver("some.path");

    // Assert
    assertEquals("some.path", resolver.getPath());
  }

  @Test
  void testTwoArgConstructorWithTimedFactoryProducesTimedValueResolver() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory(
        true,
        new TimedRecursiveValueResolverFactory()
    );

    // Act
    ValueResolver resolver = factory.buildValueResolver("entity.attribute");

    // Assert
    assertTrue(resolver instanceof TimedValueResolver);
  }

  @Test
  void testTwoArgConstructorWithRecursiveFactoryProducesRecursiveValueResolver() {
    // Arrange
    ValueSupportFactory factory = new ValueSupportFactory(
        true,
        new RecursiveValueResolverFactory()
    );

    // Act
    ValueResolver resolver = factory.buildValueResolver("entity.attribute");

    // Assert
    assertTrue(resolver instanceof RecursiveValueResolver);
  }

  @Test
  void testOneBoolArgConstructorStillProducesRecursiveValueResolver() {
    // Arrange - regression guard: existing single-arg constructor must remain unchanged
    ValueSupportFactory factory = new ValueSupportFactory(true);

    // Act
    ValueResolver resolver = factory.buildValueResolver("entity.attribute");

    // Assert
    assertTrue(resolver instanceof RecursiveValueResolver);
  }

}
