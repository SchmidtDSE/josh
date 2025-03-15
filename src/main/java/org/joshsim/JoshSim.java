/**
 * Entrypoint for the JoshSim command line interface application.
 *
 * <div>
 * Entrypoint into the JoshSim engine and language avialable via the command line. Commands include:
 *
 * <ul>
 *  <li><strong>validate</strong>: Check if a .josh file is valid and parsable.</li>
 * </ul>
 *
 * </div>
 */
package org.joshsim;

import java.io.File;
import java.util.concurrent.Callable;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.joshsim.lang.JoshLangLexer;
import org.joshsim.lang.JoshLangParser;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Command line interface for JoshSim.
 * 
 * <p>
 * Provides several subcommands like validate and run with options to help users validate simulation 
 * files or execute them through the interface.
 * </p>
 * 
 * @version 1.0
 * @since 1.0
 */
@Command(
  name = "joshsim",
  mixinStandardHelpOptions = true,
  version = "1.0",
  description = "JoshSim command line interface",
  subcommands = {
    JoshSim.ValidateCommand.class
  }
)
public class JoshSim {

  /**
   * Command to validate a simulation file.
   * 
   * <p>
   * Validate command which checks if the given josh DSL code file, yielding exit code of zero if
   * valid and parseable and non-zero otherwise.
   * </p>
   */
  @Command(
    name = "validate",
    description = "Validate a simulation file"
  )
  private static class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to file to validate")
    private File file;

    /**
     * Validates the simulation file specified.
     *
     * <p>
     * This method ensures the file exists and reads the content a JoshLang parser, returning a
     * non-zero exit code if the file is not found or cannot be parsed and a zero exit code
     * otherwise.
     * </p>
     *
     * @return an exit code indicating success (zero) or failure (non-zero).
     */
    @Override
    public Integer call() {
      if (!file.exists()) {
        System.err.println("Could not find file: " + file);
        return 1;
      }

      String fileContent;
      try {
        fileContent = new String(java.nio.file.Files.readAllBytes(file.toPath()));
      } catch (java.io.IOException e) {
        System.err.println("Error in reading input file: " + e.getMessage());
        return 2;
      }

      try {
        CharStream input = CharStreams.fromString(fileContent);
        JoshLangLexer lexer = new JoshLangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JoshLangParser parser = new JoshLangParser(tokens);
        parser.program();
        return 0;
      } catch (Exception parseException) {
        String errorDescription = describeError(parseException);
        System.err.println("Error parsing file. " + errorDescription);
        return 3;
      }
    }    

    /**
     * Describe an error based on a RecognitionException.
     *
     * <p>
     * Extracts the line number and message from the given RecognitionException and return a well-
     * formatted description of the issue that occurred during parsing.
     * </p>
     *
     * @param recognitionException The RecognitionException that occurred.
     * @return A formatted string describing the error, including the line number and message.
     */
    private String describeError(RecognitionException recognitionException) {
      int line = recognitionException.getOffendingToken().getLine();
      String description = recognitionException.getMessage();
      String.format("Issue on line %d: %s", line, description);
    }

    /**
     * Describes an error based on a generic Exception.
     *
     * @param otherException The Exception that occurred.
     * @return A formatted string describing the error message.
     */
    private String describeError(Exception otherException) {
      String.format("Issue: %s", otherException.getMessage());
    }
  }

  /**
   * The main method that serves as the entry point of the JoshSim application.
   *
   * @param args command line arguments passed to the JoshSim application to be parsed by picocli.
   */
  public static void main(String[] args) {
    int exitCode = new CommandLine(new JoshSim()).execute(args);
    System.exit(exitCode);
  }
}
