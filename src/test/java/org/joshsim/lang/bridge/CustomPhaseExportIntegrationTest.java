package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
 * Integration test proving that an attribute exported on a declared (custom) phase actually
 * reaches the written CSV, not just the in-{@code josh} assert machinery.
 *
 * <p>PR #483 added declared phases ({@code start phases ... end phases}), replacing the default
 * {@code start}/{@code step}/{@code end} substeps with a simulation-chosen sequence. Every
 * existing conformance fixture only checks phase-suffixed attributes via {@code assert.*}
 * handlers, whose truth is enforced purely by the interpreter throwing on a false assertion; none
 * of them route a declared-phase attribute through {@code export.*} and inspect the resulting
 * file. This test closes that gap by parsing the actual CSV file the simulation writes.</p>
 *
 * @license BSD-3-Clause
 */
public class CustomPhaseExportIntegrationTest {

  private static final Path SCRIPT_PATH = Path.of(
      "examples/test/test_custom_phase_export.josh"
  );

  private static final Path OUTPUT_PATH = Path.of("/tmp/test_custom_phase_export.csv");

  @Test
  public void testDeclaredPhaseExportReachesCsv() throws IOException {
    Files.deleteIfExists(OUTPUT_PATH);

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
    JoshSimFacadeUtil.SimulationStepCallback callback = completedSteps::add;

    JoshSimFacade.runSimulation(
        geometryFactory,
        program,
        "CustomPhaseExport",
        callback,
        true,
        1,
        true
    );

    assertFalse(completedSteps.isEmpty(), "Simulation should have completed at least one step");
    assertTrue(Files.exists(OUTPUT_PATH), "The declared-phase export should have written a CSV");

    List<CSVRecord> records;
    try (CSVParser parser = CSVParser.parse(
        Files.newBufferedReader(OUTPUT_PATH),
        CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
      records = parser.getRecords();
    }

    assertFalse(records.isEmpty(), "The CSV should contain at least one exported row");

    CSVRecord stepZeroRow = records.stream()
        .filter(record -> "0".equals(record.get("step")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No exported row found for step 0"));
    assertTrue(stepZeroRow.isMapped("value"),
        "A handler suffixed with a declared custom phase (export.value.manage) must still "
            + "produce a plain \"value\" column, the same as a default-phase export would");

    // value.init = 0, then base/disturb/manage each add 1 within the first simulated step.
    assertEquals("3", stepZeroRow.get("value"));
  }
}
