package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.joshsim.lang.io.ExportFacade;
import org.joshsim.lang.io.ExportFacadeFactory;
import org.joshsim.lang.io.ExportTarget;
import org.joshsim.pipeline.remote.RunRemoteContext;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.ProgressUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for RemoteResponseHandler.
 *
 * @license BSD-3-Clause
 */
public class RemoteResponseHandlerTest {

  private RunRemoteContext context;
  private ExportFacadeFactory exportFactory;
  private ExportFacade exportFacade;
  private ProgressCalculator progressCalculator;
  private OutputOptions outputOptions;
  private RemoteResponseHandler handler;
  private RemoteResponseHandler cumulativeHandler;

  /**
   * Set up test fixtures before each test.
   */
  @BeforeEach
  public void setUp() {
    context = mock(RunRemoteContext.class);
    exportFactory = mock(ExportFacadeFactory.class);
    exportFacade = mock(ExportFacade.class);
    progressCalculator = mock(ProgressCalculator.class);
    outputOptions = mock(OutputOptions.class);

    when(context.getProgressCalculator()).thenReturn(progressCalculator);
    when(context.getOutputOptions()).thenReturn(outputOptions);
    when(exportFactory.build(any(ExportTarget.class))).thenReturn(exportFacade);

    handler = new RemoteResponseHandler(context, exportFactory, false);
    cumulativeHandler = new RemoteResponseHandler(context, exportFactory, true);
  }

  @Test
  public void testProcessProgressResponse() {
    // Arrange
    String progressLine = "[progress 42]";
    ProgressUpdate progressUpdate = new ProgressUpdate(true, 42.0, "Progress: 42%");
    when(progressCalculator.updateStep(42)).thenReturn(progressUpdate);

    // Act
    Optional<org.joshsim.wire.WireResponse> result =
        handler.processResponseLine(progressLine, 0, null);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(org.joshsim.wire.WireResponse.ResponseType.PROGRESS, result.get().getType());
    verify(progressCalculator).updateStep(42);
    verify(outputOptions).printInfo("Progress: 42%");
  }

  @Test
  public void testProcessProgressResponseWithCumulative() {
    // Arrange
    String progressLine = "[progress 10]";
    AtomicInteger cumulativeCounter = new AtomicInteger(30);
    ProgressUpdate progressUpdate = new ProgressUpdate(true, 40.0, "Progress: 40%");
    when(progressCalculator.updateStep(40)).thenReturn(progressUpdate);

    // Act
    Optional<org.joshsim.wire.WireResponse> result =
        cumulativeHandler.processResponseLine(progressLine, 0, cumulativeCounter);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(40, cumulativeCounter.get()); // 30 + 10
    verify(progressCalculator).updateStep(40);
    verify(outputOptions).printInfo("Progress: 40%");
  }

  @Test
  public void testProcessProgressResponseNoReport() {
    // Arrange
    String progressLine = "[progress 15]";
    ProgressUpdate progressUpdate = new ProgressUpdate(false, 15.0, "Progress: 15%");
    when(progressCalculator.updateStep(15)).thenReturn(progressUpdate);

    // Act
    Optional<org.joshsim.wire.WireResponse> result =
        handler.processResponseLine(progressLine, 0, null);

    // Assert
    assertTrue(result.isPresent());
    verify(progressCalculator).updateStep(15);
    verify(outputOptions, never()).printInfo(any());
  }

  @Test
  public void testProcessDatumResponse() {
    // Arrange
    String datumLine = "[0] TestEntity:x=1\ty=2";

    // Act
    Optional<org.joshsim.wire.WireResponse> result =
        handler.processResponseLine(datumLine, 0, null);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(org.joshsim.wire.WireResponse.ResponseType.DATUM, result.get().getType());

    // Verify export facade was created and started
    verify(exportFactory).build(any(ExportTarget.class));
    verify(exportFacade).start();
    verify(exportFacade).write(any(org.joshsim.wire.NamedMap.class), anyLong());
  }

  @Test
  public void testProcessDatumResponseReusesFacade() {
    // Arrange
    String datumLine1 = "[0] TestEntity:x=1\ty=2";
    String datumLine2 = "[0] TestEntity:x=3\ty=4";

    // Act
    handler.processResponseLine(datumLine1, 0, null);
    handler.processResponseLine(datumLine2, 0, null);

    // Assert
    // Verify export facade was created only once but used twice
    verify(exportFactory, times(1)).build(any(ExportTarget.class));
    verify(exportFacade, times(1)).start();
    verify(exportFacade, times(2)).write(any(org.joshsim.wire.NamedMap.class), anyLong());
  }

  @Test
  public void testProcessDatumResponseMultipleEntities() {
    // Arrange
    ExportFacade exportFacade2 = mock(ExportFacade.class);
    when(exportFactory.build(any(ExportTarget.class)))
        .thenReturn(exportFacade)  // First call
        .thenReturn(exportFacade2); // Second call

    String datumLine1 = "[0] Entity1:x=1\ty=2";
    String datumLine2 = "[0] Entity2:a=3\tb=4";

    // Act
    handler.processResponseLine(datumLine1, 0, null);
    handler.processResponseLine(datumLine2, 0, null);

    // Assert
    // Verify two different export facades were created
    verify(exportFactory, times(2)).build(any(ExportTarget.class));
    verify(exportFacade).start();
    verify(exportFacade2).start();
    verify(exportFacade).write(any(org.joshsim.wire.NamedMap.class), anyLong());
    verify(exportFacade2).write(any(org.joshsim.wire.NamedMap.class), anyLong());
  }

  @Test
  public void testProcessEndResponse() {
    // Arrange
    String endLine = "[end 0]";
    ProgressUpdate endUpdate = new ProgressUpdate(true, 100.0, "Replicate 1 completed");
    when(progressCalculator.updateReplicateCompleted(1)).thenReturn(endUpdate);

    // Act
    Optional<org.joshsim.wire.WireResponse> result =
        handler.processResponseLine(endLine, 0, null);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(org.joshsim.wire.WireResponse.ResponseType.END, result.get().getType());
    assertEquals(1, handler.getCompletedReplicates().get());
    verify(progressCalculator).updateReplicateCompleted(1);
    verify(outputOptions).printInfo("Replicate 1 completed");
  }

  @Test
  public void testProcessEndResponseMultiple() {
    // Arrange
    final String endLine1 = "[end 0]";
    final String endLine2 = "[end 1]";
    ProgressUpdate endUpdate1 = new ProgressUpdate(true, 100.0, "Replicate 1 completed");
    ProgressUpdate endUpdate2 = new ProgressUpdate(true, 100.0, "Replicate 2 completed");
    when(progressCalculator.updateReplicateCompleted(1)).thenReturn(endUpdate1);
    when(progressCalculator.updateReplicateCompleted(2)).thenReturn(endUpdate2);

    // Act
    handler.processResponseLine(endLine1, 0, null);
    handler.processResponseLine(endLine2, 1, null);

    // Assert
    assertEquals(2, handler.getCompletedReplicates().get());
    verify(progressCalculator).updateReplicateCompleted(1);
    verify(progressCalculator).updateReplicateCompleted(2);
  }

  @Test
  public void testProcessErrorResponse() {
    // Arrange
    String errorLine = "[error] Simulation failed with error XYZ";

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      handler.processResponseLine(errorLine, 5, null);
    });

    assertTrue(exception.getMessage().contains("Remote execution error for replicate 5"));
    assertTrue(exception.getMessage().contains("Simulation failed with error XYZ"));
  }

  @Test
  public void testProcessIgnoredLines() {
    // Arrange - WireResponseParser will throw exception for lines not in wire format
    String ignoredLine = "Some random output line";

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      handler.processResponseLine(ignoredLine, 0, null);
    });

    assertTrue(exception.getMessage().contains("Failed to process response for replicate 0"));
    assertTrue(exception.getCause().getMessage().contains("Invalid engine response format"));
  }

  @Test
  public void testProcessEmptyLine() {
    // Arrange - Empty line will be ignored by WireResponseParser
    String emptyLine = "";

    // Act
    Optional<org.joshsim.wire.WireResponse> result =
        handler.processResponseLine(emptyLine, 0, null);

    // Assert
    assertFalse(result.isPresent());
  }

  @Test
  public void testProcessWhitespaceOnlyLine() {
    // Arrange - Whitespace-only line will be ignored by WireResponseParser
    String whitespaceLine = "   \t  \n  ";

    // Act
    Optional<org.joshsim.wire.WireResponse> result =
        handler.processResponseLine(whitespaceLine, 0, null);

    // Assert
    assertFalse(result.isPresent());
  }

  @Test
  public void testCloseExportFacades() throws Exception {
    // Arrange
    String datumLine1 = "[0] Entity1:x=1\ty=2";
    String datumLine2 = "[0] Entity2:a=3\tb=4";

    ExportFacade exportFacade2 = mock(ExportFacade.class);
    when(exportFactory.build(any(ExportTarget.class)))
        .thenReturn(exportFacade)
        .thenReturn(exportFacade2);

    // Process responses to create facades
    handler.processResponseLine(datumLine1, 0, null);
    handler.processResponseLine(datumLine2, 0, null);

    // Act
    handler.closeExportFacades();

    // Assert
    verify(exportFacade).join();
    verify(exportFacade2).join();
  }

  @Test
  public void testCloseExportFacadesWithException() throws Exception {
    // Arrange
    String datumLine = "[0] Entity1:x=1\ty=2";
    handler.processResponseLine(datumLine, 0, null);

    doThrow(new RuntimeException("Join failed")).when(exportFacade).join();

    // Act - should not throw
    handler.closeExportFacades();

    // Assert
    verify(exportFacade).join();
    verify(outputOptions).printError("Failed to close export facade: Join failed");
  }

  @Test
  public void testGetters() {
    // Test initial state
    assertEquals(0, handler.getCurrentStep().get());
    assertEquals(0, handler.getCompletedReplicates().get());
    assertTrue(handler.getExportFacades().isEmpty());

    // Mock progress calculator responses
    ProgressUpdate progressUpdate = new ProgressUpdate(false, 25.0, "Progress: 25%");
    ProgressUpdate endUpdate = new ProgressUpdate(true, 100.0, "Replicate 1 completed");
    when(progressCalculator.updateStep(25)).thenReturn(progressUpdate);
    when(progressCalculator.updateReplicateCompleted(1)).thenReturn(endUpdate);

    // Process some responses to change state
    handler.processResponseLine("[progress 25]", 0, null);
    handler.processResponseLine("[end 0]", 0, null);
    handler.processResponseLine("[0] TestEntity:x=1\ty=2", 0, null);

    // Verify state changes
    assertEquals(25, handler.getCurrentStep().get());
    assertEquals(1, handler.getCompletedReplicates().get());
    assertEquals(1, handler.getExportFacades().size());
    assertTrue(handler.getExportFacades().containsKey("TestEntity"));
  }

  @Test
  public void testProcessingException() {
    // Arrange - malformed datum line that will cause WireConverter to fail
    String malformedLine = "[0] invalid-wire-format";

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      handler.processResponseLine(malformedLine, 3, null);
    });

    assertTrue(exception.getMessage().contains("Failed to process response for replicate 3"));
    assertTrue(exception.getCause().getMessage()
        .contains("Wire format must contain a colon separator"));
  }
}
