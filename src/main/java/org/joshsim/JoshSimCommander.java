/**
 * Entrypoint for the JoshSim command line interface application.
 *
 * <p>This class serves as the main entry point for the Josh command line interface, providing
 * functionality to validate, run, and manage Josh simulations. It supports both local file
 * operations and integration with cloud storage services like Minio.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import org.joshsim.command.PreprocessCommand;
import org.joshsim.command.RunCommand;
import org.joshsim.command.ServerCommand;
import org.joshsim.command.ValidateCommand;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayer;
import org.joshsim.lang.parse.ParseError;
import org.joshsim.lang.parse.ParseResult;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine;


/**
 * Entry point for the JoshSim command line.
 *
 * <p>Entry point for the JoshSim command line application, offering various operations like
 * validation, execution, and management of Josh simulations. It facilitates local file interactions
 * as well as integration with Minio cloud storage.</p>
 *
 * @command joshsim
 * @mixinStandardHelpOptions true
 * @version 1.0
 * @description "JoshSim command line interface"
 * @subcommands { ValidateCommand, RunCommand, ServerCommand, PreprocessCommand }
 */
@CommandLine.Command(
    name = "joshsim",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "JoshSim command line interface",
    subcommands = {
        ValidateCommand.class,
        RunCommand.class,
        ServerCommand.class,
        PreprocessCommand.class
    }
)
public class JoshSimCommander {

  private static final int MINIO_ERROR_CODE = 100;
  private static final int UNKNOWN_ERROR_CODE = 404;

  /**
   * Enumeration of possible execution steps in the Josh simulation process.
   */
  public static enum CommanderStepEnum {
    LOAD,
    READ,
    PARSE,
    INTERPRET,
    RUN,
    SUCCESS
  }

  /**
   * Container class for the results of initializing a Josh program.
   */
  public static class ProgramInitResult {
    private final Optional<CommanderStepEnum> failureStep;
    private final Optional<JoshProgram> program;


    /**
     * Constructor for ProgramInitResult when initialization fails.
     *
     * @param failureStep The step where the initialization failed.
     */
    public ProgramInitResult(CommanderStepEnum failureStep) {
      this.failureStep = Optional.of(failureStep);
      program = Optional.empty();
    }

    /**
     * Constructor for ProgramInitResult when initialization is successful.
     *
     * @param program The successfully initialized JoshProgram.
     */
    public ProgramInitResult(JoshProgram program) {
      this.program = Optional.of(program);
      failureStep = Optional.empty();
    }

    /**
     * Returns the step where initialization failed, if any.
     *
     * @return An Optional containing the failure step, or Optional.empty() if initialization was
     *     successful.
     */
    public Optional<CommanderStepEnum> getFailureStep() {
      return failureStep;
    }

    /**
     * Returns the initialized JoshProgram, if any.
     *
     * @return An Optional containing the JoshProgram, or Optional.empty() if initialization failed.
     */
    public Optional<JoshProgram> getProgram() {
      return program;
    }
  }

  /**
   * Retrieves and initializes a Josh program from a file.
   *
   * @param geometryFactory The factory for creating geometry objects.
   * @param file The file containing the Josh program code.
   * @param output Options for handling output messages.
   * @return A ProgramInitResult containing either the initialized JoshProgram or information about
   *     the failure.
   */
  public static ProgramInitResult getJoshProgram(
      EngineGeometryFactory geometryFactory,
      File file,
      OutputOptions output
  ) {
    if (!file.exists()) {
      output.printError("Could not find file: " + file);
      return new ProgramInitResult(CommanderStepEnum.LOAD);
    }

    String fileContent;
    try {
      fileContent = new String(Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
      output.printError("Error in reading input file: " + e.getMessage());
      return new ProgramInitResult(CommanderStepEnum.READ);
    }

    ParseResult result = JoshSimFacade.parse(fileContent);

    if (result.hasErrors()) {
      String leadMessage = String.format("Found errors in Josh code at %s:", file);
      output.printError(leadMessage);

      for (ParseError error : result.getErrors()) {
        String lineMessage = String.format(
            " - On line %d: %s",
            error.getLine(),
            error.getMessage()
        );
        output.printError(lineMessage);
      }

      return new ProgramInitResult(CommanderStepEnum.PARSE);
    }

    InputOutputLayer inputOutputLayer = new JvmInputOutputLayer();
    JoshProgram program = JoshSimFacade.interpret(geometryFactory, result, inputOutputLayer);
    assert program != null;

    return new ProgramInitResult(program);
  }

  /**
   * Saves a file to Minio storage.
   *
   * @param subDirectories The subdirectory path in the Minio bucket
   * @param file The file to upload
   * @param minioOptions Configuration options for Minio connection
   * @param output Options for handling output messages
   * @return true if the upload was successful, false otherwise
   */
  public static boolean saveToMinio(
      String subDirectories,
      File file,
      MinioOptions minioOptions,
      OutputOptions output
  ) {
    try {
      MinioClient minioClient = minioOptions.getMinioClient();
      String bucketName = minioOptions.getBucketName();
      String objectName = minioOptions.getObjectName(subDirectories, file.getName());

      boolean bucketExists = minioClient.bucketExists(
          BucketExistsArgs.builder().bucket(bucketName).build()
      );

      if (!bucketExists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        output.printInfo("Created bucket: " + bucketName);
      }

      output.printInfo(minioOptions.toString());

      minioClient.uploadObject(
          UploadObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .filename(file.getAbsolutePath())
            .build()
      );

      String path = "minio://" + bucketName + "/" + objectName;
      String message = "Successfully uploaded " + file.getName() + " to " + path;
      output.printInfo(message);
      return true;
    } catch (Exception e) {
      output.printError("Failed to upload to Minio: " + e.getMessage());
      return false;
    }
  }

  /**
   * Main entry point for the JoshSim command line interface.
   *
   * @param args Command line arguments passed to the program
   */
  public static void main(String[] args) {
    int exitCode = new CommandLine(new JoshSimCommander()).execute(args);
    System.exit(exitCode);
  }
}
