package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for DiscoverConfigCommand.
 */
public class DiscoverConfigCommandTest {
  private ByteArrayOutputStream outContent;
  private PrintStream originalOut;

  /**
   * Set up output stream capture before each test.
   */
  @BeforeEach
  public void setUpStreams() {
    outContent = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(outContent));
  }

  /**
   * Restore original output stream after each test.
   */
  @AfterEach
  public void restoreStreams() {
    System.setOut(originalOut);
  }

  @Test
  public void testDiscoverMultipleConfigVariables(@TempDir Path tempDir) throws IOException {
    String script = """
        start simulation Test
          grid.size = config example.gridSize
          steps.low = 0 count
          steps.high = config params.stepCount
        end simulation

        start patch Default
          Tree.init = create config example.treeCount of Tree
        end patch

        start organism Tree
          height.init = config example.initialHeight
          height.step = prior.height + 0.1 meters
        end organism
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    DiscoverConfigCommand command = new DiscoverConfigCommand();
    setField(command, "file", scriptFile.toFile());

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString();
    String[] lines = output.trim().split("\n");

    // Convert to set to handle order-independent comparison
    Set<String> outputVariables = new HashSet<>(Arrays.asList(lines));
    Set<String> expectedVariables = Set.of(
        "example.gridSize",
        "params.stepCount",
        "example.treeCount",
        "example.initialHeight"
    );

    assertEquals(expectedVariables.size(), outputVariables.size());
    assertEquals(expectedVariables, outputVariables);
  }

  @Test
  public void testDiscoverSingleConfigVariable(@TempDir Path tempDir) throws IOException {
    String script = """
        start simulation Test
          grid.size = config example.gridSize
          steps.low = 0 count
          steps.high = 10 count
        end simulation
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    DiscoverConfigCommand command = new DiscoverConfigCommand();
    setField(command, "file", scriptFile.toFile());

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString().trim();
    assertEquals("example.gridSize", output);
  }

  @Test
  public void testNoConfigVariables(@TempDir Path tempDir) throws IOException {
    String script = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          height.init = 0 meters
          height.step = prior.height + 0.1 meters
        end organism
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    DiscoverConfigCommand command = new DiscoverConfigCommand();
    setField(command, "file", scriptFile.toFile());

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString().trim();
    assertTrue(output.isEmpty());
  }

  @Test
  public void testNonexistentFile() {
    DiscoverConfigCommand command = new DiscoverConfigCommand();
    setField(command, "file", new File("nonexistent.josh"));

    Integer result = command.call();
    assertEquals(1, result); // LOAD error
  }

  @Test
  public void testInvalidJoshSyntax(@TempDir Path tempDir) throws IOException {
    String script = """
        start simulation Test
          grid.size = invalid syntax here
        end simulation
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    DiscoverConfigCommand command = new DiscoverConfigCommand();
    setField(command, "file", scriptFile.toFile());

    Integer result = command.call();
    assertEquals(3, result); // PARSE error
  }

  @Test
  public void testComplexConfigVariableNames(@TempDir Path tempDir) throws IOException {
    String script = """
        start simulation Test
          grid.size = config environment.parameters.gridSize
          grid.low = 33.7 degrees latitude, -115.4 degrees longitude
          grid.high = 34.0 degrees latitude, -116.4 degrees longitude
          steps.low = 0 count
          steps.high = config simulation.settings.maxSteps
        end simulation

        start patch Default
          Tree.init = create config species.tree.initialCount of Tree
        end patch

        start organism Tree
          height.init = 0 meters
        end organism
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    DiscoverConfigCommand command = new DiscoverConfigCommand();
    setField(command, "file", scriptFile.toFile());

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString();
    String[] lines = output.trim().split("\n");

    Set<String> outputVariables = new HashSet<>(Arrays.asList(lines));
    Set<String> expectedVariables = Set.of(
        "environment.parameters.gridSize",
        "simulation.settings.maxSteps",
        "species.tree.initialCount"
    );

    assertEquals(expectedVariables.size(), outputVariables.size());
    assertEquals(expectedVariables, outputVariables);
  }

  @Test
  public void testDuplicateConfigVariables(@TempDir Path tempDir) throws IOException {
    String script = """
        start simulation Test
          grid.size = config example.gridSize
          grid.low = 33.7 degrees latitude, -115.4 degrees longitude
          grid.high = 34.0 degrees latitude, -116.4 degrees longitude
          steps.low = 0 count
          steps.high = config example.gridSize
        end simulation

        start patch Default
          Tree.init = create config example.gridSize of Tree
        end patch

        start organism Tree
          height.init = 0 meters
        end organism
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    DiscoverConfigCommand command = new DiscoverConfigCommand();
    setField(command, "file", scriptFile.toFile());

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString().trim();
    assertEquals("example.gridSize", output); // Should appear only once despite multiple uses
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
