
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
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.StringLiteralUtil;
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

  /** Refuse to splice in an imported file larger than this many bytes. */
  private static final int MAX_IMPORT_FILE_BYTES = 10 * 1024 * 1024;

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
          entryContent, parsed.getProgram().orElseThrow(), normalizedEntry, stack,
          new ArrayList<>()
      );
      return new PreprocessResult(combined);
    } catch (ImportResolutionException e) {
      return new PreprocessResult(e.getErrors());
    }
  }

  /**
   * Flatten a Josh source into a single self-contained script by inlining every import.
   *
   * <p>Behaves like {@link #preprocess} — the same recursive, order-preserving, verbatim splicing,
   * with the same handling of missing imports, circular imports, and rejected protocol/absolute
   * paths — but additionally enforces the duplicate-entity condition ahead of interpretation so the
   * offending name can be attributed to its originating file and line rather than being lost once
   * the sources are merged. {@code config} references are left untouched; flattening is purely
   * structural.</p>
   *
   * @param entryIdentifier path identifying the entry file, used to resolve its own relative
   *     imports and as the base for nested import resolution.
   * @param entryContent the entry file's raw source text.
   * @return the flattened, self-contained source, or the errors encountered.
   */
  public FlattenResult flatten(String entryIdentifier, String entryContent) {
    ParseResult parsed = new JoshParser().parse(entryContent);
    if (parsed.hasErrors()) {
      return new FlattenResult(parsed.getErrors());
    }

    String normalizedEntry = entryIdentifier.replace('\\', '/');
    Deque<String> stack = new ArrayDeque<>();
    stack.push(normalizedEntry);

    List<FlattenedEntity> entities = new ArrayList<>();
    String combined;
    try {
      combined = spliceImports(
          entryContent, parsed.getProgram().orElseThrow(), normalizedEntry, stack, entities
      );
    } catch (ImportResolutionException e) {
      return new FlattenResult(e.getErrors());
    }

    Optional<ParseError> duplicate = findDuplicateEntity(entities);
    if (duplicate.isPresent()) {
      return new FlattenResult(List.of(duplicate.get()));
    }

    return new FlattenResult(combined);
  }

  private String spliceImports(String content, JoshLangParser.ProgramContext program,
        String identifier, Deque<String> stack, List<FlattenedEntity> collected) {
    String currentDir = dirOf(identifier);

    // Walk imports and entity declarations together in source order so collected entities land in
    // the same order they occupy in the flattened output (nested imports expand in place).
    List<ParserRuleContext> topLevel = new ArrayList<>();
    topLevel.addAll(program.importStatement());
    topLevel.addAll(program.entityStanza());
    topLevel.sort(Comparator.comparingInt(ctx -> ctx.getStart().getStartIndex()));

    StringBuilder result = new StringBuilder();
    int cursor = 0;

    for (ParserRuleContext topLevelCtx : topLevel) {
      if (topLevelCtx instanceof JoshLangParser.EntityStanzaContext entityCtx) {
        // Entity text stays verbatim in the output; only record it for duplicate detection.
        collected.add(new FlattenedEntity(
            entityCtx.getChild(2).getText(),
            entityCtx.getChild(0).getText(),
            entityCtx.getStart().getLine(),
            identifier
        ));
        continue;
      }

      JoshLangParser.ImportStatementContext importCtx =
          (JoshLangParser.ImportStatementContext) topLevelCtx;
      String literal = StringLiteralUtil.stripQuotes(importCtx.path.getText());
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
          importedContent, importedParsed.getProgram().orElseThrow(), targetId, stack, collected
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

  /**
   * Find the first entity name that a {@code start} stanza redeclares after it was already defined.
   *
   * <p>Mirrors the store semantics enforced at interpretation time (see
   * {@code EntityPrototypeStoreBuilder}): a {@code start} stanza declares a new entity and collides
   * with any prior same-named entity, whereas {@code replace} and {@code update} intentionally
   * refer back to an existing one and so are never themselves the offending declaration. Reported
   * ahead of the merge so the duplicate can name its originating file and line.</p>
   *
   * @param entities the entities collected in flattened order.
   * @return the duplicate-entity error, or empty if every declaration is consistent.
   */
  private static Optional<ParseError> findDuplicateEntity(List<FlattenedEntity> entities) {
    Map<String, FlattenedEntity> defined = new HashMap<>();
    for (FlattenedEntity entity : entities) {
      FlattenedEntity prior = defined.get(entity.name);
      if ("start".equals(entity.keyword) && prior != null) {
        String message = String.format(
            "Duplicate entity \"%s\": redeclared at %s line %d (first defined at %s line %d). "
                + "Use \"replace\" or \"update\" to modify an imported entity.",
            entity.name, entity.sourceFile, entity.line, prior.sourceFile, prior.line
        );
        return Optional.of(new ParseError(entity.line, message, Optional.of(entity.sourceFile)));
      }
      // start / replace / update all leave the name defined in the store; register the earliest
      // location so a later duplicate can point back to it.
      defined.putIfAbsent(entity.name, entity);
    }
    return Optional.empty();
  }

  private String readFile(String identifier, JoshLangParser.ImportStatementContext importCtx,
        String referencingIdentifier) {
    byte[] bytes;
    try (InputStream stream = inputStrategy.open(identifier)) {
      bytes = stream.readNBytes(MAX_IMPORT_FILE_BYTES + 1);
    } catch (IOException | RuntimeException e) {
      throw errorAt(importCtx, referencingIdentifier,
          "Error reading imported file \"" + identifier + "\": " + e.getMessage());
    }
    if (bytes.length > MAX_IMPORT_FILE_BYTES) {
      throw errorAt(importCtx, referencingIdentifier,
          "Imported file \"" + identifier + "\" exceeds the " + MAX_IMPORT_FILE_BYTES
              + "-byte import size limit.");
    }
    return new String(bytes, StandardCharsets.UTF_8);
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

  private static ImportResolutionException errorAt(
        JoshLangParser.ImportStatementContext ctx, String identifier, String message) {
    return new ImportResolutionException(
        List.of(new ParseError(ctx.getStart().getLine(), message, Optional.of(identifier))));
  }

  /**
   * A top-level entity declaration recorded during splicing, with the file it originated in.
   *
   * <p>Carries just enough to enforce the duplicate-entity condition and attribute a violation to
   * its source: the declared name, the opening keyword ({@code start} / {@code replace} /
   * {@code update}), the line within its originating file, and that file's identifier.</p>
   */
  private static final class FlattenedEntity {
    private final String name;
    private final String keyword;
    private final int line;
    private final String sourceFile;

    FlattenedEntity(String name, String keyword, int line, String sourceFile) {
      this.name = name;
      this.keyword = keyword;
      this.line = line;
      this.sourceFile = sourceFile;
    }
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
