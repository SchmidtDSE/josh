package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.precompute.JshdUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the {@link InspectJshdCommand} class.
 * These tests verify the functionality of inspecting JSHD files at specific coordinates.
 */
public class InspectJshdCommandTest {

  private ByteArrayOutputStream outContent;
  private ByteArrayOutputStream errContent;
  private PrintStream originalOut;
  private PrintStream originalErr;
  private EngineValueFactory valueFactory;
  private Path testJshdFile;

  @TempDir
  Path tempDir;

  /**
   * Sets up the test environment before each test.
   */
  @BeforeEach
  public void setUp() throws Exception {
    // Set up output stream capture for both stdout and stderr
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    originalOut = System.out;
    originalErr = System.err;
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));

    valueFactory = new EngineValueFactory();
    testJshdFile = createTestJshdFile();
  }

  /**
   * Cleans up resources after each test.
   */
  @AfterEach
  public void tearDown() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  /**
   * Creates a test JSHD file with known data for testing purposes.
   * The grid has values at specific locations:
   * - (0,0,0) = 1.0 meters
   * - (1,1,0) = 2.0 meters
   * - (2,2,0) = 3.0 meters
   * - (0,0,1) = 4.0 meters
   * - (1,1,1) = 5.0 meters
   * - (2,2,1) = 6.0 meters
   * Other locations default to 0.0 meters
   */
  private Path createTestJshdFile() throws IOException {
    // Create mock extents for a 3x3 grid (0-2, 0-2)
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.valueOf(0));
    when(extents.getTopLeftY()).thenReturn(BigDecimal.valueOf(0));
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(2));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(2));

    // Create test data: 2 timesteps, 3x3 grid
    double[][][] innerValues = new double[2][3][3];

    // Timestep 0
    innerValues[0][0][0] = 1.0; // (0,0,0)
    innerValues[0][1][1] = 2.0; // (1,1,0)
    innerValues[0][2][2] = 3.0; // (2,2,0)

    // Timestep 1
    innerValues[1][0][0] = 4.0; // (0,0,1)
    innerValues[1][1][1] = 5.0; // (1,1,1)
    innerValues[1][2][2] = 6.0; // (2,2,1)

    // Create DoublePrecomputedGrid
    DoublePrecomputedGrid grid = new DoublePrecomputedGrid(
        valueFactory,
        extents,
        0, // minTimestep
        1, // maxTimestep
        Units.of("meters"),
        innerValues
    );

    // Serialize to bytes and write to temp file
    byte[] jshdData = JshdUtil.serializeToBytes(grid);
    Path jshdFile = tempDir.resolve("test.jshd");
    Files.write(jshdFile, jshdData);

    return jshdFile;
  }

  @Test
  public void testSuccessfulInspectionOfKnownValue() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "0");
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString();
    assertEquals("Value at (0, 0, 0): 1 meters\n", output);
  }

  @Test
  public void testSuccessfulInspectionOfDifferentValue() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "1");
    setField(command, "xcoordinate", "1");
    setField(command, "ycoordinate", "1");

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString();
    assertEquals("Value at (1, 1, 1): 5 meters\n", output);
  }

  @Test
  public void testSuccessfulInspectionOfDefaultValue() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "0");
    setField(command, "ycoordinate", "1");

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString();
    assertEquals("Value at (0, 1, 0): 0 meters\n", output);
  }

  @Test
  public void testFileNotFound() {
    File nonExistentFile = new File("nonexistent.jshd");

    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", nonExistentFile);
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "0");
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(1, result);

    String output = errContent.toString();
    assertEquals("Could not find JSHD file: " + nonExistentFile.getAbsolutePath() + "\n", output);
  }

  @Test
  public void testInvalidFileExtension() throws IOException {
    // Create a non-JSHD file
    Path nonJshdFile = tempDir.resolve("test.txt");
    Files.write(nonJshdFile, "test content".getBytes());

    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", nonJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "0");
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(2, result);

    String output = errContent.toString();
    assertEquals("File is not a JSHD file: test.txt\n", output);
  }

  @Test
  public void testInvalidTimestep() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "not_a_number");
    setField(command, "xcoordinate", "0");
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(3, result);

    String output = errContent.toString();
    assertEquals("Invalid time step: not_a_number. Must be a valid integer.\n", output);
  }

  @Test
  public void testInvalidXcoordinate() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "invalid_x");
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(4, result);

    String output = errContent.toString();
    assertEquals("Invalid X coordinate: invalid_x. Must be a valid number.\n", output);
  }

  @Test
  public void testInvalidYcoordinate() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "0");
    setField(command, "ycoordinate", "invalid_y");

    Integer result = command.call();
    assertEquals(5, result);

    String output = errContent.toString();
    assertEquals("Invalid Y coordinate: invalid_y. Must be a valid number.\n", output);
  }

  @Test
  public void testInvalidVariableName() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "nonexistent_variable");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "0");
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(6, result);

    String output = errContent.toString();
    assertEquals("Variable 'nonexistent_variable' not found in JSHD file. "
        + "Available variables: [data]\n", output);
  }

  @Test
  public void testCoordinateOutOfBounds() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "10"); // Outside 0-2 range
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(7, result);

    String output = errContent.toString();
    assertEquals("No value found at coordinates (10, 0) for timestep 0 in variable 'data'\n",
        output);
  }

  @Test
  public void testTimestepOutOfBounds() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "5"); // Outside 0-1 range
    setField(command, "xcoordinate", "0");
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(7, result);

    String output = errContent.toString();
    assertEquals("No value found at coordinates (0, 0) for timestep 5 in variable 'data'\n",
        output);
  }

  @Test
  public void testNegativeCoordinates() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "-1");
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(7, result);

    String output = errContent.toString();
    assertEquals("No value found at coordinates (-1, 0) for timestep 0 in variable 'data'\n",
        output);
  }

  @Test
  public void testDecimalCoordinates() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "1.5");
    setField(command, "ycoordinate", "1.5");

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString();
    // Decimal coordinates should be truncated to integers (1.5 -> 1)
    // Since (1,1,0) has value 2.0, this should return that value
    assertEquals("Value at (1.5, 1.5, 0): 2 meters\n", output);
  }

  @Test
  public void testNegativeTimestep() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "-1");
    setField(command, "xcoordinate", "0");
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(7, result);

    String output = errContent.toString();
    // Check that the output contains the expected error message (ignoring any warnings)
    assertTrue(output.contains(
        "No value found at coordinates (0, 0) for timestep -1 in variable 'data'"));
  }

  @Test
  public void testCorruptJshdFile() throws IOException {
    // Create a corrupted JSHD file
    Path corruptedFile = tempDir.resolve("corrupted.jshd");
    Files.write(corruptedFile, "corrupted data".getBytes());

    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", corruptedFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "0");
    setField(command, "ycoordinate", "0");

    Integer result = command.call();
    assertEquals(8, result);

    String output = errContent.toString();
    // Should contain error message about inspecting JSHD file
    assertTrue(output.contains("Error inspecting JSHD file:"));
  }

  @Test
  public void testLargeCoordinates() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data");
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "1000000");
    setField(command, "ycoordinate", "1000000");

    Integer result = command.call();
    assertEquals(7, result);

    String output = errContent.toString();
    assertEquals("No value found at coordinates (1000000, 1000000) "
        + "for timestep 0 in variable 'data'\n", output);
  }

  @Test
  public void testValidateCorrectVariableName() {
    InspectJshdCommand command = new InspectJshdCommand();
    setField(command, "jshdFile", testJshdFile.toFile());
    setField(command, "variable", "data"); // Correct variable name for JSHD files
    setField(command, "timestep", "0");
    setField(command, "xcoordinate", "2");
    setField(command, "ycoordinate", "2");

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString();
    assertEquals("Value at (2, 2, 0): 3 meters\n", output);
  }

  /**
   * Helper method to set private fields using reflection.
   *
   * @param target The object to modify
   * @param fieldName The name of the field to set
   * @param value The value to set
   */
  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }
}
