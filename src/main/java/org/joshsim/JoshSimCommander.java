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

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.joshsim.cloud.EnvCloudApiDataLayer;
import org.joshsim.cloud.JoshSimServer;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.parse.ParseError;
import org.joshsim.lang.parse.ParseResult;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command line interface for Josh.
 *
 * <p>Provides several subcommands like validate and run which check simulation files or execute
 * them via the command line respectively. This is in contrast to using Josh as a library like
 * through JoshSimFacade where client code can perform operations on the platform
 * programmatically.</p>
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
        JoshSimCommander.ValidateCommand.class,
        JoshSimCommander.RunCommand.class,
        JoshSimCommander.ServerCommand.class
    }
)
public class JoshSimCommander {

  private static final int MINIO_ERROR_CODE = 100;
  private static final int UNKNOWN_ERROR_CODE = 404;

  /**
   * Command to validate a simulation file.
   *
   * <p>Validate command which checks if the given josh DSL code file, yielding exit code of zero
   * if valid and parseable and non-zero otherwise.</p>
   */
  @Command(
      name = "validate",
      description = "Validate a simulation file"
  )
  static class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to file to validate")
    private File file;

    @Mixin
    private OutputOptions output = new OutputOptions();

    @Mixin
    private MinioOptions minioOptions = new MinioOptions();

    /**
     * Validates the simulation file specified.
     *
     * <p>Ensure the file exists and read the content through a JoshLang parser, returning a non-
     * zero exit code if the file is not found or cannot be parsed and a zero exit code
     * otherwise.</p>
     *
     * @return an exit code indicating success (zero) or failure (non-zero).
     */
    @Override
    public Integer call() {

      ProgramInitResult initResult = JoshSimCommander.getJoshProgram(
          new GridGeometryFactory(),
          file,
          output
      );

      if (initResult.getFailureStep().isPresent()) {
        CommanderStepEnum failStep = initResult.getFailureStep().get();
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

  /**
   * Command to run a simulation file.
   *
   * <p>Run command which checks if the given josh DSL code file, yielding exit code of zero
   * if valid and parseable and non-zero otherwise. If valid, runs.</p>
   */
  @Command(
      name = "run",
      description = "Run a simulation file"
  )
  static class RunCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to file to validate")
    private File file;

    @Parameters(index = "1", description = "Simulation to run")
    private String simulation;

    @Option(names = "--crs", description = "Coordinate Reference System", defaultValue = "")
    private String crs;

    @Mixin
    private OutputOptions output = new OutputOptions();

    @Mixin
    private MinioOptions minioOptions = new MinioOptions();

    @Option(
        names = "--serial-patches",
        description = "Run patches in serial instead of parallel",
        defaultValue = "false"
    )
    private boolean serialPatches;

    /**
     * Runs the simulation file specified.
     *
     * <p>Ensure the file exists and read the content through a JoshLang parser, returning a non-
     * zero exit code if the file is not found or cannot be parsed and a zero exit code
     * otherwise. If validates, executes.</p>
     *
     * @return an exit code indicating success (zero) or failure (non-zero).
     */
    @Override
    public Integer call() {
      EngineGeometryFactory geometryFactory;
      if (crs.isEmpty()) {
        geometryFactory = new GridGeometryFactory();
      } else {
        CoordinateReferenceSystem crsRealized;
        try {
          crsRealized = CRS.decode(crs);
        } catch (FactoryException e) {
          throw new RuntimeException(e);
        }
        geometryFactory = new EarthGeometryFactory(crsRealized);
      }

      ProgramInitResult initResult = JoshSimCommander.getJoshProgram(geometryFactory, file, output);
      if (initResult.getFailureStep().isPresent()) {
        CommanderStepEnum failStep = initResult.getFailureStep().get();
        return switch (failStep) {
          case LOAD -> 1;
          case READ -> 2;
          case PARSE -> 3;
          default -> UNKNOWN_ERROR_CODE;
        };
      }

      output.printInfo("Validated Josh code at " + file);

      JoshProgram program = initResult.getProgram().orElseThrow();
      if (!program.getSimulations().hasPrototype(simulation)) {
        output.printError("Could not find simulation: " + simulation);
        return 4;
      }

      JoshSimFacade.runSimulation(
          geometryFactory,
          program,
          simulation,
          (step) -> output.printInfo(String.format("Completed step %d.", step)),
          serialPatches
      );

      if (minioOptions.isMinioOutput()) {
        return saveToMinio("run", file);  // TODO
      }

      return 0;
    }

    private Integer saveToMinio(String subDirectories, File file) {
      boolean successful = JoshSimCommander.saveToMinio(subDirectories, file, minioOptions, output);
      return successful ? 0 : MINIO_ERROR_CODE;
    }
  }

  /**
   * The main method that serves as the entry point of the JoshSim application.
   *
   * @param args command line arguments passed to the JoshSim application to be parsed by picocli.
   */
  public static void main(String[] args) {
    int exitCode = new CommandLine(new JoshSimCommander()).execute(args);
    System.exit(exitCode);
  }

  /**
   * Saves a file to Minio storage.
   *
   * @param subDirectories The directory path at which to save.
   * @param file the file to save to Minio
   * @param minioOptions Options for operating Minio
   * @param output Options for output
   * @return True if successful and false otherwise
   */
  private static boolean saveToMinio(String subDirectories, File file, MinioOptions minioOptions,
      OutputOptions output) {
    try {
      // Create MinioClient and get bucket and object names
      MinioClient minioClient = minioOptions.getMinioClient();
      String bucketName = minioOptions.getBucketName();

      // Use the method that supports subdirectories - store in the "validate" subdirectory
      String objectName = minioOptions.getObjectName(subDirectories, file.getName());

      // Check if bucket exists, create if it doesn't
      boolean bucketExists = minioClient.bucketExists(
          BucketExistsArgs.builder().bucket(bucketName).build());

      if (!bucketExists) {
        minioClient.makeBucket(
            MakeBucketArgs.builder().bucket(bucketName).build());
        output.printInfo("Created bucket: " + bucketName);
      }

      // Display configuration details for debugging purposes
      output.printInfo(minioOptions.toString());

      // Upload the file
      minioClient.uploadObject(
          UploadObjectArgs.builder()
          .bucket(bucketName)
          .object(objectName)
          .filename(file.getAbsolutePath())
          .build());

      output.printInfo("Successfully uploaded " + file.getName()
            + " to minio://" + bucketName + "/" + objectName);
      return true;
    } catch (Exception e) {
      output.printError("Failed to upload to Minio: " + e.getMessage());
      return false;
    }
  }

  private static ProgramInitResult getJoshProgram(EngineGeometryFactory geometryFactory, File file,
        OutputOptions output) {
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

  private static enum CommanderStepEnum {
    LOAD,
    READ,
    PARSE,
    INTERPRET,
    RUN,
    SUCCESS
  }

  private static class ProgramInitResult {

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

  /**
   * Command to run the JoshSim server locally.
   */
  @Command(
      name = "server",
      description = "Run the JoshSim server locally"
  )
  static class ServerCommand implements Callable<Integer> {

    @Option(names = "--port", description = "Port number for the server", defaultValue = "8085")
    private int port;

    @Option(
        names = "--concurrent-workers",
        description = "Nubmer of concurrent workers allowed",
        defaultValue = "0"
    )
    private int workers;

    @Option(names = "--worker-url", description = "URL for worker requests", defaultValue = "http://localhost:8085/runReplicate")
    private String workerUrl;

    @Option(names = "--use-http2", description = "Enable HTTP/2 support", defaultValue = "false")
    private boolean useHttp2;

    @Option(
        names = "--serial-patches",
        description = "Run patches in serial instead of parallel",
        defaultValue = "false"
    )
    private boolean serialPatches;

    @Override
    public Integer call() {
      try {
        int numProcessors = Runtime.getRuntime().availableProcessors();

        if (workers == 0) {
          workers = workerUrl.startsWith("localhost") ? 1 : numProcessors - 1;
        }

        JoshSimServer server = new JoshSimServer(
            new EnvCloudApiDataLayer(),
            useHttp2,
            workerUrl,
            port,
            workers,
            serialPatches
        );

        server.start();
        System.out.println("Server started on port " + port);
        System.out.println(
            "Open your browser at http://localhost:" + port + "/ to run simulations"
        );

        // Keep the server running
        Thread.currentThread().join();
        return 0;
      } catch (Exception e) {
        System.err.println("Server error: " + e.getMessage());
        return 1;
      }
    }
  }

}
