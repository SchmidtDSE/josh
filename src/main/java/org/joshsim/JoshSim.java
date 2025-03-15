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

@Command(
    name = "joshsim",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "JoshSim command line interface",
    subcommands = {
        JoshSim.ValidateCommand.class,
        JoshSim.RunCommand.class
    }
)
public class JoshSim {

    @Command(
        name = "validate",
        description = "Validate a simulation file"
    )
    static class ValidateCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to file to validate")
        private File file;

        @Override
        public Integer call() {
            if (!file.exists()) {
                System.err.println("Error: File does not exist: " + file);
                return 1;
            }
            
            // TODO: Implement actual validation logic here
            System.out.println("Validating file: " + file);
            return 0;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JoshSim()).execute(args);
        System.exit(exitCode);
    }
}
