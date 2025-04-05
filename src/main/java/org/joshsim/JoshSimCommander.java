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
import java.util.concurrent.Callable;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.parse.ParseError;
import org.joshsim.lang.parse.ParseResult;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
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
        JoshSimCommander.ValidateCommand.class
    }
)
public class JoshSimCommander {

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
      if (!file.exists()) {
        output.printError("Could not find file: " + file);
        return 1;
      }

      String fileContent;
      try {
        fileContent = new String(Files.readAllBytes(file.toPath()));
      } catch (IOException e) {
        output.printError("Error in reading input file: " + e.getMessage());
        return 2;
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

        return 3;
      }

      JoshProgram program = JoshSimFacade.interpret(result);
      assert program != null;
      
      output.printInfo("Validated Josh code at " + file);

      if (minioOptions.isMinioOutput()) {
        return saveToMinio("validate", file);
      }

      return 0;
    }

    /**
     * Saves a file to Minio storage.
     *
     * @param file the file to save to Minio
     */
    private Integer saveToMinio(String subDirectories, File file) {
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
        return 0;
      } catch (Exception e) {
        output.printError("Failed to upload to Minio: " + e.getMessage());
        return 2; // Special error code for Minio issues
      }
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
}
