package org.joshsim.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigInputParserTest {

  private ConfigInputParser parser;
  private EngineValueFactory factory;

  @BeforeEach
  void setUp() {
    factory = new EngineValueFactory(true);
    parser = new ConfigInputParser(factory);
  }

  @Test
  void testParseExampleJshcFile() {
    // Arrange
    String input = "# Group 1\n"
        + "testVar1 = 5 meters\n"
        + "testVar2 =  10m\n"
        + "\n"
        + "# Group 2\n"
        + "testVar3  = 15  km";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertTrue(config.hasValue("testVar1"));
    assertTrue(config.hasValue("testVar2"));
    assertTrue(config.hasValue("testVar3"));

    assertEquals(5.0, config.getValue("testVar1").getAsDouble(), 0.0001);
    assertEquals(Units.METERS, config.getValue("testVar1").getUnits());

    assertEquals(10.0, config.getValue("testVar2").getAsDouble(), 0.0001);
    assertEquals(Units.of("m"), config.getValue("testVar2").getUnits());

    assertEquals(15.0, config.getValue("testVar3").getAsDouble(), 0.0001);
    assertEquals(Units.of("km"), config.getValue("testVar3").getUnits());
  }

  @Test
  void testParseEmptyInput() {
    // Act
    Config config = parser.parse("");

    // Assert
    assertFalse(config.hasValue("anything"));
  }

  @Test
  void testParseWhitespaceOnly() {
    // Act
    Config config = parser.parse("   \n\t\r\n  ");

    // Assert
    assertFalse(config.hasValue("anything"));
  }

  @Test
  void testParseCommentsOnly() {
    // Arrange
    String input = "# This is a comment\n"
        + "# Another comment\n"
        + "   # Indented comment";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertFalse(config.hasValue("anything"));
  }

  @Test
  void testParseSimpleVariable() {
    // Arrange
    String input = "myVar = 42 count";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertTrue(config.hasValue("myVar"));
    assertEquals(42.0, config.getValue("myVar").getAsDouble(), 0.0001);
    assertEquals(Units.COUNT, config.getValue("myVar").getUnits());
  }

  @Test
  void testParseVariableWithoutUnits() {
    // Arrange
    String input = "number = 3.14";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertTrue(config.hasValue("number"));
    assertEquals(3.14, config.getValue("number").getAsDouble(), 0.0001);
    assertEquals(Units.EMPTY, config.getValue("number").getUnits());
  }

  @Test
  void testParseVariableWithComplexUnits() {
    // Arrange
    String input = "velocity = 5.5 meters / second";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertTrue(config.hasValue("velocity"));
    assertEquals(5.5, config.getValue("velocity").getAsDouble(), 0.0001);
    assertEquals(Units.of("meters / second"), config.getValue("velocity").getUnits());
  }

  @Test
  void testParseWithVariousWhitespace() {
    // Arrange
    String input = "   var1   =   42   meters   \n"
        + "\tvar2\t=\t3.14\tcount\t\n"
        + "var3= 100 km";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertTrue(config.hasValue("var1"));
    assertTrue(config.hasValue("var2"));
    assertTrue(config.hasValue("var3"));
    assertEquals(42.0, config.getValue("var1").getAsDouble(), 0.0001);
    assertEquals(3.14, config.getValue("var2").getAsDouble(), 0.0001);
    assertEquals(100.0, config.getValue("var3").getAsDouble(), 0.0001);
  }

  @Test
  void testParseWithInlineComments() {
    // Arrange
    String input = "var1 = 42 meters # This is a comment\n"
        + "var2 = 3.14 # Another comment";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertTrue(config.hasValue("var1"));
    assertTrue(config.hasValue("var2"));
    assertEquals(42.0, config.getValue("var1").getAsDouble(), 0.0001);
    assertEquals(Units.METERS, config.getValue("var1").getUnits());
    assertEquals(3.14, config.getValue("var2").getAsDouble(), 0.0001);
    assertEquals(Units.EMPTY, config.getValue("var2").getUnits());
  }

  @Test
  void testParseWithEmptyLines() {
    // Arrange
    String input = "\n\nvar1 = 42\n\n\nvar2 = 3.14\n\n";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertTrue(config.hasValue("var1"));
    assertTrue(config.hasValue("var2"));
  }

  @Test
  void testParseEndingWithoutNewline() {
    // Arrange
    String input = "var1 = 42 meters";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertTrue(config.hasValue("var1"));
    assertEquals(42.0, config.getValue("var1").getAsDouble(), 0.0001);
    assertEquals(Units.METERS, config.getValue("var1").getUnits());
  }

  @Test
  void testParseInvalidVariableName() {
    // Arrange
    String input = "123invalid = 42";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> parser.parse(input)
    );
    assertTrue(exception.getMessage().contains("Invalid character"));
    assertTrue(exception.getMessage().contains("line 1"));
  }

  @Test
  void testParseInvalidCharacterInVariableName() {
    // Arrange
    String input = "var@name = 42";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> parser.parse(input)
    );
    assertTrue(exception.getMessage().contains("Invalid character"));
    assertTrue(exception.getMessage().contains("variable name"));
  }

  @Test
  void testParseMissingEquals() {
    // Arrange
    String input = "variableName 42";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> parser.parse(input)
    );
    assertTrue(exception.getMessage().contains("Missing equals sign"));
  }

  @Test
  void testParseMissingValue() {
    // Arrange
    String input = "variableName = \n";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> parser.parse(input)
    );
    assertTrue(exception.getMessage().contains("Missing value"));
  }

  @Test
  void testParseEmptyValue() {
    // Arrange
    String input = "variableName =    \n";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> parser.parse(input)
    );
    assertTrue(exception.getMessage().contains("Missing value after equals sign"));
  }

  @Test
  void testParseInvalidNumber() {
    // Arrange
    String input = "variableName = notanumber meters";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> parser.parse(input)
    );
    assertTrue(exception.getMessage().contains("Invalid value format"));
  }

  @Test
  void testParseInvalidUnits() {
    // Arrange
    String input = "variableName = 42 invalid / invalid / invalid";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> parser.parse(input)
    );
    assertTrue(exception.getMessage().contains("Invalid units")
        || exception.getMessage().contains("denominator"));
  }

  @Test
  void testParseEmptyVariableName() {
    // Arrange
    String input = " = 42";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> parser.parse(input)
    );
    assertTrue(exception.getMessage().contains("Invalid character"));
  }

  @Test
  void testParseMultipleVariables() {
    // Arrange
    String input = "var1 = 1 count\n"
        + "var2 = 2.5 meters\n"
        + "var3 = 100 km\n"
        + "var4 = 0";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertTrue(config.hasValue("var1"));
    assertTrue(config.hasValue("var2"));
    assertTrue(config.hasValue("var3"));
    assertTrue(config.hasValue("var4"));
    
    assertEquals(1.0, config.getValue("var1").getAsDouble(), 0.0001);
    assertEquals(2.5, config.getValue("var2").getAsDouble(), 0.0001);
    assertEquals(100.0, config.getValue("var3").getAsDouble(), 0.0001);
    assertEquals(0.0, config.getValue("var4").getAsDouble(), 0.0001);
  }

  @Test
  void testParseInvalidVariableNameWithUnderscore() {
    // Arrange
    String input = "test_var = 42";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> parser.parse(input)
    );
    assertTrue(exception.getMessage().contains("Invalid character"));
    assertTrue(exception.getMessage().contains("variable name"));
  }

  @Test
  void testParseNegativeNumbers() {
    // Arrange
    String input = "negative = -42.5 meters";

    // Act
    Config config = parser.parse(input);

    // Assert
    assertTrue(config.hasValue("negative"));
    assertEquals(-42.5, config.getValue("negative").getAsDouble(), 0.0001);
    assertEquals(Units.METERS, config.getValue("negative").getUnits());
  }


  @Test
  void testParseLineNumberInErrorMessages() {
    // Arrange
    String input = "var1 = 42\n"
        + "var2 = 3.14\n"
        + "invalid@name = 100";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, 
        () -> parser.parse(input)
    );
    assertTrue(exception.getMessage().contains("line 3"));
  }
}