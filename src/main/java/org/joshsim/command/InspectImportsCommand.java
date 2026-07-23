/**
 * Command line interface handler for inspecting imports in Josh simulation files.
 *
 * <p>This class implements the 'inspect-imports' command which parses a Josh script file and
 * lists every {@code import "path"} reachable from it (including imports nested inside imported
 * files), without running the simulation.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;
import org.joshsim.lang.io.JvmWorkingDirInputGetter;
import org.joshsim.lang.parse.ImportRecord;
import org.joshsim.lang.parse.ImportsResult;
import org.joshsim.lang.parse.JoshImportPreprocessor;
import org.joshsim.lang.parse.ParseError;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command handler for inspecting imports in Josh simulation files.
 *
 * <p>Parses a Josh script file and resolves every top-level {@code import "path"} reachable from
 * it, transitively. Outputs each import's literal path, its path resolved relative to the entry
 * file, the file that contains the statement, and the line number, in JSON format for programmatic
 * consumption (e.g. by joshpy, which needs to know which files make up a model).</p>
 */
@Command(
    name = "inspect-imports",
    description = "List all import paths reachable from a Josh model"
)
public class InspectImportsCommand implements Callable<Integer> {

  private static final int FILE_NOT_FOUND_CODE = 1;
  private static final int IO_ERROR_CODE = 2;
  private static final int INSPECT_ERROR_CODE = 3;

  @Parameters(index = "0", description = "Path to the entry Josh file to inspect")
  private File entryFile;

  @Option(
      names = "--import-base",
      description = "Directory used as the resolution root for the entry file's relative imports "
          + "(default: the entry file's own directory)"
  )
  private String importBase;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Option(
      names = "--json",
      description = "Output in JSON format (default: true)",
      defaultValue = "true"
  )
  private boolean jsonOutput = true;

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

    ImportsResult result = new JoshImportPreprocessor(new JvmWorkingDirInputGetter())
        .listImports(resolveEntryIdentifier(), entryContent);

    if (result.hasErrors()) {
      output.printError("Failed to inspect imports in Josh code at " + entryFile + ":");
      for (ParseError error : result.getErrors()) {
        String source = error.getSourceName().orElse(entryFile.toString());
        output.printError(String.format(
            " - %s, line %d: %s", source, error.getLine(), error.getMessage()
        ));
      }
      return INSPECT_ERROR_CODE;
    }

    List<ImportRecord> imports = result.getImports().orElseThrow();
    if (jsonOutput) {
      outputJson(imports);
    } else {
      outputPlain(imports);
    }
    return 0;
  }

  private void outputJson(List<ImportRecord> imports) {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"entry\": \"").append(escapeJson(resolveEntryIdentifier())).append("\",\n");
    json.append("  \"imports\": [");

    if (imports.isEmpty()) {
      json.append("]\n");
    } else {
      json.append("\n");
      for (int i = 0; i < imports.size(); i++) {
        ImportRecord record = imports.get(i);
        json.append("    {\n");
        json.append("      \"path\": \"").append(escapeJson(record.getPath())).append("\",\n");
        json.append("      \"resolvedPath\": \"")
            .append(escapeJson(record.getResolvedPath())).append("\",\n");
        json.append("      \"sourceFile\": \"")
            .append(escapeJson(record.getSourceFile())).append("\",\n");
        json.append("      \"line\": ").append(record.getLine()).append("\n");
        json.append("    }");
        json.append(i < imports.size() - 1 ? ",\n" : "\n");
      }
      json.append("  ]\n");
    }

    json.append("}");
    output.printInfo(json.toString());
  }

  private void outputPlain(List<ImportRecord> imports) {
    output.printInfo("Entry: " + resolveEntryIdentifier());
    output.printInfo("Imports:");
    if (imports.isEmpty()) {
      output.printInfo("  (none)");
      return;
    }
    for (ImportRecord record : imports) {
      output.printInfo(String.format(
          "  - %s (resolved: %s, source: %s:%d)",
          record.getPath(),
          record.getResolvedPath(),
          record.getSourceFile(),
          record.getLine()
      ));
    }
  }

  /**
   * Determine the identifier against which the entry file's relative imports resolve.
   *
   * <p>By default the entry file's own directory is the resolution root, matching {@code flatten}.
   * When {@code --import-base} is supplied it overrides that root, letting the entry file live
   * outside the tree its imports point into. Nested imports always resolve against the directory of
   * the file that references them.</p>
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

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
