
/**
 * Tests for SandboxExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.joshsim.lang.io.strategy.MemoryExportFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Test suite for the SandboxExportFacadeFactory class.
 */
class SandboxExportFacadeFactoryTest {

  private SandboxExportCallback callbackMock;
  private SandboxExportFacadeFactory factory;

  @BeforeEach
  void setUp() {
    callbackMock = mock(SandboxExportCallback.class);
    factory = new SandboxExportFacadeFactory(callbackMock);
  }

  @Test
  void shouldThrowExceptionForNonMemoryProtocol() {
    // Given
    ExportTarget invalidTarget = new ExportTarget("file", "editor", "test.csv");

    // Then
    assertThrows(IllegalArgumentException.class, () -> {
      factory.build(invalidTarget);
    });
  }

  @Test
  void shouldThrowExceptionForNonEditorHost() {
    // Given
    ExportTarget invalidTarget = new ExportTarget("memory", "invalid", "test");

    // Then
    assertThrows(IllegalArgumentException.class, () -> {
      factory.build(invalidTarget);
    });
  }

  @Test
  void shouldBuildValidExportFacade() {
    // Given
    ExportTarget validTarget = new ExportTarget("memory", "editor", "test");

    // When
    ExportFacade facade = factory.build(validTarget);

    // Then
    assertTrue(facade instanceof MemoryExportFacade);
  }

  @Test
  void shouldIgnoreHeaderWhenBuildingFacade() {
    // Given
    ExportTarget validTarget = new ExportTarget("memory", "editor", "test");
    Iterable<String> headers = java.util.Arrays.asList("col1", "col2");

    // When
    ExportFacade facadeWithHeader = factory.build(validTarget, headers);
    ExportFacade facadeWithoutHeader = factory.build(validTarget);

    // Then
    assertTrue(facadeWithHeader instanceof MemoryExportFacade);
    assertTrue(facadeWithoutHeader instanceof MemoryExportFacade);
  }
}
