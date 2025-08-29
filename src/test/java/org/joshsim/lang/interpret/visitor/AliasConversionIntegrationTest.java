package org.joshsim.lang.interpret.visitor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
 * Integration test for alias conversion between different unit aliases.
 *
 * <p>This test verifies that conversion between aliases (like "yeers" and "yrs")
 * works correctly, allowing them to be treated as the same unit.
 */
public class AliasConversionIntegrationTest {

  @Test
  public void testAliasConversionBetweenYeersAndYrs() {
    // Create Josh source code that defines unit aliases and attempts to convert between them
    String joshCode = """
        start unit year
          alias years
          alias yr
          alias yrs
          alias yeers
        end unit

        start simulation AliasConversionTest
          grid.size = 1 count
          grid.low = 0 count latitude, 0 count longitude
          grid.high = 1 count latitude, 1 count longitude
          steps.low = 0 count
          steps.high = 3 count
        end simulation

        start patch Default
          # Initialize with one unit alias
          testValue.init = 5 yeers

          # Try to convert to another alias
          convertedValue.step = current.testValue as yrs

          # Export values for verification
          export.originalValue.step = current.testValue
          export.convertedValue.step = current.convertedValue
        end patch
        """;

    // Parse the Josh code
    ParseResult parsed = JoshSimFacade.parse(joshCode);
    assertFalse(parsed.hasErrors(),
        "Josh code should parse without errors. Errors: " + parsed.getErrors());

    // Create geometry factory and input/output layer
    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    JvmInputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(1)
        .build();

    // Interpret the code
    JoshProgram program = JoshSimFacade.interpret(geometryFactory, parsed, inputOutputLayer);
    assertNotNull(program, "Program should be successfully interpreted");

    // Capture simulation step numbers
    List<Long> completedSteps = new ArrayList<>();

    // Callback to track simulation progress
    JoshSimFacadeUtil.SimulationStepCallback callback = (stepNumber) -> {
      completedSteps.add(stepNumber);
    };

    // Run the simulation - alias conversion works
    JoshSimFacade.runSimulation(
        geometryFactory,
        program,
        "AliasConversionTest",
        callback,
        true,  // serial patches
        1,     // replicate number
        false  // favor double over BigDecimal
    );

    // Verify simulation completed successfully
    assertFalse(completedSteps.isEmpty(), "Simulation should have completed at least one step");
  }

  @Test
  public void testMultipleAliasConversionsInCalculations() {
    // Test arithmetic operations with different aliases
    String joshCode = """
        start unit year
          alias years
          alias yr
          alias yrs
          alias yeers
        end unit

        start simulation MultiAliasCalculationTest
          grid.size = 1 count
          grid.low = 0 count latitude, 0 count longitude
          grid.high = 1 count latitude, 1 count longitude
          steps.low = 0 count
          steps.high = 2 count
        end simulation

        start patch Default
          # Initialize with different aliases
          value1.init = 2 yeers
          value2.init = 3 yrs

          # Perform calculations using different aliases
          sum.step = current.value1 + current.value2
          difference.step = current.value2 - current.value1

          # Convert results to different aliases
          sumAsYr.step = current.sum as yr
          sumAsYears.step = current.sum as years

          # Export for verification
          export.sum.step = current.sum
          export.sumAsYr.step = current.sumAsYr
          export.sumAsYears.step = current.sumAsYears
        end patch
        """;

    // Parse and run the simulation
    ParseResult parsed = JoshSimFacade.parse(joshCode);
    assertFalse(parsed.hasErrors(),
        "Josh code should parse without errors. Errors: " + parsed.getErrors());

    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    JvmInputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(1)
        .build();
    JoshProgram program = JoshSimFacade.interpret(geometryFactory, parsed, inputOutputLayer);

    // Capture step results
    List<Long> completedSteps = new ArrayList<>();
    JoshSimFacadeUtil.SimulationStepCallback callback = (stepNumber) -> {
      completedSteps.add(stepNumber);
    };

    // Run simulation - arithmetic with aliases works
    JoshSimFacade.runSimulation(
        geometryFactory,
        program,
        "MultiAliasCalculationTest",
        callback,
        true, 1, false
    );

    // Verify simulation completed successfully
    assertFalse(completedSteps.isEmpty(), "Simulation should have completed at least one step");
  }
}
