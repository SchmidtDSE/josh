package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for RunFromMinioCommand, focusing on export path validation logic.
 */
public class RunFromMinioCommandTest {

  @TempDir
  File tempDir;

  private RunFromMinioCommand command;

  @BeforeEach
  void setUp() throws Exception {
    command = new RunFromMinioCommand();

    // Inject simulation name via reflection
    setField(command, "simulation", "Main");

    // Inject output options
    setField(command, "output", new OutputOptions());
  }

  @Test
  void validateExportPaths_withMinioAndReplicate_shouldPass() throws IOException {
    // Use the known-good example file format
    String joshCode = "start unit year\n"
        + "  alias years\n"
        + "end unit\n"
        + "\n"
        + "start simulation Main\n"
        + "  grid.size = 100 m\n"
        + "  grid.low = 0 degrees latitude, 0 degrees longitude\n"
        + "  grid.high = 0.001 degrees latitude, 0.001 degrees longitude\n"
        + "  steps.low = 0 count\n"
        + "  steps.high = 2 count\n"
        + "  exportFiles.patch = \"minio://bucket/results/output_{replicate}.csv\"\n"
        + "end simulation\n"
        + "\n"
        + "start patch Default\n"
        + "  Tree.init = create 1 count of Tree\n"
        + "  export.avgAge.step = mean(Tree.age)\n"
        + "end patch\n"
        + "\n"
        + "start organism Tree\n"
        + "  age.init = 0 year\n"
        + "  age.step = prior.age + 1 year\n"
        + "end organism\n";
    File joshFile = writeJoshFile(joshCode);

    Integer result = command.validateExportPaths(joshFile);
    assertEquals(0, result, "Valid minio:// path with {replicate} should pass validation");
  }

  @Test
  void validateExportPaths_withoutReplicate_shouldFail() throws IOException {
    // Josh script with minio:// export missing {replicate}
    String joshCode = buildJoshScript(
        "\"minio://bucket/results/output.csv\""
    );
    File joshFile = writeJoshFile(joshCode);

    Integer result = command.validateExportPaths(joshFile);
    assertNotEquals(0, result, "Path missing {replicate} should fail validation");
  }

  @Test
  void validateExportPaths_withFileProtocol_shouldFail() throws IOException {
    // Josh script with file:// export (not minio://)
    String joshCode = buildJoshScript(
        "\"file:///tmp/output_{replicate}.csv\""
    );
    File joshFile = writeJoshFile(joshCode);

    Integer result = command.validateExportPaths(joshFile);
    assertNotEquals(0, result, "file:// protocol should fail validation for batch execution");
  }

  @Test
  void validateExportPaths_withNoExportFiles_shouldPass() throws IOException {
    // Josh script with no exportFiles defined at all (valid - nothing to check)
    String joshCode = "start unit year\n"
        + "  alias years\n"
        + "end unit\n"
        + "\n"
        + "start simulation Main\n"
        + "  grid.size = 100 m\n"
        + "  grid.low = 0 degrees latitude, 0 degrees longitude\n"
        + "  grid.high = 0.001 degrees latitude, 0.001 degrees longitude\n"
        + "  steps.low = 0 count\n"
        + "  steps.high = 2 count\n"
        + "end simulation\n"
        + "\n"
        + "start patch Default\n"
        + "  Tree.init = create 1 count of Tree\n"
        + "end patch\n"
        + "\n"
        + "start organism Tree\n"
        + "  age.init = 0 year\n"
        + "  age.step = prior.age + 1 year\n"
        + "end organism\n";
    File joshFile = writeJoshFile(joshCode);

    Integer result = command.validateExportPaths(joshFile);
    assertEquals(0, result, "Script with no export paths should pass validation");
  }

  @Test
  void validateExportPaths_withBothFileAndMissingReplicate_shouldFail() throws IOException {
    // Josh script with both problems: file:// protocol AND missing {replicate}
    String joshCode = buildJoshScript(
        "\"file:///tmp/output.csv\""
    );
    File joshFile = writeJoshFile(joshCode);

    Integer result = command.validateExportPaths(joshFile);
    assertNotEquals(0, result, "Path with both file:// and missing {replicate} should fail");
  }

  @Test
  void validateExportPaths_withNonexistentSimulation_shouldFail() throws Exception {
    // Set simulation to a name that doesn't exist in the script
    setField(command, "simulation", "NonexistentSim");

    String joshCode = buildJoshScript(
        "\"minio://bucket/results/output_{replicate}.csv\""
    );
    File joshFile = writeJoshFile(joshCode);

    Integer result = command.validateExportPaths(joshFile);
    assertNotEquals(0, result, "Nonexistent simulation name should fail validation");
  }

  @Test
  void validateExportPaths_withInvalidJoshFile_shouldFail() throws IOException {
    // Invalid Josh syntax
    File joshFile = writeJoshFile("this is not valid josh code");

    Integer result = command.validateExportPaths(joshFile);
    assertNotEquals(0, result, "Invalid Josh file should fail validation");
  }

  /**
   * Builds a minimal Josh script with the given exportFiles.patch value.
   */
  private String buildJoshScript(String exportPath) {
    return "start unit year\n"
        + "  alias years\n"
        + "end unit\n"
        + "\n"
        + "start simulation Main\n"
        + "  grid.size = 100 m\n"
        + "  grid.low = 0 degrees latitude, 0 degrees longitude\n"
        + "  grid.high = 0.001 degrees latitude, 0.001 degrees longitude\n"
        + "  steps.low = 0 count\n"
        + "  steps.high = 2 count\n"
        + "  exportFiles.patch = " + exportPath + "\n"
        + "end simulation\n"
        + "\n"
        + "start patch Default\n"
        + "  Tree.init = create 1 count of Tree\n"
        + "  export.avgAge.step = mean(Tree.age)\n"
        + "end patch\n"
        + "\n"
        + "start organism Tree\n"
        + "  age.init = 0 year\n"
        + "  age.step = prior.age + 1 year\n"
        + "end organism\n";
  }

  /**
   * Writes Josh code to a temp file and returns it.
   */
  private File writeJoshFile(String content) throws IOException {
    File joshFile = new File(tempDir, "test_simulation.josh");
    Files.writeString(joshFile.toPath(), content);
    return joshFile;
  }

  /**
   * Sets a private field on an object via reflection.
   */
  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = findField(target.getClass(), fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  /**
   * Finds a field by name, searching up the class hierarchy.
   */
  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    while (clazz != null) {
      try {
        return clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
