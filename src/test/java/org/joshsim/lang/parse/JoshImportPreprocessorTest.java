
/**
 * Tests for JoshImportPreprocessor.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.lang.io.SandboxInputGetter;
import org.joshsim.lang.io.VirtualFile;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for splicing {@code import} statements into a single combined source.
 */
class JoshImportPreprocessorTest {

  private static final String ENTITY_A =
      "start organism A "
      + "height.init = 1 m "
      + "end organism";

  private static final String ENTITY_B =
      "start organism B "
      + "height.init = 2 m "
      + "end organism";

  private JoshImportPreprocessor preprocessorFor(Map<String, String> files) {
    Map<String, VirtualFile> virtualFiles = new HashMap<>();
    for (Map.Entry<String, String> entry : files.entrySet()) {
      virtualFiles.put(entry.getKey(), new VirtualFile(entry.getKey(), entry.getValue(), false));
    }
    return new JoshImportPreprocessor(new SandboxInputGetter(virtualFiles));
  }

  @Test
  void programWithoutImportsPassesThroughUnchanged() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of());
    PreprocessResult result = preprocessor.preprocess("main.josh", ENTITY_A);

    assertFalse(result.hasErrors());
    assertEquals(ENTITY_A, result.getSource().orElseThrow());
  }

  @Test
  void splicesOneImportInPlace() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of("b.josh", ENTITY_B));
    String entry = ENTITY_A + " import \"b.josh\"";

    PreprocessResult result = preprocessor.preprocess("main.josh", entry);

    assertFalse(result.hasErrors());
    ParseResult reparsed = new JoshParser().parse(result.getSource().orElseThrow());
    assertFalse(reparsed.hasErrors(), "spliced source should parse: " + reparsed.getErrors());
    assertEquals(2, reparsed.getProgram().orElseThrow().entityStanza().size());
  }

  @Test
  void resolvesNestedImportsRelativeToTheImportingFileNotTheEntry() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of(
        "sub/b.josh", ENTITY_B + " import \"c.josh\"",
        "sub/c.josh", "start organism C height.init = 3 m end organism"
    ));
    String entry = ENTITY_A + " import \"sub/b.josh\"";

    PreprocessResult result = preprocessor.preprocess("main.josh", entry);

    assertFalse(result.hasErrors(), "should resolve: " + result.getErrors());
    ParseResult reparsed = new JoshParser().parse(result.getSource().orElseThrow());
    assertFalse(reparsed.hasErrors(), "spliced source should parse: " + reparsed.getErrors());
    assertEquals(3, reparsed.getProgram().orElseThrow().entityStanza().size());
  }

  @Test
  void climbingOutOfSubdirectoryResolvesAgainstImportingFilesDirectory() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of(
        "sub/b.josh", ENTITY_B + " import \"../d.josh\"",
        "d.josh", "start organism D height.init = 4 m end organism"
    ));
    String entry = ENTITY_A + " import \"sub/b.josh\"";

    PreprocessResult result = preprocessor.preprocess("main.josh", entry);

    assertFalse(result.hasErrors(), "should resolve: " + result.getErrors());
  }

  @Test
  void resolvesImportsRelativeToAnAbsoluteEntryPath() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of("/base/b.josh", ENTITY_B));
    String entry = ENTITY_A + " import \"b.josh\"";

    PreprocessResult result = preprocessor.preprocess("/base/main.josh", entry);

    assertFalse(result.hasErrors(), "should resolve against the absolute entry directory: "
        + result.getErrors());
  }

  @Test
  void rejectsCircularImports() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of(
        "a.josh", ENTITY_A + " import \"b.josh\"",
        "b.josh", ENTITY_B + " import \"a.josh\""
    ));

    PreprocessResult result = preprocessor.preprocess("a.josh", ENTITY_A + " import \"b.josh\"");

    assertTrue(result.hasErrors());
    assertTrue(result.getErrors().get(0).getMessage().contains("Circular import"));
  }

  @Test
  void rejectsMissingImportedFile() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of());
    String entry = ENTITY_A + " import \"missing.josh\"";

    PreprocessResult result = preprocessor.preprocess("main.josh", entry);

    assertTrue(result.hasErrors());
    assertTrue(result.getErrors().get(0).getMessage().contains("missing.josh"));
  }

  @Test
  void rejectsAbsoluteImportPaths() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of());
    String entry = ENTITY_A + " import \"/etc/other.josh\"";

    PreprocessResult result = preprocessor.preprocess("main.josh", entry);

    assertTrue(result.hasErrors());
  }

  @Test
  void rejectsImportPathsThatEscapeAboveTheRoot() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of());
    String entry = ENTITY_A + " import \"../escape.josh\"";

    PreprocessResult result = preprocessor.preprocess("main.josh", entry);

    assertTrue(result.hasErrors());
    assertTrue(result.getErrors().get(0).getMessage().contains("escapes above its root"));
  }

  @Test
  void attributesSyntaxErrorInImportedFileToThatFile() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of("b.josh", "start organism B"));
    String entry = ENTITY_A + " import \"b.josh\"";

    PreprocessResult result = preprocessor.preprocess("main.josh", entry);

    assertTrue(result.hasErrors());
    assertEquals("b.josh", result.getErrors().get(0).getSourceName().orElseThrow());
  }

  @Test
  void entrySyntaxErrorsAreNotAttributedToAnImportedFile() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of());
    PreprocessResult result = preprocessor.preprocess("main.josh", "start organism A");

    assertTrue(result.hasErrors());
    assertTrue(result.getErrors().get(0).getSourceName().isEmpty());
  }

  @Test
  void flattenInlinesImportsIntoParsableSingleSource() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of("b.josh", ENTITY_B));
    String entry = ENTITY_A + " import \"b.josh\"";

    FlattenResult result = preprocessor.flatten("main.josh", entry);

    assertFalse(result.hasErrors(), "should flatten: " + result.getErrors());
    ParseResult reparsed = new JoshParser().parse(result.getSource().orElseThrow());
    assertFalse(reparsed.hasErrors(), "flattened source should parse: " + reparsed.getErrors());
    assertEquals(2, reparsed.getProgram().orElseThrow().entityStanza().size());
  }

  @Test
  void flattenMatchesPreprocessOutputWhenThereAreNoDuplicates() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of("b.josh", ENTITY_B));
    String entry = ENTITY_A + " import \"b.josh\"";

    FlattenResult flattened = preprocessor.flatten("main.josh", entry);
    PreprocessResult preprocessed = preprocessor.preprocess("main.josh", entry);

    assertEquals(preprocessed.getSource().orElseThrow(), flattened.getSource().orElseThrow());
  }

  @Test
  void flattenIsDeterministic() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of(
        "b.josh", ENTITY_B + " import \"c.josh\"",
        "c.josh", "start organism C height.init = 3 m end organism"
    ));
    String entry = ENTITY_A + " import \"b.josh\"";

    String first = preprocessor.flatten("main.josh", entry).getSource().orElseThrow();
    String second = preprocessor.flatten("main.josh", entry).getSource().orElseThrow();

    assertEquals(first, second);
  }

  @Test
  void flattenReportsDuplicateEntityWithOffendingSourceAndLine() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of("b.josh", ENTITY_A));
    String entry = ENTITY_A + " import \"b.josh\"";

    FlattenResult result = preprocessor.flatten("main.josh", entry);

    assertTrue(result.hasErrors());
    ParseError error = result.getErrors().get(0);
    assertTrue(error.getMessage().contains("Duplicate entity"), error.getMessage());
    assertTrue(error.getMessage().contains("\"A\""), error.getMessage());
    assertEquals("b.josh", error.getSourceName().orElseThrow());
  }

  @Test
  void flattenDoesNotFlagReplaceOfAnImportedEntity() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of(
        "b.josh", "replace organism A height.init = 9 m end organism"
    ));
    String entry = ENTITY_A + " import \"b.josh\"";

    FlattenResult result = preprocessor.flatten("main.josh", entry);

    assertFalse(result.hasErrors(), "replace should not be a duplicate: " + result.getErrors());
  }

  @Test
  void flattenDoesNotFlagUpdateOfAnImportedEntity() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of(
        "b.josh", "update organism A height.init = 9 m end organism"
    ));
    String entry = ENTITY_A + " import \"b.josh\"";

    FlattenResult result = preprocessor.flatten("main.josh", entry);

    assertFalse(result.hasErrors(), "update should not be a duplicate: " + result.getErrors());
  }

  @Test
  void flattenRejectsProtocolImports() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of());
    String entry = ENTITY_A + " import \"https://example.com/remote.josh\"";

    FlattenResult result = preprocessor.flatten("main.josh", entry);

    assertTrue(result.hasErrors());
    assertTrue(result.getErrors().get(0).getMessage().contains("relative"));
  }

  @Test
  void flattenRejectsCircularImports() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of(
        "a.josh", ENTITY_A + " import \"b.josh\"",
        "b.josh", ENTITY_B + " import \"a.josh\""
    ));

    FlattenResult result = preprocessor.flatten("a.josh", ENTITY_A + " import \"b.josh\"");

    assertTrue(result.hasErrors());
    assertTrue(result.getErrors().get(0).getMessage().contains("Circular import"));
  }

  @Test
  void flattenReportsMissingImportTarget() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of());
    String entry = ENTITY_A + " import \"missing.josh\"";

    FlattenResult result = preprocessor.flatten("main.josh", entry);

    assertTrue(result.hasErrors());
    assertTrue(result.getErrors().get(0).getMessage().contains("missing.josh"));
  }

}
