
/**
 * Preprocessor which resolves {@code import} statements by splicing in referenced files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.io.InputGetterStrategy;


/**
 * Resolves {@code import "path"} statements ahead of the real parse.
 *
 * <p>Each imported file must parse standalone as a complete program; its raw text is then spliced
 * in place of the {@code import} statement that named it, and its own imports are resolved first
 * (relative to its own directory) so nested imports expand depth-first before splicing into the
 * parent. The result is a single combined source string that can be handed to the normal
 * {@link JoshParser} / interpreter pipeline exactly as if it had been written in one file.</p>
 *
 * <p>Import paths are always relative to the directory of the file that references them (not the
 * working directory), joined with {@code /} and {@code ..} regardless of host OS; absolute paths
 * are rejected. Diamond imports (the same file reachable via two different import chains) are not
 * deduplicated — each occurrence is spliced in, so the resulting duplicate entity names are
 * expected to surface as an ordinary duplicate-definition error once interpreted.</p>
 */
public class JoshImportPreprocessor {

  private final InputGetterStrategy inputStrategy;

  /**
   * Create a preprocessor that resolves imports through a per-environment file strategy.
   *
   * @param inputStrategy strategy used to open and check existence of imported files.
   */
  public JoshImportPreprocessor(InputGetterStrategy inputStrategy) {
    this.inputStrategy = inputStrategy;
  }

  /**
   * Resolve all imports reachable from an already-read entry source.
   *
   * @param entryIdentifier path identifying the entry file, used to resolve its own relative
   *     imports and as the base for nested import resolution.
   * @param entryContent the entry file's raw source text.
   * @return the combined source with all imports spliced in, or the errors encountered.
   */
  public PreprocessResult preprocess(String entryIdentifier, String entryContent) {
    ParseResult parsed = new JoshParser().parse(entryContent);
    if (parsed.hasErrors()) {
      return new PreprocessResult(parsed.getErrors());
    }

    String normalizedEntry = entryIdentifier.replace('\\', '/');
    Deque<String> stack = new ArrayDeque<>();
    stack.push(normalizedEntry);

    try {
      String combined = spliceImports(
          entryContent, parsed.getProgram().orElseThrow(), normalizedEntry, stack
      );
      return new PreprocessResult(combined);
    } catch (ImportResolutionException e) {
      return new PreprocessResult(e.getErrors());
    }
  }

  private String spliceImports(String content, JoshLangParser.ProgramContext program,
        String identifier, Deque<String> stack) {
    List<JoshLangParser.ImportStatementContext> imports = program.importStatement();
    if (imports.isEmpty()) {
      return content;
    }

    String currentDir = dirOf(identifier);
    StringBuilder result = new StringBuilder();
    int cursor = 0;

    for (JoshLangParser.ImportStatementContext importCtx : imports) {
      String literal = stripQuotes(importCtx.path.getText());
      String targetId;
      try {
        targetId = resolvePath(currentDir, literal);
      } catch (IllegalArgumentException e) {
        throw errorAt(importCtx, identifier, e.getMessage());
      }

      if (stack.contains(targetId)) {
        List<String> chain = new ArrayList<>(stack);
        Collections.reverse(chain);
        chain.add(targetId);
        throw errorAt(importCtx, identifier, "Circular import: " + String.join(" -> ", chain));
      }
      if (!inputStrategy.exists(targetId)) {
        throw errorAt(importCtx, identifier, "Cannot find imported file: " + targetId);
      }

      String importedContent = readFile(targetId, importCtx, identifier);
      ParseResult importedParsed = new JoshParser().parse(importedContent);
      if (importedParsed.hasErrors()) {
        List<ParseError> attributed = new ArrayList<>();
        for (ParseError error : importedParsed.getErrors()) {
          attributed.add(
              new ParseError(error.getLine(), error.getMessage(), Optional.of(targetId)));
        }
        throw new ImportResolutionException(attributed);
      }

      stack.push(targetId);
      String expandedChild = spliceImports(
          importedContent, importedParsed.getProgram().orElseThrow(), targetId, stack
      );
      stack.pop();

      int start = importCtx.getStart().getStartIndex();
      int stop = importCtx.getStop().getStopIndex();
      result.append(content, cursor, start).append(expandedChild);
      cursor = stop + 1;
    }

    result.append(content, cursor, content.length());
    return result.toString();
  }

  private String readFile(String identifier, JoshLangParser.ImportStatementContext importCtx,
        String referencingIdentifier) {
    try (InputStream stream = inputStrategy.open(identifier)) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException | RuntimeException e) {
      throw errorAt(importCtx, referencingIdentifier,
          "Error reading imported file \"" + identifier + "\": " + e.getMessage());
    }
  }

  private static String resolvePath(String currentDir, String literal) {
    if (literal.startsWith("/") || literal.contains(":")) {
      throw new IllegalArgumentException(
          "Only relative import paths are supported: \"" + literal + "\"");
    }

    String combined = currentDir.isEmpty() ? literal : currentDir + "/" + literal;
    boolean absolute = combined.startsWith("/");
    Deque<String> segments = new ArrayDeque<>();
    for (String segment : combined.split("/")) {
      if (segment.isEmpty() || segment.equals(".")) {
        continue;
      }
      if (segment.equals("..")) {
        if (segments.isEmpty()) {
          throw new IllegalArgumentException(
              "Import path escapes above its root: \"" + literal + "\"");
        }
        segments.removeLast();
      } else {
        segments.addLast(segment);
      }
    }
    String joined = String.join("/", segments);
    return absolute ? "/" + joined : joined;
  }

  private static String dirOf(String identifier) {
    int lastSlash = identifier.lastIndexOf('/');
    return lastSlash < 0 ? "" : identifier.substring(0, lastSlash);
  }

  private static String stripQuotes(String raw) {
    boolean quoted = raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"");
    return quoted ? raw.substring(1, raw.length() - 1) : raw;
  }

  private static ImportResolutionException errorAt(
        JoshLangParser.ImportStatementContext ctx, String identifier, String message) {
    return new ImportResolutionException(
        List.of(new ParseError(ctx.getStart().getLine(), message, Optional.of(identifier))));
  }

  /**
   * Unchecked exception used internally to unwind to {@link #preprocess} with attributed errors.
   */
  private static final class ImportResolutionException extends RuntimeException {
    private final transient List<ParseError> errors;

    ImportResolutionException(List<ParseError> errors) {
      super(errors.get(0).getMessage());
      this.errors = errors;
    }

    List<ParseError> getErrors() {
      return errors;
    }
  }

}
