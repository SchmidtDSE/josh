/**
 * Entrypoint for the JoshSim command line interface application.
 *
 * <p><div> Entrypoint into the JoshSim engine and language avialable via the command line. Commands
 * include:
 *
 * <ul>
 *   <li><strong>validate</strong>: Check if a .josh file is valid and parsable.
 * </ul>
 *
 * </div>
 */
package org.joshsim;

import java.io.File;
import java.util.concurrent.Callable;
import org.joshsim.lang.parse.ParseError;
import org.joshsim.lang.parse.ParseResult;
import org.joshsim.lang.parse.Parser;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command line interface for JoshSim.
 *
 * <p>Provides several subcommands like validate and run with options to help users validate
 * simulation files or execute them through the interface.
 *
 * @version 1.0
 * @since 1.0
 */
@Command(
    name = "joshsim",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "JoshSim command line interface",
    subcommands = {JoshSim.ValidateCommand.class})
public class JoshSim {

  /**
   * Command to validate a simulation file.
   *
   * <p>Validate command which checks if the given josh DSL code file, yielding exit code of zero if
   * valid and parseable and non-zero otherwise.
   */
  @Command(name = "validate", description = "Validate a simulation file")
  static class ValidateCommand implements Callable<Integer> {

    @Option(names = "--quiet", description = "Suppress output messages")
    private boolean quiet;

    @Parameters(index = "0", description = "Path to file to validate")
    private File file;

    /**
     * Validates the simulation file specified.
     *
     * <p>Ensure the file exists and read the content through a JoshLang parser, returning a non-
     * zero exit code if the file is not found or cannot be parsed and a zero exit code otherwise.
     *
     * @return an exit code indicating success (zero) or failure (non-zero).
     */
    @Override
    public Integer call() {
      if (!file.exists()) {
        printError("Could not find file: " + file);
        return 1;
      }

      String fileContent;
      try {
        fileContent = new String(java.nio.file.Files.readAllBytes(file.toPath()));
      } catch (java.io.IOException e) {
        printError("Error in reading input file: " + e.getMessage());
        return 2;
      }

      Parser parser = new Parser();
      ParseResult result = parser.parse(fileContent);

      if (result.hasErrors()) {
        String leadMessage = String.format("Found errors in Josh code at %s:", file);
        printError(leadMessage);

        for (ParseError error : result.getErrors()) {
          String lineMessage =
              String.format(" - On line %d: %s", error.getLine(), error.getMessage());
          printError(lineMessage);
        }

        return 3;
      } else {
        printOut("Validated Josh code at " + file);
        return 0;
      }
    }

    /**
     * Print a message to standard out if quiet is not enabled.
     *
     * @param message the message to print to standard out.
     */
    private void printOut(String message) {
      if (quiet) {
        return;
      }

      System.out.println(message);
    }

    /**
     * Print a message to standard error if quiet is not enabled.
     *
     * @param message the message to print to standard error.
     */
    private void printError(String message) {
      if (quiet) {
        return;
      }

      System.err.println(message);
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
