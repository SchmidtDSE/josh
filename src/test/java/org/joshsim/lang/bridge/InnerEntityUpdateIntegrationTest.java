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
}
