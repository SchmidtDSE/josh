package org.joshsim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.StringJoiner;
import org.joshsim.engine.config.Config;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigInterpreterTest {

  private ConfigInterpreter interpreter;
  private EngineValueFactory factory;

  @BeforeEach
  void setUp() {
    factory = new EngineValueFactory(true);
    interpreter = new ConfigInterpreter();
  }

  @Test
  void testParseExampleJshcFile() {
    // Arrange
    StringJoiner joiner = new StringJoiner("\n");
    joiner.add("# Group 1");
    joiner.add("testVar1 = 5 meters");
    joiner.add("testVar2 =  10 m");
    joiner.add("");
    joiner.add("# Group 2");
    joiner.add("testVar3  = 15  km");
    String input = joiner.toString();

    // Act
    Config config = interpreter.interpret(input, factory);

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
    Config config = interpreter.interpret("", factory);

    // Assert
    assertFalse(config.hasValue("anything"));
  }

  @Test
  void testParseWhitespaceOnly() {
    // Act
    Config config = interpreter.interpret("   \n\t\r\n  ", factory);

    // Assert
    assertFalse(config.hasValue("anything"));
  }

  @Test
  void testParseCommentsOnly() {
    // Arrange
    StringJoiner joiner = new StringJoiner("\n");
    joiner.add("# This is a comment");
    joiner.add("# Another comment");
    joiner.add("   # Indented comment");
    String input = joiner.toString();

    // Act
    Config config = interpreter.interpret(input, factory);

    // Assert
    assertFalse(config.hasValue("anything"));
  }

  @Test
  void testParseSimpleVariable() {
    // Arrange
    String input = "myVar = 42 count";

    // Act
    Config config = interpreter.interpret(input, factory);

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
    Config config = interpreter.interpret(input, factory);

    // Assert
    assertTrue(config.hasValue("number"));
    assertEquals(3.14, config.getValue("number").getAsDouble(), 0.0001);
    assertEquals(Units.EMPTY, config.getValue("number").getUnits());
  }

  @Test
  void testParseVariableWithComplexUnits() {
    // Arrange
    String input = "velocity = 5.5 meters/second";

    // Act
    Config config = interpreter.interpret(input, factory);

    // Assert
    assertTrue(config.hasValue("velocity"));
    assertEquals(5.5, config.getValue("velocity").getAsDouble(), 0.0001);
    assertEquals(Units.of("meters/second"), config.getValue("velocity").getUnits());
  }

  @Test
  void testParseWithVariousWhitespace() {
    // Arrange
    StringJoiner joiner = new StringJoiner("\n");
    joiner.add("   var1   =   42   meters   ");
    joiner.add("\tvar2\t=\t3.14\tcount\t");
    joiner.add("var3= 100 km");
    String input = joiner.toString();

    // Act
    Config config = interpreter.interpret(input, factory);

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
    StringJoiner joiner = new StringJoiner("\n");
    joiner.add("var1 = 42 meters # This is a comment");
    joiner.add("var2 = 3.14 # Another comment");
    String input = joiner.toString();

    // Act
    Config config = interpreter.interpret(input, factory);

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
    Config config = interpreter.interpret(input, factory);

    // Assert
    assertTrue(config.hasValue("var1"));
    assertTrue(config.hasValue("var2"));
  }

  @Test
  void testParseEndingWithoutNewline() {
    // Arrange
    String input = "var1 = 42 meters";

    // Act
    Config config = interpreter.interpret(input, factory);

    // Assert
    assertTrue(config.hasValue("var1"));
    assertEquals(42.0, config.getValue("var1").getAsDouble(), 0.0001);
    assertEquals(Units.METERS, config.getValue("var1").getUnits());
  }

  @Test
  void testParseInvalidInput() {
    // Arrange - invalid syntax should cause parsing error
    String input = "123invalid = 42";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> interpreter.interpret(input, factory)
    );
    assertTrue(exception.getMessage().contains("Failed to parse"));
  }

  @Test
  void testParseInvalidNumber() {
    // Arrange
    String input = "variableName = notanumber meters";

    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> interpreter.interpret(input, factory)
    );
    assertTrue(exception.getMessage().contains("Failed to parse"));
  }

  @Test
  void testParseMultipleVariables() {
    // Arrange
    StringJoiner joiner = new StringJoiner("\n");
    joiner.add("var1 = 1 count");
    joiner.add("var2 = 2.5 meters");
    joiner.add("var3 = 100 km");
    joiner.add("var4 = 0");
    String input = joiner.toString();

    // Act
    Config config = interpreter.interpret(input, factory);

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
  void testParseNegativeNumbers() {
    // Arrange
    String input = "negative = -42.5 meters";

    // Act
    Config config = interpreter.interpret(input, factory);

    // Assert
    assertTrue(config.hasValue("negative"));
    assertEquals(-42.5, config.getValue("negative").getAsDouble(), 0.0001);
    assertEquals(Units.METERS, config.getValue("negative").getUnits());
  }
}