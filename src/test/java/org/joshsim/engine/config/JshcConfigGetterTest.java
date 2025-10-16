/**
 * Tests for JshcConfigGetter.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
    when(mockInputStrategy.exists("test.jshc")).thenReturn(true);
    when(mockInputStrategy.open("test.jshc")).thenReturn(inputStream);

    // Execute
    Optional<Config> configOpt = getter.getConfig("test.jshc");

    // Verify
    assertTrue(configOpt.isPresent());
    Config config = configOpt.get();
    assertNotNull(config.getValue("testVar"));
    assertEquals(5.0, config.getValue("testVar").getAsDouble(), 0.001);
    assertEquals(Units.of("m"), config.getValue("testVar").getUnits());

    assertNotNull(config.getValue("anotherVar"));
    assertEquals(10.0, config.getValue("anotherVar").getAsDouble(), 0.001);
    assertEquals(Units.of("km"), config.getValue("anotherVar").getUnits());

    verify(mockInputStrategy).exists("test.jshc");
    verify(mockInputStrategy).open("test.jshc");
  }

  @Test
  public void testGetConfigWithJshcExtension() throws IOException {
    // Setup
    String configContent = "testVar = 5 m";
    InputStream inputStream = new ByteArrayInputStream(
        configContent.getBytes(StandardCharsets.UTF_8));
    when(mockInputStrategy.exists("test.jshc")).thenReturn(true);
    when(mockInputStrategy.open("test.jshc")).thenReturn(inputStream);

    // Execute
    Optional<Config> configOpt = getter.getConfig("test.jshc");

    // Verify
    assertTrue(configOpt.isPresent());
    Config config = configOpt.get();
    assertNotNull(config.getValue("testVar"));
    assertEquals(5.0, config.getValue("testVar").getAsDouble(), 0.001);
    assertEquals(Units.of("m"), config.getValue("testVar").getUnits());
    verify(mockInputStrategy).exists("test.jshc");
    verify(mockInputStrategy).open("test.jshc");
  }

  @Test
  public void testCaching() throws IOException {
    // Setup
    String configContent = "testVar = 5 m";
    InputStream inputStream = new ByteArrayInputStream(
        configContent.getBytes(StandardCharsets.UTF_8));
    when(mockInputStrategy.exists("test.jshc")).thenReturn(true);
    when(mockInputStrategy.open("test.jshc")).thenReturn(inputStream);

    // Execute - get config twice
    Optional<Config> config1 = getter.getConfig("test.jshc");
    Optional<Config> config2 = getter.getConfig("test.jshc");

    // Verify - should be the same cached instance
    assertTrue(config1.isPresent());
    assertTrue(config2.isPresent());
    assertSame(config1.get(), config2.get());
    // Should only check existence and load from input strategy once due to caching
    verify(mockInputStrategy, times(1)).exists("test.jshc");
    verify(mockInputStrategy, times(1)).open("test.jshc");
  }

  @Test
  public void testFileDoesNotExist() throws IOException {
    // Setup
    when(mockInputStrategy.exists("test.jshc")).thenReturn(false);

    // Execute
    Optional<Config> configOpt = getter.getConfig("test.jshc");

    // Verify - should return empty Optional when file doesn't exist
    assertFalse(configOpt.isPresent());
    verify(mockInputStrategy).exists("test.jshc");
    verify(mockInputStrategy, times(0)).open("test.jshc");
  }

  @Test
  public void testIoException() throws IOException {
    // Setup - create an InputStream that throws IOException when read
    InputStream failingStream = mock(InputStream.class);
    when(failingStream.readAllBytes()).thenThrow(new IOException("Read error"));
    when(mockInputStrategy.exists("test.jshc")).thenReturn(true);
    when(mockInputStrategy.open("test.jshc")).thenReturn(failingStream);

    // Execute
    Optional<Config> configOpt = getter.getConfig("test.jshc");

    // Verify - should return empty Optional on IO error
    assertFalse(configOpt.isPresent());
    verify(mockInputStrategy).exists("test.jshc");
    verify(mockInputStrategy).open("test.jshc");
  }

  @Test
  public void testParseError() throws IOException {
    // Setup - invalid config content
    String invalidContent = "invalid = = = content";
    InputStream inputStream = new ByteArrayInputStream(
        invalidContent.getBytes(StandardCharsets.UTF_8));
    when(mockInputStrategy.exists("test.jshc")).thenReturn(true);
    when(mockInputStrategy.open("test.jshc")).thenReturn(inputStream);

    // Execute
    Optional<Config> configOpt = getter.getConfig("test.jshc");

    // Verify - should return empty Optional on parse error
    assertFalse(configOpt.isPresent());
    verify(mockInputStrategy).exists("test.jshc");
    verify(mockInputStrategy).open("test.jshc");
  }

  @Test
  public void testWithoutExtensionReturnsEmpty() throws IOException {
    // Setup - file exists but we don't append extension
    when(mockInputStrategy.exists("test")).thenReturn(false);

    // Execute
    Optional<Config> configOpt = getter.getConfig("test");

    // Verify - should return empty because extension is required
    assertFalse(configOpt.isPresent());
    verify(mockInputStrategy).exists("test");
    verify(mockInputStrategy, times(0)).open("test");
  }
}
