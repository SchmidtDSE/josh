/**
 * Tests for the RunUtil shared run pipeline.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.joshsim.JoshSimCommander;
import org.joshsim.util.JoshTestFixtures;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link RunUtil}.
 *
 * <p>Exercises the shared run pipeline that both the {@code run} CLI command and the MCP
 * {@code run_simulation} tool delegate to: a real end-to-end run, the simulation-not-found path,
 * the program-init-failure path, and seed determinism.</p>
 */
class RunUtilTest {

  @TempDir
  Path tempDir;

  /**
   * A real run of a minimal simulation succeeds, writes its export, and reports progress.
   */
  @Test
  void runSucceedsAndWritesOutput() throws Exception {
    Path joshFile = tempDir.resolve("test.josh");
    Path outputFile = tempDir.resolve("output.csv");
    Files.writeString(joshFile,
        JoshTestFixtures.minimalSimulationWithExport(outputFile.toString()));

    RunUtil.RunOptions options = RunUtil.RunOptions.builder(joshFile.toFile(), "TestSim").build();
    RunUtil.RunResult result = RunUtil.run(options, new OutputOptions());

    assertTrue(result.isSuccess(), "Run should succeed: " + result.getMessage());
    assertEquals(1, result.getTotalReplicatesRun());
    assertTrue(result.getLastStep() > 0, "Last step should advance past zero");
    assertTrue(Files.exists(outputFile), "Export CSV should be written");
    assertTrue(Files.readString(outputFile).contains("treeCount"),
        "Export CSV should contain the treeCount column");
  }

  /**
   * Requesting a simulation name that does not exist reports a structured not-found result.
   */
  @Test
  void runReportsSimulationNotFound() throws Exception {
    Path joshFile = tempDir.resolve("test.josh");
    Files.writeString(joshFile, JoshTestFixtures.MINIMAL_SIMULATION_NO_EXPORT);

    RunUtil.RunOptions options = RunUtil.RunOptions.builder(joshFile.toFile(), "DoesNotExist")
        .build();
    RunUtil.RunResult result = RunUtil.run(options, new OutputOptions());

    assertFalse(result.isSuccess());
    assertTrue(result.isSimulationNotFound());
    assertTrue(result.getMessage().contains("DoesNotExist"));
  }

  /**
   * A missing script file surfaces a program-init failure with a step the CLI can map to an
   * exit code.
   */
  @Test
  void runReportsInitFailureForMissingFile() {
    Path missing = tempDir.resolve("nope.josh");

    RunUtil.RunOptions options = RunUtil.RunOptions.builder(missing.toFile(), "TestSim").build();
    RunUtil.RunResult result = RunUtil.run(options, new OutputOptions());

    assertFalse(result.isSuccess());
    assertTrue(result.getInitFailureStep().isPresent(),
        "A missing file should fail at a program-init step");
  }

  /**
   * Two runs with the same seed produce the same output content, confirming the seed is plumbed
   * through and serial execution is forced for determinism. Compared as an order-independent set
   * of rows, since patch export row ordering is not itself guaranteed.
   */
  @Test
  void seededRunsAreDeterministic() throws Exception {
    Path joshFile = tempDir.resolve("test.josh");
    Path outA = tempDir.resolve("a.csv");
    Path outB = tempDir.resolve("b.csv");

    Files.writeString(joshFile, JoshTestFixtures.minimalSimulationWithExport(outA.toString()));
    RunUtil.run(RunUtil.RunOptions.builder(joshFile.toFile(), "TestSim")
        .seed(Optional.of(42L)).build(), new OutputOptions());

    Files.writeString(joshFile, JoshTestFixtures.minimalSimulationWithExport(outB.toString()));
    RunUtil.run(RunUtil.RunOptions.builder(joshFile.toFile(), "TestSim")
        .seed(Optional.of(42L)).build(), new OutputOptions());

    List<String> rowsA = Files.readAllLines(outA).stream().sorted().toList();
    List<String> rowsB = Files.readAllLines(outB).stream().sorted().toList();
    assertEquals(rowsA, rowsB,
        "Runs with the same seed should produce the same set of output rows");
  }

  /**
   * The init-failure step is exposed so callers (the CLI) can derive a distinct exit code.
   */
  @Test
  void initFailureStepIsTypedForExitCodeMapping() {
    Path missing = tempDir.resolve("nope.josh");
    RunUtil.RunResult result = RunUtil.run(
        RunUtil.RunOptions.builder(missing.toFile(), "TestSim").build(), new OutputOptions());

    Optional<JoshSimCommander.CommanderStepEnum> step = result.getInitFailureStep();
    assertTrue(step.isPresent());
  }
}
