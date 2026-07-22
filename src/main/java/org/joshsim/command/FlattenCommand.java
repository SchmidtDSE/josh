
/**
 * Command line interface handler for flattening Josh simulation files.
 *
 * <p>This class implements the 'flatten' command which inlines every {@code import "path"}
 * reachable from an entry Josh file into a single self-contained {@code .josh} script.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import org.joshsim.lang.io.JvmWorkingDirInputGetter;
import org.joshsim.lang.parse.FlattenResult;
import org.joshsim.lang.parse.JoshImportPreprocessor;
import org.joshsim.lang.parse.ParseError;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command handler for flattening a Josh model into a single self-contained file.
 *
 * <p>Recursively resolves every top-level {@code import "path"} reachable from the entry file and
 * inlines the imported {@code .josh} content in place, preserving top-level declaration order. The
 * result is written to {@code --output} or standard output. Flattening is purely structural:
 * {@code config} references are left verbatim so plain overlays keep working and are resolved by
 * the engine at run time. Missing imports, circular imports, rejected protocol/absolute paths, and
 * the duplicate-entity condition are reported as errors with a nonzero exit code.</p>
 */
@Command(
    name = "flatten",
    description = "Inline a Josh model's imports into a single self-contained file"
)
public class FlattenCommand implements Callable<Integer> {

  private static final int FILE_NOT_FOUND_CODE = 1;
  private static final int IO_ERROR_CODE = 2;
  private static final int FLATTEN_ERROR_CODE = 3;

  @Parameters(index = "0", description = "Path to the entry Josh file to flatten")
  private File entryFile;

  @Option(
      names = "--import-base",
      description = "Directory used as the resolution root for the entry file's relative imports "
          + "(default: the entry file's own directory)"
  )
  private String importBase;

  @Option(
      names = {"--output", "-o"},
      description = "Write the flattened output to this file instead of standard output"
  )
  private File outputFile;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    if (!entryFile.exists()) {
      output.printError("Could not find file: " + entryFile);
      return FILE_NOT_FOUND_CODE;
    }

    String entryContent;
    try {
      entryContent = new String(Files.readAllBytes(entryFile.toPath()), StandardCharsets.UTF_8);
    } catch (IOException e) {
      output.printError("Error in reading entry file: " + e.getMessage());
      return IO_ERROR_CODE;
    }

    FlattenResult result = new JoshImportPreprocessor(new JvmWorkingDirInputGetter())
        .flatten(resolveEntryIdentifier(), entryContent);

    if (result.hasErrors()) {
      output.printError("Failed to flatten Josh code at " + entryFile + ":");
      for (ParseError error : result.getErrors()) {
        String source = error.getSourceName().orElse(entryFile.toString());
        output.printError(String.format(
            " - %s, line %d: %s", source, error.getLine(), error.getMessage()
        ));
      }
      return FLATTEN_ERROR_CODE;
    }

    String flattened = result.getSource().orElseThrow();
    if (outputFile == null) {
      System.out.print(flattened);
      return 0;
    }

    try {
      Files.write(outputFile.toPath(), flattened.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      output.printError("Error in writing output file: " + e.getMessage());
      return IO_ERROR_CODE;
    }
    output.printInfo("Flattened " + entryFile + " to " + outputFile);
    return 0;
  }

  /**
   * Determine the identifier against which the entry file's relative imports resolve.
   *
   * <p>By default the entry file's own directory is the resolution root, matching a co-located
   * rendered tree. When {@code --import-base} is supplied it overrides that root, letting the entry
   * file live outside the tree its imports point into (e.g. entry-only rendering of plain-overlay
   * models). Nested imports always resolve against the directory of the file that references
   * them.</p>
   *
   * @return the identifier used as the entry file's resolution base.
   */
  private String resolveEntryIdentifier() {
    String entryPath = entryFile.getPath().replace(File.separatorChar, '/');
    if (importBase == null) {
      return entryPath;
    }

    String base = importBase.replace(File.separatorChar, '/');
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    String name = entryFile.getName();
    return base.isEmpty() ? name : base + "/" + name;
  }
}
