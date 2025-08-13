package org.joshsim.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigTest {

  private EngineValueFactory factory;
  private Config config;

  @BeforeEach
  void setUp() {
    factory = new EngineValueFactory(true);
    Map<String, EngineValue> values = new HashMap<>();
    values.put("testVar1", factory.parseNumber("5", Units.METERS));
    values.put("testVar2", factory.parseNumber("10", Units.of("m")));
    values.put("testVar3", factory.parseNumber("15", Units.of("km")));
    config = new Config(values);
  }

  @Test
  void testGetValueExists() {
    // Act
    EngineValue result = config.getValue("testVar1");

    // Assert
    assertEquals(5.0, result.getAsDouble(), 0.0001);
    assertEquals(Units.METERS, result.getUnits());
  }

  @Test
  void testGetValueNotExists() {
    // Act
    EngineValue result = config.getValue("nonExistent");

    // Assert
    assertNull(result);
  }

  @Test
  void testHasValueExists() {
    // Act & Assert
    assertTrue(config.hasValue("testVar1"));
    assertTrue(config.hasValue("testVar2"));
    assertTrue(config.hasValue("testVar3"));
  }

  @Test
  void testHasValueNotExists() {
    // Act & Assert
    assertFalse(config.hasValue("nonExistent"));
    assertFalse(config.hasValue(""));
    assertFalse(config.hasValue(null));
  }

  @Test
  void testEmptyConfig() {
    // Arrange
    Config emptyConfig = new Config(Map.of());

    // Act & Assert
    assertNull(emptyConfig.getValue("anything"));
    assertFalse(emptyConfig.hasValue("anything"));
  }

  @Test
  void testConfigIsImmutable() {
    // Arrange
    Map<String, EngineValue> originalValues = new HashMap<>();
    originalValues.put("original", factory.parseNumber("42", Units.COUNT));
    Config testConfig = new Config(originalValues);

    // Modify the original map
    originalValues.put("modified", factory.parseNumber("100", Units.COUNT));

    // Act & Assert
    assertTrue(testConfig.hasValue("original"));
    assertFalse(testConfig.hasValue("modified")); // Should not be affected by external changes
  }
}