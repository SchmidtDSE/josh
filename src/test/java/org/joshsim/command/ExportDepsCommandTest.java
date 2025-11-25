package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for ExportDepsCommand.
 */
public class ExportDepsCommandTest {
  private ByteArrayOutputStream outContent;
  private PrintStream originalOut;
  private ObjectMapper objectMapper;

  /**
   * Set up output stream capture and JSON parser before each test.
   */
  @BeforeEach
  public void setUpStreams() {
    outContent = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(outContent));
    objectMapper = new ObjectMapper();
  }

  /**
   * Restore original output stream after each test.
   */
  @AfterEach
  public void restoreStreams() {
    System.setOut(originalOut);
  }

  @Test
  public void testExportDepsToFile(@TempDir Path tempDir) throws Exception {
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
          age.init = 0 year
          age.step = prior.age + 1 year
          height.init = 0 m
          height.step = current.age * 0.1 m
        end organism
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    Path outputFile = tempDir.resolve("deps.json");

    ExportDepsCommand command = new ExportDepsCommand();
    setField(command, "scriptFile", scriptFile.toFile());
    setField(command, "simulationName", "Test");
    setField(command, "output", outputFile.toFile());

    Integer result = command.call();
    assertEquals(0, result);

    // Verify output file exists
    assertTrue(Files.exists(outputFile));

    // Parse and verify JSON structure
    String jsonContent = Files.readString(outputFile);
    JsonNode root = objectMapper.readTree(jsonContent);

    assertTrue(root.has("nodes"), "JSON should have 'nodes' field");
    assertTrue(root.has("edges"), "JSON should have 'edges' field");
    assertTrue(root.get("nodes").isArray(), "nodes should be an array");
    assertTrue(root.get("edges").isArray(), "edges should be an array");

    // Verify we have some nodes and edges
    assertTrue(root.get("nodes").size() > 0, "Should have at least one node");
    assertTrue(root.get("edges").size() > 0, "Should have at least one edge");

    // Verify node structure
    JsonNode firstNode = root.get("nodes").get(0);
    assertTrue(firstNode.has("id"), "Node should have 'id' field");
    assertTrue(firstNode.has("entityType"), "Node should have 'entityType' field");
    assertTrue(firstNode.has("attribute"), "Node should have 'attribute' field");
    assertTrue(firstNode.has("event"), "Node should have 'event' field");
    assertTrue(firstNode.has("label"), "Node should have 'label' field");

    // Verify edge structure
    if (root.get("edges").size() > 0) {
      JsonNode firstEdge = root.get("edges").get(0);
      assertTrue(firstEdge.has("from"), "Edge should have 'from' field");
      assertTrue(firstEdge.has("to"), "Edge should have 'to' field");
      assertTrue(firstEdge.has("label"), "Edge should have 'label' field");
    }
  }

  @Test
  public void testExportDepsToStdout(@TempDir Path tempDir) throws Exception {
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
          age.init = 0 year
          age.step = prior.age + 1 year
        end organism
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    ExportDepsCommand command = new ExportDepsCommand();
    setField(command, "scriptFile", scriptFile.toFile());
    setField(command, "simulationName", "Test");
    // Don't set output file - should write to stdout

    Integer result = command.call();
    assertEquals(0, result);

    // Verify JSON was written to stdout
    String output = outContent.toString();
    assertTrue(output.contains("\"nodes\""), "Output should contain nodes");
    assertTrue(output.contains("\"edges\""), "Output should contain edges");

    // Parse and verify it's valid JSON
    JsonNode root = objectMapper.readTree(output);
    assertTrue(root.has("nodes"), "JSON should have 'nodes' field");
    assertTrue(root.has("edges"), "JSON should have 'edges' field");
  }

  @Test
  public void testExportDepsWithDependencies(@TempDir Path tempDir) throws Exception {
    String script = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
          globalTemp.init = 20.0 celsius
        end simulation

        start patch Default
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          age.init = 0 year
          age.step = prior.age + 1 year
          growth.step = meta.globalTemp * 0.1 m
        end organism
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    Path outputFile = tempDir.resolve("deps.json");

    ExportDepsCommand command = new ExportDepsCommand();
    setField(command, "scriptFile", scriptFile.toFile());
    setField(command, "simulationName", "Test");
    setField(command, "output", outputFile.toFile());

    Integer result = command.call();
    assertEquals(0, result);

    // Parse JSON
    String jsonContent = Files.readString(outputFile);
    JsonNode root = objectMapper.readTree(jsonContent);

    // Find temporal dependency edge (prior.age)
    boolean foundPriorDependency = false;
    for (JsonNode edge : root.get("edges")) {
      String label = edge.get("label").asText();
      if (label.contains("prior.age")) {
        foundPriorDependency = true;
        break;
      }
    }
    assertTrue(foundPriorDependency, "Should find temporal dependency using 'prior.age'");

    // Find cross-entity dependency edge (meta.globalTemp)
    boolean foundMetaDependency = false;
    for (JsonNode edge : root.get("edges")) {
      String label = edge.get("label").asText();
      if (label.contains("meta.globalTemp")) {
        foundMetaDependency = true;
        break;
      }
    }
    assertTrue(foundMetaDependency, "Should find cross-entity dependency using 'meta.globalTemp'");
  }

  @Test
  public void testNonexistentFile() throws Exception {
    ExportDepsCommand command = new ExportDepsCommand();
    setField(command, "scriptFile", new File("nonexistent.josh"));
    setField(command, "simulationName", "Test");

    Integer result = command.call();
    assertEquals(1, result); // File not found error
  }

  @Test
  public void testInvalidJoshSyntax(@TempDir Path tempDir) throws Exception {
    String script = """
        start simulation Test
          grid.size = invalid syntax here
        end simulation
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    ExportDepsCommand command = new ExportDepsCommand();
    setField(command, "scriptFile", scriptFile.toFile());
    setField(command, "simulationName", "Test");

    Integer result = command.call();
    assertEquals(3, result); // Parse error
  }

  @Test
  public void testInvalidSimulationName(@TempDir Path tempDir) throws Exception {
    String script = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
        end patch
        """;

    Path scriptFile = tempDir.resolve("test.josh");
    Files.writeString(scriptFile, script);

    ExportDepsCommand command = new ExportDepsCommand();
    setField(command, "scriptFile", scriptFile.toFile());
    setField(command, "simulationName", "NonexistentSimulation");

    Integer result = command.call();
    assertEquals(5, result); // Simulation not found error
  }

  @Test
  public void testExportDepsWithConditionalExpressions(@TempDir Path tempDir) throws Exception {
    // Use test_states_conditional.josh which has conditional state transitions
    String testFile = "josh-tests/conformance/control/states/test_states_conditional.josh";
    Path outputPath = tempDir.resolve("states_deps.json");

    ExportDepsCommand command = new ExportDepsCommand();
    setField(command, "scriptFile", new File(testFile));
    setField(command, "simulationName", "StatesConditional");
    setField(command, "output", outputPath.toFile());

    int result = command.call();
    assertEquals(0, result, "Command should succeed");

    // Read and parse the JSON output
    String json = Files.readString(outputPath);
    JsonNode root = objectMapper.readTree(json);

    // Find the Tree.state.step node
    JsonNode nodes = root.get("nodes");
    JsonNode stateStepNode = null;
    for (JsonNode node : nodes) {
      if (node.get("id").asText().equals("Tree.state.step")) {
        stateStepNode = node;
        break;
      }
    }

    assertNotNull(stateStepNode, "Should have Tree.state.step node");

    // Verify allSources contains condition information
    JsonNode allSources = stateStepNode.get("allSources");
    assertNotNull(allSources, "Should have allSources array");
    assertTrue(allSources.size() >= 3, "Should have at least 3 conditional branches");

    // Check that at least one source has condition field
    boolean hasCondition = false;
    for (JsonNode source : allSources) {
      if (source.has("condition")) {
        hasCondition = true;
        // Verify the condition looks like a state transition condition
        String condition = source.get("condition").asText();
        assertTrue(
            condition.contains("age") || condition.contains("state"),
            "Condition should reference age or state"
        );
        break;
      }
    }
    assertTrue(hasCondition, "At least one source should have a condition field");
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
