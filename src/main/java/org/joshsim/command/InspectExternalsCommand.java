/**
 * Command line interface handler for inspecting external data references in Josh files.
 *
 * <p>This class implements the 'inspect-externals' command which parses a Josh script file and
 * lists every external data resource it reads, including reads that appear in imported files,
 * without running the simulation.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.visitor.JoshExternalDiscoveryVisitor;
import org.joshsim.lang.io.JvmWorkingDirInputGetter;
import org.joshsim.lang.parse.FlattenResult;
import org.joshsim.lang.parse.JoshImportPreprocessor;
import org.joshsim.lang.parse.JoshParser;
import org.joshsim.lang.parse.ParseError;
import org.joshsim.lang.parse.ParseResult;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command handler for inspecting external data references in Josh simulation files.
 *
 * <p>Parses a Josh script file and reports the name of every external resource it reads, so that
 * build systems and tooling can determine which preprocessed {@code .jshd} / {@code .jshdz} files a
 * model requires. Names are reported without a file extension, matching the identifier used in the
 * script; the corresponding data file is that name plus a {@code .jshd} or {@code .jshdz}
 * extension.</p>
 *
 * <p>Imports are resolved first, so a resource read only by an imported file is still reported.
 * Every read form contributes, including the temporal metadata queries such as
 * {@code first year of external precipitation}.</p>
 */
@Command(
    name = "inspect-externals",
    description = "List all external data resources a Josh model reads"
)
public class InspectExternalsCommand implements Callable<Integer> {

  private static final int FILE_NOT_FOUND_CODE = 1;
  private static final int IO_ERROR_CODE = 2;
  private static final int INSPECT_ERROR_CODE = 3;
  private static final int USAGE_ERROR_CODE = 4;

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
      description = "Output in JSON format. This is the default and the flag is accepted for "
          + "explicitness."
  )
  private boolean jsonRequested;

  /**
   * Selects plain text output.
   *
   * <p>Plain text is selected by its own flag rather than by negating {@code --json}, because
   * picocli sets a matched boolean flag to the opposite of its default: a {@code --json} flag
   * defaulting to true would turn JSON off when passed, which is the opposite of what its name
   * says. Both flags default to false, where picocli's behavior is unambiguous.</p>
   */
  @Option(
      names = {"--no-json", "--plain"},
      description = "Output plain text instead of JSON."
  )
  private boolean plainRequested;

  @Override
  public Integer call() {
    if (jsonRequested && plainRequested) {
      output.printError("--json and --no-json are mutually exclusive");
      return USAGE_ERROR_CODE;
    }

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

    // Resolve imports first so that a resource read only by an imported file is still discovered.
    FlattenResult flattened = new JoshImportPreprocessor(new JvmWorkingDirInputGetter())
        .flatten(resolveEntryIdentifier(), entryContent);

    if (flattened.hasErrors()) {
      // This step parses as well as resolving, so a plain syntax error surfaces here too.
      output.printError("Failed to parse or resolve imports in Josh code at " + entryFile + ":");
      for (ParseError error : flattened.getErrors()) {
        String source = error.getSourceName().orElse(entryFile.toString());
        output.printError(String.format(
            " - %s, line %d: %s", source, error.getLine(), error.getMessage()
        ));
      }
      return INSPECT_ERROR_CODE;
    }

    ParseResult parsed = new JoshParser().parse(flattened.getSource().orElseThrow());
    if (parsed.hasErrors()) {
      output.printError("Failed to parse Josh code at " + entryFile + ":");
      for (ParseError error : parsed.getErrors()) {
        output.printError(String.format(" - line %d: %s", error.getLine(), error.getMessage()));
      }
      return INSPECT_ERROR_CODE;
    }

    Set<String> externals;
    try {
      externals = discoverExternals(parsed.getProgram().orElseThrow());
    } catch (Exception e) {
      output.printError("Error discovering external references: " + e.getMessage());
      return INSPECT_ERROR_CODE;
    }

    List<String> sorted = new ArrayList<>(externals);
    if (plainRequested) {
      outputPlain(sorted);
    } else {
      outputJson(sorted);
    }
    return 0;
  }

  /**
   * Walks the parse tree and collects external resource names.
   *
   * @param program The parsed program, with imports already inlined.
   * @return The discovered resource names in deterministic sorted order.
   */
  private Set<String> discoverExternals(JoshLangParser.ProgramContext program) {
    Set<String> discovered = new JoshExternalDiscoveryVisitor().visit(program);
    return new TreeSet<>(discovered == null ? Set.of() : discovered);
  }

  private void outputJson(List<String> externals) {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"entry\": \"").append(escapeJson(resolveEntryIdentifier())).append("\",\n");
    json.append("  \"externals\": [");

    if (externals.isEmpty()) {
      json.append("]\n");
    } else {
      json.append("\n");
      for (int i = 0; i < externals.size(); i++) {
        json.append("    \"").append(escapeJson(externals.get(i))).append("\"");
        json.append(i < externals.size() - 1 ? ",\n" : "\n");
      }
      json.append("  ]\n");
    }

    json.append("}");
    output.printInfo(json.toString());
  }

  private void outputPlain(List<String> externals) {
    output.printInfo("Entry: " + resolveEntryIdentifier());
    output.printInfo("Externals:");
    if (externals.isEmpty()) {
      output.printInfo("  (none)");
      return;
    }
    for (String name : externals) {
      output.printInfo("  - " + name);
    }
  }

  /**
   * Determine the identifier against which the entry file's relative imports resolve.
   *
   * <p>By default the entry file's own directory is the resolution root, matching {@code flatten}
   * and {@code inspect-imports}. When {@code --import-base} is supplied it overrides that root,
   * letting the entry file live outside the tree its imports point into. Nested imports always
   * resolve against the directory of the file that references them.</p>
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
