
/**
 * Command line interface handler for validating Josh simulation files.
 *
 * <p>This class implements the 'validate' command which checks Josh script files for syntax
 * errors and other validation issues. It can optionally save validated files to Minio storage.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.util.concurrent.Callable;
import org.joshsim.JoshSimCommander;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;


/**
 * Command handler for validating Josh simulation files.
 * 
 * <p>Processes command line arguments to validate Josh script files, checking for syntax errors
 * and other validation issues. Can optionally save validated files to Minio storage for further
 * processing or deployment.</p>
 */
@Command(
    name = "validate",
    description = "Validate a simulation file"
)
public class ValidateCommand implements Callable<Integer> {
  private static final int MINIO_ERROR_CODE = 100;
  private static final int UNKNOWN_ERROR_CODE = 404;

  @Parameters(index = "0", description = "Path to file to validate")
  private File file;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Mixin
  private MinioOptions minioOptions = new MinioOptions();

  @Override
  public Integer call() {
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

    output.printInfo("Validated Josh code at " + file);

    if (minioOptions.isMinioOutput()) {
      return saveToMinio("validate", file);
    }

    return 0;
  }

  private Integer saveToMinio(String subDirectories, File file) {
    boolean successful = JoshSimCommander.saveToMinio(subDirectories, file, minioOptions, output);
    return successful ? 0 : MINIO_ERROR_CODE;
  }
}
