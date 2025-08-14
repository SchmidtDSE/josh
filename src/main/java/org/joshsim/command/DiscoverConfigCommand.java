/**
 * Command line interface handler for discovering configuration variables in Josh simulation files.
 *
 * <p>This class implements the 'discoverConfig' command which analyzes Josh script files to
 * identify all configuration variables used with 'config' expressions. It prints each discovered
 * variable name on a separate line to stdout.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.Callable;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.joshsim.JoshSimCommander;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.lang.antlr.JoshLangLexer;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.visitor.JoshConfigDiscoveryVisitor;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

/**
 * Command handler for discovering configuration variables in Josh scripts.
 *
 * <p>Analyzes Josh script files to discover all configuration variables used
 * with 'config' expressions. Prints each discovered variable name on a separate
 * line to stdout.</p>
 */
@Command(
    name = "discoverConfig",
    description = "Discover configuration variables used in a Josh script"
)
public class DiscoverConfigCommand implements Callable<Integer> {
  private static final int DISCOVERY_ERROR_CODE = 5;
  private static final int UNKNOWN_ERROR_CODE = 404;

  @Parameters(index = "0", description = "Path to Josh script file")
  private File file;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    // First validate the file using existing validation infrastructure
    JoshSimCommander.ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
        new GridGeometryFactory(),
        file,
        output
    );

    if (initResult.getFailureStep().isPresent()) {
      JoshSimCommander.CommanderStepEnum failStep = initResult.getFailureStep().get();
      return switch (failStep) {
        case LOAD -> 1;
        case READ -> 2;
        case PARSE -> 3;
        default -> UNKNOWN_ERROR_CODE;
      };
    }

    // Parse file for config discovery
    try {
      String fileContent = Files.readString(file.toPath());
      JoshLangLexer lexer = new JoshLangLexer(CharStreams.fromString(fileContent));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      JoshLangParser parser = new JoshLangParser(tokens);
      ParseTree tree = parser.program();

      JoshConfigDiscoveryVisitor visitor = new JoshConfigDiscoveryVisitor();
      Set<String> configVariables = visitor.visit(tree);

      // Print each variable on its own line
      for (String variable : configVariables) {
        System.out.println(variable);
      }

      return 0;
    } catch (IOException e) {
      output.printError("Error reading file: " + e.getMessage());
      return DISCOVERY_ERROR_CODE;
    } catch (Exception e) {
      output.printError("Error discovering config variables: " + e.getMessage());
      return DISCOVERY_ERROR_CODE;
    }
  }
}