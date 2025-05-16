package org.joshsim.engine.value.engine;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.DecimalScalar;
import org.joshsim.engine.value.type.DoubleScalar;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;


class EngineValueFactoryTest {

  @Test
  void testParseNumberWithBigDecimal() {
    // Arrange
    EngineValueFactory factory = new EngineValueFactory(true);
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
    EngineValueFactory factory = new EngineValueFactory(false);
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
    EngineValueFactory factory = new EngineValueFactory(true);
    String input = "invalid";

    // Act & Assert
    assertThrows(NumberFormatException.class, () -> factory.parseNumber(input, Units.EMPTY));
  }

  @Test
  void testParseNumberWithBigDecimalEdgeCase() {
    // Arrange
    EngineValueFactory factory = new EngineValueFactory(true);
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
    EngineValueFactory factory = new EngineValueFactory(false);
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
    EngineValueFactory factory = new EngineValueFactory(true);
    double input = 123.456;

    // Act
    EngineValue result = factory.buildForNumber(input, Units.COUNT);

    // Assert
    assertTrue(result instanceof DecimalScalar);
    assertEquals(new BigDecimal("123.456"), result.getAsDecimal());
    assertEquals(Units.COUNT, result.getUnits());
  }

  @Test
  void testBuildForNumberWithDouble() {
    // Arrange
    EngineValueFactory factory = new EngineValueFactory(false);
    double input = 123.456;

    // Act
    EngineValue result = factory.buildForNumber(input, Units.COUNT);

    // Assert
    assertTrue(result instanceof DoubleScalar);
    assertEquals(123.456, result.getAsDouble());
    assertEquals(Units.COUNT, result.getUnits());
  }

}
