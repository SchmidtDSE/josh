package org.joshsim.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigBuilderTest {

  private EngineValueFactory factory;
  private ConfigBuilder builder;

  @BeforeEach
  void setUp() {
    factory = new EngineValueFactory(true);
    builder = new ConfigBuilder();
  }

  @Test
  void testAddValueAndBuild() {
    // Arrange
    EngineValue value1 = factory.parseNumber("5", Units.METERS);
    EngineValue value2 = factory.parseNumber("10", Units.of("km"));

    // Act
    Config config = builder
        .addValue("var1", value1)
        .addValue("var2", value2)
        .build();

    // Assert
    assertNotNull(config);
    assertTrue(config.hasValue("var1"));
    assertTrue(config.hasValue("var2"));
    assertEquals(5.0, config.getValue("var1").getAsDouble(), 0.0001);
    assertEquals(10.0, config.getValue("var2").getAsDouble(), 0.0001);
    assertEquals(Units.METERS, config.getValue("var1").getUnits());
    assertEquals(Units.of("km"), config.getValue("var2").getUnits());
  }

  @Test
  void testAddValueReturnsBuilder() {
    // Arrange
    EngineValue value = factory.parseNumber("42", Units.COUNT);

    // Act
    ConfigBuilder result = builder.addValue("test", value);

    // Assert
    assertSame(builder, result); // Should return the same instance for method chaining
  }

  @Test
  void testEmptyBuilder() {
    // Act
    Config config = builder.build();

    // Assert
    assertNotNull(config);
    assertFalse(config.hasValue("anything"));
    assertNull(config.getValue("anything"));
  }

  @Test
  void testOverwriteValue() {
    // Arrange
    EngineValue value1 = factory.parseNumber("5", Units.METERS);
    EngineValue value2 = factory.parseNumber("10", Units.METERS);

    // Act
    Config config = builder
        .addValue("var", value1)
        .addValue("var", value2) // Overwrite
        .build();

    // Assert
    assertTrue(config.hasValue("var"));
    // Should have the second value
    assertEquals(10.0, config.getValue("var").getAsDouble(), 0.0001);
  }

  @Test
  void testMultipleBuilds() {
    // Arrange
    EngineValue value1 = factory.parseNumber("5", Units.METERS);
    builder.addValue("var1", value1);

    // Act
    Config config1 = builder.build();

    EngineValue value2 = factory.parseNumber("10", Units.of("km"));
    builder.addValue("var2", value2);
    Config config2 = builder.build();

    // Assert
    // First config should have only var1
    assertTrue(config1.hasValue("var1"));
    assertFalse(config1.hasValue("var2"));

    // Second config should have both (builder accumulates)
    assertTrue(config2.hasValue("var1"));
    assertTrue(config2.hasValue("var2"));
  }

  @Test
  void testChainedMethodCalls() {
    // Arrange
    EngineValue value1 = factory.parseNumber("1", Units.COUNT);
    EngineValue value2 = factory.parseNumber("2", Units.COUNT);
    EngineValue value3 = factory.parseNumber("3", Units.COUNT);

    // Act
    Config config = builder
        .addValue("one", value1)
        .addValue("two", value2)
        .addValue("three", value3)
        .build();

    // Assert
    assertTrue(config.hasValue("one"));
    assertTrue(config.hasValue("two"));
    assertTrue(config.hasValue("three"));
    assertEquals(1.0, config.getValue("one").getAsDouble(), 0.0001);
    assertEquals(2.0, config.getValue("two").getAsDouble(), 0.0001);
    assertEquals(3.0, config.getValue("three").getAsDouble(), 0.0001);
  }

  @Test
  void testCombineBasic() {
    // Arrange
    EngineValue value1 = factory.parseNumber("5", Units.METERS);
    EngineValue value2 = factory.parseNumber("10", Units.COUNT);
    EngineValue value3 = factory.parseNumber("15", Units.of("kg"));

    ConfigBuilder builder1 = new ConfigBuilder().addValue("var1", value1);
    ConfigBuilder builder2 = new ConfigBuilder().addValue("var2", value2);

    // Act
    ConfigBuilder combined = builder1.combine(builder2);
    Config config = combined.build();

    // Assert
    assertTrue(config.hasValue("var1"));
    assertTrue(config.hasValue("var2"));
    assertEquals(5.0, config.getValue("var1").getAsDouble(), 0.0001);
    assertEquals(10.0, config.getValue("var2").getAsDouble(), 0.0001);
  }

  @Test
  void testCombineWithOverride() {
    // Arrange
    EngineValue value1 = factory.parseNumber("5", Units.METERS);
    EngineValue value2 = factory.parseNumber("10", Units.METERS);

    ConfigBuilder builder1 = new ConfigBuilder().addValue("var", value1);
    ConfigBuilder builder2 = new ConfigBuilder().addValue("var", value2);

    // Act
    ConfigBuilder combined = builder1.combine(builder2);
    Config config = combined.build();

    // Assert
    assertTrue(config.hasValue("var"));
    // Should have the value from builder2 (the other builder)
    assertEquals(10.0, config.getValue("var").getAsDouble(), 0.0001);
  }

  @Test
  void testCombineEmpty() {
    // Arrange
    EngineValue value1 = factory.parseNumber("5", Units.METERS);
    ConfigBuilder builder1 = new ConfigBuilder().addValue("var1", value1);
    ConfigBuilder emptyBuilder = new ConfigBuilder();

    // Act
    ConfigBuilder combined = builder1.combine(emptyBuilder);
    Config config = combined.build();

    // Assert
    assertTrue(config.hasValue("var1"));
    assertEquals(5.0, config.getValue("var1").getAsDouble(), 0.0001);
  }

  @Test
  void testCombineEmptyWithNonEmpty() {
    // Arrange
    EngineValue value2 = factory.parseNumber("10", Units.COUNT);
    ConfigBuilder emptyBuilder = new ConfigBuilder();
    ConfigBuilder builder2 = new ConfigBuilder().addValue("var2", value2);

    // Act
    ConfigBuilder combined = emptyBuilder.combine(builder2);
    Config config = combined.build();

    // Assert
    assertTrue(config.hasValue("var2"));
    assertEquals(10.0, config.getValue("var2").getAsDouble(), 0.0001);
  }

  @Test
  void testCombineDoesNotModifyOriginal() {
    // Arrange
    EngineValue value1 = factory.parseNumber("5", Units.METERS);
    EngineValue value2 = factory.parseNumber("10", Units.COUNT);

    ConfigBuilder builder1 = new ConfigBuilder().addValue("var1", value1);
    ConfigBuilder builder2 = new ConfigBuilder().addValue("var2", value2);

    // Act - combine and immediately test
    final ConfigBuilder combined = builder1.combine(builder2);
    final Config configCombined = combined.build();

    // Assert
    // Original builders should only have their original values
    Config config1 = builder1.build();
    Config config2 = builder2.build();

    assertTrue(config1.hasValue("var1"));
    assertFalse(config1.hasValue("var2"));

    assertFalse(config2.hasValue("var1"));
    assertTrue(config2.hasValue("var2"));

    // Test the combined config
    assertTrue(configCombined.hasValue("var1"));
    assertTrue(configCombined.hasValue("var2"));
  }
}
