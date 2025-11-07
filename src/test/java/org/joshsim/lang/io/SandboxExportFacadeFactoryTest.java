
/**
 * Tests for SandboxExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

  @Test
  void shouldAllowTemplatesWhenEnabled() {
    // Given
    SandboxExportFacadeFactory factoryWithTemplates =
        new SandboxExportFacadeFactory(callbackMock, true);
    String pathWithTemplates = "memory://editor/output_{replicate}_{step}.csv";

    // When
    String result = factoryWithTemplates.getPath(pathWithTemplates);

    // Then
    assertEquals(pathWithTemplates, result);
  }

  @Test
  void shouldRejectTemplatesByDefault() {
    // Given
    SandboxExportFacadeFactory defaultFactory = new SandboxExportFacadeFactory(callbackMock);
    String pathWithTemplates = "memory://editor/output_{replicate}.csv";

    // When/Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      defaultFactory.getPath(pathWithTemplates);
    });
    assertTrue(exception.getMessage().contains("Template strings are not supported"));
  }

  @Test
  void shouldAllowMultipleTemplatesWhenEnabled() {
    // Given
    SandboxExportFacadeFactory factoryWithTemplates =
        new SandboxExportFacadeFactory(callbackMock, true);
    String complexPath = "memory://editor/{example}_{replicate}_{step}_{variable}.csv";

    // When
    String result = factoryWithTemplates.getPath(complexPath);

    // Then
    assertEquals(complexPath, result);
  }

  @Test
  void shouldAllowExportTemplatesWhenEnabled() {
    // Given
    SandboxExportFacadeFactory factoryWithTemplates =
        new SandboxExportFacadeFactory(callbackMock, true);
    String pathWithExportTemplates = "memory://editor/output_{step}_{variable}_{replicate}.nc";

    // When
    String result = factoryWithTemplates.getPath(pathWithExportTemplates);

    // Then
    assertEquals(pathWithExportTemplates, result);
  }

  @Test
  void shouldRejectTemplatesWhenDisabledExplicitly() {
    // Given
    SandboxExportFacadeFactory factoryNoTemplates =
        new SandboxExportFacadeFactory(callbackMock, false);
    String pathWithTemplates = "memory://editor/output_{editor}.csv";

    // When/Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      factoryNoTemplates.getPath(pathWithTemplates);
    });
    assertTrue(exception.getMessage().contains("Template strings are not supported"));
    assertTrue(exception.getMessage().contains("{editor}"));
  }

  @Test
  void shouldAllowFileTargetsWhenTemplatesEnabled() {
    // Given - Remote workers stream output, so file:// targets are allowed
    SandboxExportFacadeFactory factoryWithTemplates =
        new SandboxExportFacadeFactory(callbackMock, true);
    ExportTarget fileTarget = new ExportTarget("file", "", "/tmp/output_replicate.csv");

    // When
    ExportFacade facade = factoryWithTemplates.build(fileTarget);

    // Then
    assertNotNull(facade);
    assertTrue(facade instanceof MemoryExportFacade);
  }

  @Test
  void shouldAllowMinioTargetsWhenTemplatesEnabled() {
    // Given - Remote workers stream output, so minio:// targets are allowed
    SandboxExportFacadeFactory factoryWithTemplates =
        new SandboxExportFacadeFactory(callbackMock, true);
    ExportTarget minioTarget = new ExportTarget("minio", "bucket", "/path/output_replicate.csv");

    // When
    ExportFacade facade = factoryWithTemplates.build(minioTarget);

    // Then
    assertNotNull(facade);
    assertTrue(facade instanceof MemoryExportFacade);
  }

  @Test
  void shouldRejectFileTargetsByDefault() {
    // Given - WebAssembly mode should reject file:// targets
    SandboxExportFacadeFactory defaultFactory = new SandboxExportFacadeFactory(callbackMock);
    ExportTarget fileTarget = new ExportTarget("file", "", "/tmp/output.csv");

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      defaultFactory.build(fileTarget);
    });
    assertTrue(exception.getMessage().contains("Only in-memory targets supported on WASM"));
  }

  @Test
  void shouldRejectMinioTargetsByDefault() {
    // Given - WebAssembly mode should reject minio:// targets
    SandboxExportFacadeFactory defaultFactory = new SandboxExportFacadeFactory(callbackMock);
    ExportTarget minioTarget = new ExportTarget("minio", "bucket", "/path/output.csv");

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      defaultFactory.build(minioTarget);
    });
    assertTrue(exception.getMessage().contains("Only in-memory targets supported on WASM"));
  }

  @Test
  void shouldRejectNonEditorHostByDefault() {
    // Given - WebAssembly mode should reject non-editor hosts
    SandboxExportFacadeFactory defaultFactory = new SandboxExportFacadeFactory(callbackMock);
    ExportTarget nonEditorTarget = new ExportTarget("memory", "somehost", "/output.csv");

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      defaultFactory.build(nonEditorTarget);
    });
    assertTrue(exception.getMessage().contains("Only editor targets supported on WASM"));
  }
}
