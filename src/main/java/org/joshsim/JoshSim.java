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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.io.File;
import java.util.concurrent.Callable;

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
    static class ValidateCommand implements Callable<Integer> {

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
                org.antlr.v4.runtime.CharStream input = org.antlr.v4.runtime.CharStreams.fromString(fileContent);
                org.joshsim.lang.JoshLangLexer lexer = new org.joshsim.lang.JoshLangLexer(input);
                org.antlr.v4.runtime.CommonTokenStream tokens = new org.antlr.v4.runtime.CommonTokenStream(lexer);
                org.joshsim.lang.JoshLangParser parser = new org.joshsim.lang.JoshLangParser(tokens);
                parser.identifier(); // Start parsing from root rule
                return 0;
            } catch (Exception e) {
                System.err.println("Error parsing file: " + e.getMessage());
                return 3;
            }
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
