
/**
 * Tests for SandboxExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

  @Test
  void shouldReturnPathUnchangedWhenNoTemplates() {
    // Given
    String pathWithoutTemplates = "memory://editor/simple_path.csv";

    // When
    String result = factory.getPath(pathWithoutTemplates);

    // Then
    assertEquals(pathWithoutTemplates, result);
  }

  @Test
  void shouldThrowExceptionForSingleTemplate() {
    // Given
    String pathWithTemplate = "memory://editor/{example}.csv";

    // When/Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      factory.getPath(pathWithTemplate);
    });

    String message = exception.getMessage();
    assertTrue(message.contains("Template strings are not supported in sandbox/editor execution"));
    assertTrue(message.contains("Found template variables: {example}"));
  }

  @Test
  void shouldThrowExceptionForMultipleTemplates() {
    // Given
    String pathWithTemplates = "memory://editor/output_{example}_{other}_{replicate}.csv";

    // When/Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      factory.getPath(pathWithTemplates);
    });

    String message = exception.getMessage();
    assertTrue(message.contains("Template strings are not supported in sandbox/editor execution"));
    assertTrue(message.contains("{example}"));
    assertTrue(message.contains("{other}"));
    assertTrue(message.contains("{replicate}"));
  }

  @Test
  void shouldThrowExceptionForExportSpecificTemplates() {
    // Given
    String pathWithExportTemplates = "memory://editor/output_{step}_{variable}_{replicate}.nc";

    // When/Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      factory.getPath(pathWithExportTemplates);
    });

    String message = exception.getMessage();
    assertTrue(message.contains("Template strings are not supported in sandbox/editor execution"));
    assertTrue(message.contains("{step}"));
    assertTrue(message.contains("{variable}"));
    assertTrue(message.contains("{replicate}"));
  }

  @Test
  void shouldThrowExceptionForComplexTemplates() {
    // Given
    String complexPath = "memory://editor/complex_{example}_step{step}_"
        + "var{variable}_rep{replicate}.tiff";

    // When/Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      factory.getPath(complexPath);
    });

    String message = exception.getMessage();
    assertTrue(message.contains("Template strings are not supported in sandbox/editor execution"));
    assertTrue(message.contains("{example}"));
    assertTrue(message.contains("{step}"));
    assertTrue(message.contains("{variable}"));
    assertTrue(message.contains("{replicate}"));
  }

  @Test
  void shouldAllowPathsWithCurlyBracesButNotTemplates() {
    // Given - braces that don't form valid template patterns (single braces, empty braces)
    String pathWithNonTemplates = "memory://editor/path_with_{}_and_{incomplete.csv";

    // When
    String result = factory.getPath(pathWithNonTemplates);

    // Then
    assertEquals(pathWithNonTemplates, result);
  }

  @Test
  void shouldDetectTemplatesInDifferentFileFormats() {
    // Test various file formats to ensure template detection works regardless of extension
    String[] templatedPaths = {
        "memory://editor/{template}.csv",
        "memory://editor/{template}.nc",
        "memory://editor/{template}.tif",
        "memory://editor/{template}.tiff",
        "memory://editor/{template}.json",
        "memory://editor/{template}.txt"
    };

    for (String path : templatedPaths) {
      RuntimeException exception = assertThrows(RuntimeException.class, () -> {
        factory.getPath(path);
      }, "Should reject template in path: " + path);

      assertTrue(exception.getMessage().contains("Template strings are not supported"),
          "Exception message should explain template restriction for: " + path);
      assertTrue(exception.getMessage().contains("{template}"),
          "Exception should identify the template variable for: " + path);
    }
  }
}
