package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.joshsim.JoshSimFacade;
import org.joshsim.JoshSimFacadeUtil;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.JvmInputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.parse.ParseResult;
import org.junit.jupiter.api.Test;

/**
 * Integration test for inner entity updates.
 *
 * <p>This test verifies that inner entities (organisms created within patches)
 * properly update their attributes across simulation steps.</p>
 *
 * @license BSD-3-Clause
 */
public class InnerEntityUpdateIntegrationTest {

  private static final Path SCRIPT_PATH = Path.of(
      "examples/test/assert_changing_entities.josh"
  );

  private static final Path STATE_BLOCK_SCRIPT_PATH = Path.of(
      "examples/test/assert_changing_entities_within_state_block.josh"
  );

  private static final Path STALE_VALUES_SCRIPT_PATH = Path.of(
      "examples/test/test1_patch_sees_stale_inner_entity_values.josh"
  );

  private static final Path MISSING_STATE_INIT_SCRIPT_PATH = Path.of(
      "examples/test/test2_missing_state_init.josh"
  );

  private static final Path NO_HANDLER_FOR_STATE_SCRIPT_PATH = Path.of(
      "examples/test/test3_no_handler_for_current_state.josh"
  );

  @Test
  public void testInnerEntitiesUpdateAcrossSteps() throws IOException {
    String joshCode = Files.readString(SCRIPT_PATH);

    ParseResult parsed = JoshSimFacade.parse(joshCode);
    assertFalse(parsed.hasErrors(),
        "Josh code should parse without errors. Errors: " + parsed.getErrors());

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    JvmInputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(1)
        .build();

    JoshProgram program = JoshSimFacade.interpret(geometryFactory, parsed, inputOutputLayer);
    assertNotNull(program, "Program should be successfully interpreted");

    List<Long> completedSteps = new ArrayList<>();

    JoshSimFacadeUtil.SimulationStepCallback callback = (stepNumber) -> {
      completedSteps.add(stepNumber);
    };

    JoshSimFacade.runSimulation(
        geometryFactory,
        program,
        "Main",
        callback,
        true,
        1,
        true
    );

    assertFalse(completedSteps.isEmpty(), "Simulation should have completed at least one step");
  }

  /**
   * Test that entities update correctly when attribute handlers are defined within state blocks.
   *
   * <p>This test verifies that when an attribute (like maturity) only has handlers defined
   * inside state blocks, the state transition based on that attribute works correctly.</p>
   */
  @Test
  public void testInnerEntitiesUpdateWithinStateBlock() throws IOException {
    String joshCode = Files.readString(STATE_BLOCK_SCRIPT_PATH);

    ParseResult parsed = JoshSimFacade.parse(joshCode);
    assertFalse(parsed.hasErrors(),
        "Josh code should parse without errors. Errors: " + parsed.getErrors());

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    JvmInputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(1)
        .build();

    JoshProgram program = JoshSimFacade.interpret(geometryFactory, parsed, inputOutputLayer);
    assertNotNull(program, "Program should be successfully interpreted");

    List<Long> completedSteps = new ArrayList<>();

    JoshSimFacadeUtil.SimulationStepCallback callback = (stepNumber) -> {
      completedSteps.add(stepNumber);
    };

    JoshSimFacade.runSimulation(
        geometryFactory,
        program,
        "Main",
        callback,
        true,
        1,
        true
    );

    assertFalse(completedSteps.isEmpty(), "Simulation should have completed at least one step");
  }

  /**
   * Test that patches see current values from inner entities with state-block handlers.
   *
   * <p>This test verifies that when a patch queries an inner entity attribute (like maturity)
   * that is computed in a state block, the patch sees the current computed value rather than
   * a stale prior value. The state must be resolved before other attributes that depend on it.</p>
   */
  @Test
  public void testPatchSeesCurrentInnerEntityValuesFromStateBlock() throws IOException {
    String joshCode = Files.readString(STALE_VALUES_SCRIPT_PATH);

    ParseResult parsed = JoshSimFacade.parse(joshCode);
    assertFalse(parsed.hasErrors(),
        "Josh code should parse without errors. Errors: " + parsed.getErrors());

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    JvmInputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(1)
        .build();

    JoshProgram program = JoshSimFacade.interpret(geometryFactory, parsed, inputOutputLayer);
    assertNotNull(program, "Program should be successfully interpreted");

    List<Long> completedSteps = new ArrayList<>();

    JoshSimFacadeUtil.SimulationStepCallback callback = (stepNumber) -> {
      completedSteps.add(stepNumber);
    };

    JoshSimFacade.runSimulation(
        geometryFactory,
        program,
        "Main",
        callback,
        true,
        1,
        true
    );

    assertFalse(completedSteps.isEmpty(), "Simulation should have completed at least one step");
  }

  /**
   * Test that entities without state.init fall back to base handlers correctly.
   *
   * <p>This test verifies that when an entity has state blocks defined but no state.init,
   * the system falls back to base (non-state-specific) handlers for attribute resolution
   * rather than crashing.</p>
   */
  @Test
  public void testMissingStateInitFallsBackToBaseHandlers() throws IOException {
    String joshCode = Files.readString(MISSING_STATE_INIT_SCRIPT_PATH);

    ParseResult parsed = JoshSimFacade.parse(joshCode);
    assertFalse(parsed.hasErrors(),
        "Josh code should parse without errors. Errors: " + parsed.getErrors());

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    JvmInputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(1)
        .build();

    JoshProgram program = JoshSimFacade.interpret(geometryFactory, parsed, inputOutputLayer);
    assertNotNull(program, "Program should be successfully interpreted");

    List<Long> completedSteps = new ArrayList<>();

    JoshSimFacadeUtil.SimulationStepCallback callback = (stepNumber) -> {
      completedSteps.add(stepNumber);
    };

    JoshSimFacade.runSimulation(
        geometryFactory,
        program,
        "Main",
        callback,
        true,
        1,
        true
    );

    assertFalse(completedSteps.isEmpty(), "Simulation should have completed at least one step");
  }

  /**
   * Test that entities fall back to base handlers when current state has no handler.
   *
   * <p>This test verifies that when an entity is in a state that has no handler defined
   * for a particular attribute (e.g., maturity has handlers for "juvenile" and "adult"
   * but not "seedling"), the system falls back to base handlers rather than crashing.</p>
   */
  @Test
  public void testNoHandlerForCurrentStateFallsBackToBase() throws IOException {
    String joshCode = Files.readString(NO_HANDLER_FOR_STATE_SCRIPT_PATH);

    ParseResult parsed = JoshSimFacade.parse(joshCode);
    assertFalse(parsed.hasErrors(),
        "Josh code should parse without errors. Errors: " + parsed.getErrors());

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    JvmInputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(1)
        .build();

    JoshProgram program = JoshSimFacade.interpret(geometryFactory, parsed, inputOutputLayer);
    assertNotNull(program, "Program should be successfully interpreted");

    List<Long> completedSteps = new ArrayList<>();

    JoshSimFacadeUtil.SimulationStepCallback callback = (stepNumber) -> {
      completedSteps.add(stepNumber);
    };

    JoshSimFacade.runSimulation(
        geometryFactory,
        program,
        "Main",
        callback,
        true,
        1,
        true
    );

    assertFalse(completedSteps.isEmpty(), "Simulation should have completed at least one step");
  }
}
