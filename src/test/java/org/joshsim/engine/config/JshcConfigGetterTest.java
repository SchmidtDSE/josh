/**
 * Tests for JshcConfigGetter.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.io.InputGetterStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test suite for JshcConfigGetter.
 */
public class JshcConfigGetterTest {

  private InputGetterStrategy mockInputStrategy;
  private EngineValueFactory valueFactory;
  private JshcConfigGetter getter;

  /**
   * Set up test fixtures.
   */
  @BeforeEach
  public void setUp() {
    mockInputStrategy = mock(InputGetterStrategy.class);
    valueFactory = new EngineValueFactory();
    getter = new JshcConfigGetter(mockInputStrategy, valueFactory);
  }

  @Test
  public void testGetConfig() throws IOException {
    // Setup
    String configContent = "testVar = 5 m\nanotherVar = 10 km";
    InputStream inputStream = new ByteArrayInputStream(
        configContent.getBytes(StandardCharsets.UTF_8));
    when(mockInputStrategy.open("test.jshc")).thenReturn(inputStream);

    // Execute
    Config config = getter.getConfig("test");

    // Verify
    assertNotNull(config);
    assertNotNull(config.getValue("testVar"));
    assertEquals(5.0, config.getValue("testVar").getAsDouble(), 0.001);
    assertEquals(Units.of("m"), config.getValue("testVar").getUnits());

    assertNotNull(config.getValue("anotherVar"));
    assertEquals(10.0, config.getValue("anotherVar").getAsDouble(), 0.001);
    assertEquals(Units.of("km"), config.getValue("anotherVar").getUnits());

    verify(mockInputStrategy).open("test.jshc");
  }

  @Test
  public void testGetConfigWithJshcExtension() throws IOException {
    // Setup
    String configContent = "testVar = 5 m";
    InputStream inputStream = new ByteArrayInputStream(
        configContent.getBytes(StandardCharsets.UTF_8));
    when(mockInputStrategy.open("test.jshc")).thenReturn(inputStream);

    // Execute
    Config config = getter.getConfig("test.jshc");

    // Verify
    assertNotNull(config);
    assertNotNull(config.getValue("testVar"));
    assertEquals(5.0, config.getValue("testVar").getAsDouble(), 0.001);
    assertEquals(Units.of("m"), config.getValue("testVar").getUnits());
    verify(mockInputStrategy).open("test.jshc");
  }

  @Test
  public void testCaching() throws IOException {
    // Setup
    String configContent = "testVar = 5 m";
    InputStream inputStream = new ByteArrayInputStream(
        configContent.getBytes(StandardCharsets.UTF_8));
    when(mockInputStrategy.open("test.jshc")).thenReturn(inputStream);

    // Execute - get config twice
    Config config1 = getter.getConfig("test");
    Config config2 = getter.getConfig("test");

    // Verify - should be the same cached instance
    assertSame(config1, config2);
    // Should only load from input strategy once due to caching
    verify(mockInputStrategy, times(1)).open("test.jshc");
  }

  @Test
  public void testIoException() throws IOException {
    // Setup
    when(mockInputStrategy.open("test.jshc")).thenThrow(new RuntimeException("File not found"));

    // Execute and verify
    RuntimeException exception = assertThrows(
        RuntimeException.class,
        () -> getter.getConfig("test")
    );
    assertEquals("File not found", exception.getMessage());
  }

  @Test
  public void testParseError() throws IOException {
    // Setup - invalid config content
    String invalidContent = "invalid = = = content";
    InputStream inputStream = new ByteArrayInputStream(
        invalidContent.getBytes(StandardCharsets.UTF_8));
    when(mockInputStrategy.open("test.jshc")).thenReturn(inputStream);

    // Execute and verify - should throw exception from parser
    assertThrows(
        IllegalArgumentException.class,
        () -> getter.getConfig("test")
    );
  }
}
