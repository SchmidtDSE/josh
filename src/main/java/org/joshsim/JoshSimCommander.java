/**
 * Entrypoint for the JoshSim command line interface application.
 *
 * <p>This class serves as the main entry point for the Josh command line interface,
 * providing functionality to validate, run, and manage Josh simulations. It supports
 * both local file operations and integration with cloud storage services like Minio.</p>
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
import org.joshsim.command.RunCommand;
import org.joshsim.command.ServerCommand;
import org.joshsim.command.ValidateCommand;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.parse.ParseError;
import org.joshsim.lang.parse.ParseResult;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine;
import picocli.CommandLine.Command;


@Command(
  name = "joshsim",
  mixinStandardHelpOptions = true,
  version = "1.0",
  description = "JoshSim command line interface",
  subcommands = {
    ValidateCommand.class,
    RunCommand.class,
    ServerCommand.class
  }
)
public class JoshSimCommander {

  private static final int MINIO_ERROR_CODE = 100;
  private static final int UNKNOWN_ERROR_CODE = 404;

  /**
   * Enumeration of possible execution steps in the Josh simulation process.
   * These steps represent the sequential phases of loading, reading, parsing,
   * interpreting, and running a Josh simulation.
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
   * Holds either a successfully initialized program or information about
   * which step in the initialization process failed.
   */
  public static class ProgramInitResult {
    private final Optional<CommanderStepEnum> failureStep;
    private final Optional<JoshProgram> program;

    public ProgramInitResult(CommanderStepEnum failureStep) {
      this.failureStep = Optional.of(failureStep);
      program = Optional.empty();
    }

    public ProgramInitResult(JoshProgram program) {
      this.program = Optional.of(program);
      failureStep = Optional.empty();
    }

    public Optional<CommanderStepEnum> getFailureStep() {
      return failureStep;
    }

    public Optional<JoshProgram> getProgram() {
      return program;
    }
  }

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

    JoshProgram program = JoshSimFacade.interpret(geometryFactory, result);
    assert program != null;

    return new ProgramInitResult(program);
  }

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

      output.printInfo(
        "Successfully uploaded " + file.getName() + " to minio://" + bucketName + "/"
          + objectName
      );
      return true;
    } catch (Exception e) {
      output.printError("Failed to upload to Minio: " + e.getMessage());
      return false;
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new JoshSimCommander()).execute(args);
    System.exit(exitCode);
  }
}
