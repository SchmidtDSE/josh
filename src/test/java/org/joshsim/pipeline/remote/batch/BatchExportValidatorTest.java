package org.joshsim.pipeline.remote.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for BatchExportValidator.
 */
public class BatchExportValidatorTest {

  @TempDir
  File tempDir;

  @Test
  void validate_validMinioWithReplicate_shouldReturnNoErrors() throws IOException {
    File joshFile = writeJoshFile(
        "\"minio://bucket/results/output_{replicate}.csv\""
    );

    List<String> errors = BatchExportValidator.validate(joshFile, "Main", new OutputOptions());

    assertTrue(errors.isEmpty(), "Valid path should produce no errors");
  }

  @Test
  void validate_missingReplicate_shouldReturnError() throws IOException {
    File joshFile = writeJoshFile(
        "\"minio://bucket/results/output.csv\""
    );

    List<String> errors = BatchExportValidator.validate(joshFile, "Main", new OutputOptions());

    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("{replicate}")));
  }

  @Test
  void validate_fileProtocol_shouldReturnError() throws IOException {
    File joshFile = writeJoshFile(
        "\"file:///tmp/output_{replicate}.csv\""
    );

    List<String> errors = BatchExportValidator.validate(joshFile, "Main", new OutputOptions());

    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("minio://")));
  }

  @Test
  void validate_bothErrors_shouldReturnBothMessages() throws IOException {
    File joshFile = writeJoshFile(
        "\"file:///tmp/output.csv\""
    );

    List<String> errors = BatchExportValidator.validate(joshFile, "Main", new OutputOptions());

    // Should have both: missing {replicate} AND wrong protocol
    assertEquals(2, errors.size());
  }

  @Test
  void validate_noExportPaths_shouldReturnNoErrors() throws IOException {
    String joshCode = buildBaseJoshScript(null);
    File joshFile = new File(tempDir, "test.josh");
    Files.writeString(joshFile.toPath(), joshCode);

    List<String> errors = BatchExportValidator.validate(joshFile, "Main", new OutputOptions());

    assertTrue(errors.isEmpty());
  }

  @Test
  void validate_invalidSimulation_shouldReturnError() throws IOException {
    File joshFile = writeJoshFile(
        "\"minio://bucket/results/output_{replicate}.csv\""
    );

    List<String> errors = BatchExportValidator.validate(
        joshFile, "NonexistentSim", new OutputOptions()
    );

    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.contains("Could not find simulation")));
  }

  @Test
  void validate_invalidJoshFile_shouldReturnError() throws IOException {
    File joshFile = new File(tempDir, "invalid.josh");
    Files.writeString(joshFile.toPath(), "this is not valid josh code");

    List<String> errors = BatchExportValidator.validate(joshFile, "Main", new OutputOptions());

    assertFalse(errors.isEmpty());
  }

  /**
   * Writes a Josh file with the given exportFiles.patch value.
   */
  private File writeJoshFile(String exportPath) throws IOException {
    String code = buildBaseJoshScript(exportPath);
    File joshFile = new File(tempDir, "test.josh");
    Files.writeString(joshFile.toPath(), code);
    return joshFile;
  }

  /**
   * Builds base Josh script, optionally with an export path.
   */
  private String buildBaseJoshScript(String exportPath) {
    StringBuilder sb = new StringBuilder();
    sb.append("start unit year\n");
    sb.append("  alias years\n");
    sb.append("end unit\n\n");
    sb.append("start simulation Main\n");
    sb.append("  grid.size = 100 m\n");
    sb.append("  grid.low = 0 degrees latitude, 0 degrees longitude\n");
    sb.append("  grid.high = 0.001 degrees latitude, 0.001 degrees longitude\n");
    sb.append("  steps.low = 0 count\n");
    sb.append("  steps.high = 2 count\n");
    if (exportPath != null) {
      sb.append("  exportFiles.patch = ").append(exportPath).append("\n");
    }
    sb.append("end simulation\n\n");
    sb.append("start patch Default\n");
    sb.append("  Tree.init = create 1 count of Tree\n");
    if (exportPath != null) {
      sb.append("  export.avgAge.step = mean(Tree.age)\n");
    }
    sb.append("end patch\n\n");
    sb.append("start organism Tree\n");
    sb.append("  age.init = 0 year\n");
    sb.append("  age.step = prior.age + 1 year\n");
    sb.append("end organism\n");
    return sb.toString();
  }
}
