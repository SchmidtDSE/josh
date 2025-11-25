/**
 * Command line interface handler for exporting dependency graphs.
 *
 * <p>This class implements the 'exportDeps' command which extracts and exports dependency
 * information from Josh script files to JSON format. The output can be used for visualization
 * and analysis of how attributes depend on each other across entities.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import org.joshsim.JoshSimCommander;
import org.joshsim.JoshSimFacade;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.lang.analyze.DependencyGraph;
import org.joshsim.lang.analyze.DependencyGraphExtractor;
import org.joshsim.lang.analyze.DependencyTracker;
import org.joshsim.lang.analyze.JsonExporter;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.parse.ParseResult;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command handler for exporting dependency graphs from Josh simulations.
 *
 * <p>This command parses a Josh script file, interprets it with dependency tracking enabled,
 * and exports the resulting dependency graph to JSON format. The JSON output includes nodes
 * for each attribute at each event (init, step, end) and edges showing dependencies between
 * them. This enables visualization of both intra-entity and cross-entity dependencies.</p>
 *
 * <p>Example usage:
 * <pre>
 * joshsim exportDeps script.josh SimulationName -o output.json
 * joshsim exportDeps script.josh SimulationName  # Outputs to stdout
 * </pre>
 */
@Command(
    name = "exportDeps",
    description = "Export dependency graph to JSON"
)
public class ExportDepsCommand implements Callable<Integer> {

  @Parameters(index = "0", description = "Josh script file")
  private File scriptFile;

  @Parameters(index = "1", description = "Simulation name")
  private String simulationName;

  @Option(
      names = {"-o", "--output"},
      description = "Output JSON file (stdout if not specified)"
  )
  private File output;

  @Mixin
  private OutputOptions outputOptions = new OutputOptions();

  @Override
  public Integer call() throws Exception {
    // Validate that the script file exists
    if (!scriptFile.exists()) {
      outputOptions.printError("Could not find file: " + scriptFile);
      return 1;
    }

    // Read the file content
    String fileContent;
    try {
      fileContent = new String(Files.readAllBytes(scriptFile.toPath()));
    } catch (Exception e) {
      outputOptions.printError("Error reading input file: " + e.getMessage());
      return 2;
    }

    // Parse the Josh script
    ParseResult parseResult = JoshSimFacade.parse(fileContent);
    if (parseResult.hasErrors()) {
      outputOptions.printError("Parse errors found:");
      parseResult.getErrors().forEach(error ->
          outputOptions.printError(" - Line " + error.getLine() + ": " + error.getMessage())
      );
      return 3;
    }

    // Enable dependency tracking before interpretation
    DependencyTracker.enable();

    JoshProgram program;
    try {
      // Create InputOutputLayer for interpretation
      InputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder().build();

      // Interpret the program with dependency tracking enabled
      program = JoshSimFacade.interpret(
          new GridGeometryFactory(),
          parseResult,
          inputOutputLayer
      );

      if (program == null) {
        outputOptions.printError("Failed to interpret program");
        return 4;
      }

      // Verify the simulation exists
      if (!program.getSimulations().hasPrototype(simulationName)) {
        outputOptions.printError("Could not find simulation: " + simulationName);
        return 5;
      }

    } catch (Exception e) {
      outputOptions.printError("Error during interpretation: " + e.getMessage());
      return 4;
    }

    // Extract the dependency graph
    // Note: DependencyTracker remains enabled during extraction
    DependencyGraphExtractor extractor = new DependencyGraphExtractor();
    DependencyGraph graph;
    try {
      graph = extractor.extract(program, simulationName);
    } catch (Exception e) {
      outputOptions.printError("Error extracting dependency graph: " + e.getMessage());
      return 6;
    } finally {
      // Disable tracking after extraction
      DependencyTracker.disable();
    }

    // Export to JSON
    JsonExporter jsonExporter = new JsonExporter();
    String json;
    try {
      json = jsonExporter.export(graph);
    } catch (Exception e) {
      outputOptions.printError("Error exporting to JSON: " + e.getMessage());
      return 7;
    }

    // Write output to file or stdout
    try {
      if (output != null) {
        Files.writeString(output.toPath(), json);
        outputOptions.printInfo("Dependency graph exported to: " + output);
      } else {
        System.out.println(json);
      }
    } catch (Exception e) {
      outputOptions.printError("Error writing output: " + e.getMessage());
      return 8;
    }

    return 0;
  }
}
