
/**
 * Tests for WasmExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

/**
 * Test suite for the WasmExportFacadeFactory class.
 */
class WasmExportFacadeFactoryTest {

    private WasmExportCallback callbackMock;
    private WasmExportFacadeFactory factory;

    @BeforeEach
    void setUp() {
        callbackMock = mock(WasmExportCallback.class);
        factory = new WasmExportFacadeFactory(callbackMock);
    }

    @Test
    void shouldThrowExceptionForNonMemoryProtocol() {
        // Given
        ExportTarget invalidTarget = new ExportTarget("file", "editor", "test.csv", "csv");

        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            factory.build(invalidTarget);
        });
    }

    @Test
    void shouldThrowExceptionForNonEditorHost() {
        // Given
        ExportTarget invalidTarget = new ExportTarget("memory", "invalid", "test", "csv");

        // Then
        assertThrows(IllegalArgumentException.class, () -> {
            factory.build(invalidTarget);
        });
    }

    @Test
    void shouldBuildValidExportFacade() {
        // Given
        ExportTarget validTarget = new ExportTarget("memory", "editor", "test", "csv");

        // When
        ExportFacade facade = factory.build(validTarget);

        // Then
        assertTrue(facade instanceof MemoryExportFacade);
    }

    @Test
    void shouldIgnoreHeaderWhenBuildingFacade() {
        // Given
        ExportTarget validTarget = new ExportTarget("memory", "editor", "test", "csv");
        Iterable<String> headers = java.util.Arrays.asList("col1", "col2");

        // When
        ExportFacade facadeWithHeader = factory.build(validTarget, headers);
        ExportFacade facadeWithoutHeader = factory.build(validTarget);

        // Then
        assertTrue(facadeWithHeader instanceof MemoryExportFacade);
        assertTrue(facadeWithoutHeader instanceof MemoryExportFacade);
    }
}
