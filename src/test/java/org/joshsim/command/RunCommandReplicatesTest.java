package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import org.joshsim.JoshSimCommander;
import org.joshsim.JoshSimFacade;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.SimulationMetadata;
import org.joshsim.util.SimulationMetadataExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;


/**
 * Test class for RunCommand replicates functionality.
 *
 * <p>Tests the new --replicates parameter functionality including:
 * - Default behavior (replicates=1)
 * - Multiple replicates execution
 * - Parameter validation
 * - Progress reporting integration
 * - Error handling
 */
class RunCommandReplicatesTest {

  private RunCommand runCommand;
  private AutoCloseable mockito;

  @Mock
  private File mockFile;

  @Mock
  private JoshProgram mockProgram;

  @Mock
  private EngineBridgeSimulationStore mockSimulations;

  @Mock
  private EngineGeometryFactory mockGeometryFactory;

  private ByteArrayOutputStream outputStream;
  private PrintStream originalOut;

  @BeforeEach
  void setUp() throws Exception {
    mockito = MockitoAnnotations.openMocks(this);
    runCommand = new RunCommand();

    // Capture output for testing
    outputStream = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(outputStream));

    // Set up basic mock behaviors
    when(mockFile.exists()).thenReturn(true);
    when(mockProgram.getSimulations()).thenReturn(mockSimulations);
    when(mockSimulations.hasPrototype(anyString())).thenReturn(true);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mockito != null) {
      mockito.close();
    }
    System.setOut(originalOut);
  }

  @Test
  void testDefaultReplicatesValue() throws Exception {
    // Arrange - default RunCommand should have replicates=1
    runCommand = new RunCommand();

    // Act - get the replicates field value
    int replicates = getReplicatesFieldValue(runCommand);

    // Assert
    assertEquals(1, replicates);
  }

  @Test
  void testReplicatesParameterValidation() throws Exception {
    // Arrange
    setupBasicFields(runCommand);
    setFieldValue(runCommand, "simulation", "test-simulation");
    setFieldValue(runCommand, "replicates", -1); // Invalid value

    // Act
    Integer result = runCommand.call();

    // Assert
    assertEquals(1, result); // Error code for validation failure
  }

  @Test
  void testReplicatesParameterValidationZero() throws Exception {
    // Arrange
    setupBasicFields(runCommand);
    setFieldValue(runCommand, "simulation", "test-simulation");
    setFieldValue(runCommand, "replicates", 0); // Invalid value

    // Act
    Integer result = runCommand.call();

    // Assert
    assertEquals(1, result); // Error code for validation failure
  }

  @Test
  void testSingleReplicateExecution() throws Exception {
    // Arrange
    setupBasicFields(runCommand);
    setFieldValue(runCommand, "simulation", "test-simulation");
    setFieldValue(runCommand, "replicates", 1);

    SimulationMetadata metadata = new SimulationMetadata(0, 10, 11);

    try (MockedStatic<JoshSimCommander> commanderMock = mockStatic(JoshSimCommander.class);
         MockedStatic<JoshSimFacade> facadeMock = mockStatic(JoshSimFacade.class);
         MockedStatic<SimulationMetadataExtractor> extractorMock =
             mockStatic(SimulationMetadataExtractor.class)) {

      // Mock the commander initialization
      JoshSimCommander.ProgramInitResult mockResult =
          new JoshSimCommander.ProgramInitResult(mockProgram);
      commanderMock.when(() -> JoshSimCommander.getJoshProgram(
          any(EngineGeometryFactory.class), any(File.class), any(OutputOptions.class), any()))
          .thenReturn(mockResult);

      // Mock metadata extraction
      extractorMock.when(() -> SimulationMetadataExtractor.extractMetadata(
          mockFile, "test-simulation"))
          .thenReturn(metadata);

      // Act
      Integer result = runCommand.call();

      // Assert
      assertEquals(0, result);
      facadeMock.verify(() -> JoshSimFacade.runSimulation(
          any(EngineGeometryFactory.class),
          eq(mockProgram),
          eq("test-simulation"),
          any(), // step callback
          anyBoolean(), // serialPatches
          eq(0), // replicate number
          anyBoolean() // favorBigDecimal
      ), times(1));
    }
  }

  @Test
  void testMultipleReplicatesExecution() throws Exception {
    // Arrange
    setupBasicFields(runCommand);
    setFieldValue(runCommand, "simulation", "test-simulation");
    setFieldValue(runCommand, "replicates", 3);

    SimulationMetadata metadata = new SimulationMetadata(0, 10, 11);

    try (MockedStatic<JoshSimCommander> commanderMock = mockStatic(JoshSimCommander.class);
         MockedStatic<JoshSimFacade> facadeMock = mockStatic(JoshSimFacade.class);
         MockedStatic<SimulationMetadataExtractor> extractorMock =
             mockStatic(SimulationMetadataExtractor.class)) {

      // Mock the commander initialization
      JoshSimCommander.ProgramInitResult mockResult =
          new JoshSimCommander.ProgramInitResult(mockProgram);
      commanderMock.when(() -> JoshSimCommander.getJoshProgram(
          any(EngineGeometryFactory.class), any(File.class), any(OutputOptions.class), any()))
          .thenReturn(mockResult);

      // Mock metadata extraction
      extractorMock.when(() -> SimulationMetadataExtractor.extractMetadata(
          mockFile, "test-simulation"))
          .thenReturn(metadata);

      // Act
      Integer result = runCommand.call();

      // Assert
      assertEquals(0, result);
      // Verify that runSimulation was called 3 times with correct replicate numbers
      facadeMock.verify(() -> JoshSimFacade.runSimulation(
          any(EngineGeometryFactory.class),
          eq(mockProgram),
          eq("test-simulation"),
          any(), // step callback
          anyBoolean(), // serialPatches
          eq(0), // replicate number for first run
          anyBoolean() // favorBigDecimal
      ), times(1));

      facadeMock.verify(() -> JoshSimFacade.runSimulation(
          any(EngineGeometryFactory.class),
          eq(mockProgram),
          eq("test-simulation"),
          any(), // step callback
          anyBoolean(), // serialPatches
          eq(1), // replicate number for second run
          anyBoolean() // favorBigDecimal
      ), times(1));

      facadeMock.verify(() -> JoshSimFacade.runSimulation(
          any(EngineGeometryFactory.class),
          eq(mockProgram),
          eq("test-simulation"),
          any(), // step callback
          anyBoolean(), // serialPatches
          eq(2), // replicate number for third run
          anyBoolean() // favorBigDecimal
      ), times(1));
    }
  }

  @Test
  void testReplicateNumberOffsetExecution() throws Exception {
    // Arrange - test that replicate numbers are offset by replicateNumber field
    setupBasicFields(runCommand);
    setFieldValue(runCommand, "simulation", "test-simulation");
    setFieldValue(runCommand, "replicates", 2);
    // setFieldValue(runCommand, "replicateNumber", 5); // Field removed in grid search

    SimulationMetadata metadata = new SimulationMetadata(0, 10, 11);

    try (MockedStatic<JoshSimCommander> commanderMock = mockStatic(JoshSimCommander.class);
         MockedStatic<JoshSimFacade> facadeMock = mockStatic(JoshSimFacade.class);
         MockedStatic<SimulationMetadataExtractor> extractorMock =
             mockStatic(SimulationMetadataExtractor.class)) {

      // Mock the commander initialization
      JoshSimCommander.ProgramInitResult mockResult =
          new JoshSimCommander.ProgramInitResult(mockProgram);
      commanderMock.when(() -> JoshSimCommander.getJoshProgram(
          any(EngineGeometryFactory.class), any(File.class), any(OutputOptions.class), any()))
          .thenReturn(mockResult);

      // Mock metadata extraction
      extractorMock.when(() -> SimulationMetadataExtractor.extractMetadata(
          mockFile, "test-simulation"))
          .thenReturn(metadata);

      // Act
      Integer result = runCommand.call();

      // Assert
      assertEquals(0, result);
      // Verify that runSimulation was called with replicate numbers 0 and 1 (no offset)
      facadeMock.verify(() -> JoshSimFacade.runSimulation(
          any(EngineGeometryFactory.class),
          eq(mockProgram),
          eq("test-simulation"),
          any(), // step callback
          anyBoolean(), // serialPatches
          eq(0), // replicate number for first run (0-based indexing)
          anyBoolean() // favorBigDecimal
      ), times(1));

      facadeMock.verify(() -> JoshSimFacade.runSimulation(
          any(EngineGeometryFactory.class),
          eq(mockProgram),
          eq("test-simulation"),
          any(), // step callback
          anyBoolean(), // serialPatches
          eq(1), // replicate number for second run (1)
          anyBoolean() // favorBigDecimal
      ), times(1));
    }
  }

  @Test
  void testMetadataExtractionFailureFallback() throws Exception {
    // Arrange
    setupBasicFields(runCommand);
    setFieldValue(runCommand, "simulation", "test-simulation");
    setFieldValue(runCommand, "replicates", 1);

    try (MockedStatic<JoshSimCommander> commanderMock = mockStatic(JoshSimCommander.class);
         MockedStatic<JoshSimFacade> facadeMock = mockStatic(JoshSimFacade.class);
         MockedStatic<SimulationMetadataExtractor> extractorMock =
             mockStatic(SimulationMetadataExtractor.class)) {

      // Mock the commander initialization
      JoshSimCommander.ProgramInitResult mockResult =
          new JoshSimCommander.ProgramInitResult(mockProgram);
      commanderMock.when(() -> JoshSimCommander.getJoshProgram(
          any(EngineGeometryFactory.class), any(File.class), any(OutputOptions.class), any()))
          .thenReturn(mockResult);

      // Mock metadata extraction failure
      extractorMock.when(() -> SimulationMetadataExtractor.extractMetadata(
          mockFile, "test-simulation"))
          .thenThrow(new IOException("Extraction failed"));

      // Act
      Integer result = runCommand.call();

      // Assert
      assertEquals(0, result); // Should still succeed with default metadata
      facadeMock.verify(() -> JoshSimFacade.runSimulation(
          any(EngineGeometryFactory.class),
          eq(mockProgram),
          eq("test-simulation"),
          any(), // step callback
          anyBoolean(), // serialPatches
          anyInt(), // replicate number
          anyBoolean() // favorBigDecimal
      ), times(1));
    }
  }

  @Test
  void testSimulationNotFound() throws Exception {
    // Arrange
    setupBasicFields(runCommand);
    setFieldValue(runCommand, "simulation", "nonexistent-simulation");
    setFieldValue(runCommand, "replicates", 1);

    // Mock simulations to not have the prototype
    when(mockSimulations.hasPrototype("nonexistent-simulation")).thenReturn(false);

    try (MockedStatic<JoshSimCommander> commanderMock = mockStatic(JoshSimCommander.class);
         MockedStatic<JoshSimFacade> facadeMock = mockStatic(JoshSimFacade.class)) {

      // Mock the commander initialization
      JoshSimCommander.ProgramInitResult mockResult =
          new JoshSimCommander.ProgramInitResult(mockProgram);
      commanderMock.when(() -> JoshSimCommander.getJoshProgram(
          any(EngineGeometryFactory.class), any(File.class), any(OutputOptions.class), any()))
          .thenReturn(mockResult);

      // Act
      Integer result = runCommand.call();

      // Assert
      assertEquals(4, result); // Error code for simulation not found
      facadeMock.verify(() -> JoshSimFacade.runSimulation(
          any(EngineGeometryFactory.class),
          any(JoshProgram.class),
          anyString(),
          any(), // step callback
          anyBoolean(), // serialPatches
          anyInt(), // replicate number
          anyBoolean() // favorBigDecimal
      ), never());
    }
  }

  /**
   * Helper method to get private field value using reflection.
   */
  private int getReplicatesFieldValue(RunCommand command) throws Exception {
    Field field = RunCommand.class.getDeclaredField("replicates");
    field.setAccessible(true);
    return (int) field.get(command);
  }

  /**
   * Helper method to set private field value using reflection.
   */
  private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  /**
   * Helper method to set up basic required fields for RunCommand.
   */
  private void setupBasicFields(RunCommand command) throws Exception {
    setFieldValue(command, "file", mockFile);
    setFieldValue(command, "crs", "");
    setFieldValue(command, "dataFiles", new String[0]);
    setFieldValue(command, "replicates", 1);
  }
}
