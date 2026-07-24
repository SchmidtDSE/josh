
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
import java.util.List;
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

  @Test
  void listImportsReturnsEmptyForProgramWithoutImports() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of());

    ImportsResult result = preprocessor.listImports("main.josh", ENTITY_A);

    assertFalse(result.hasErrors());
    assertTrue(result.getImports().orElseThrow().isEmpty());
  }

  @Test
  void listImportsRecordsSingleImportWithLiteralAndResolvedPath() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of("b.josh", ENTITY_B));
    String entry = ENTITY_A + " import \"b.josh\"";

    ImportsResult result = preprocessor.listImports("main.josh", entry);

    assertFalse(result.hasErrors(), "should list: " + result.getErrors());
    List<ImportRecord> imports = result.getImports().orElseThrow();
    assertEquals(1, imports.size());
    ImportRecord record = imports.get(0);
    assertEquals("b.josh", record.getPath());
    assertEquals("b.josh", record.getResolvedPath());
    assertEquals("main.josh", record.getSourceFile());
  }

  @Test
  void listImportsWalksNestedImportsTransitively() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of(
        "sub/b.josh", ENTITY_B + " import \"c.josh\"",
        "sub/c.josh", "start organism C height.init = 3 m end organism"
    ));
    String entry = ENTITY_A + " import \"sub/b.josh\"";

    ImportsResult result = preprocessor.listImports("main.josh", entry);

    assertFalse(result.hasErrors(), "should list: " + result.getErrors());
    List<ImportRecord> imports = result.getImports().orElseThrow();
    assertEquals(2, imports.size());
    assertEquals("sub/b.josh", imports.get(0).getResolvedPath());
    assertEquals("main.josh", imports.get(0).getSourceFile());
    assertEquals("sub/c.josh", imports.get(1).getResolvedPath());
    assertEquals("sub/b.josh", imports.get(1).getSourceFile());
  }

  @Test
  void listImportsRecordsLineNumberWithinSourceFile() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of("b.josh", ENTITY_B));
    String entry = "start organism A\n  height.init = 1 m\nend organism\nimport \"b.josh\"\n";

    ImportsResult result = preprocessor.listImports("main.josh", entry);

    assertFalse(result.hasErrors(), "should list: " + result.getErrors());
    List<ImportRecord> imports = result.getImports().orElseThrow();
    assertEquals(1, imports.size());
    assertEquals(4, imports.get(0).getLine());
  }

  @Test
  void listImportsDoesNotDeduplicateDiamondImports() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of(
        "b.josh", ENTITY_B + " import \"c.josh\"",
        "d.josh", "start organism D height.init = 4 m end organism import \"c.josh\"",
        "c.josh", "start organism C height.init = 3 m end organism"
    ));
    String entry = ENTITY_A + " import \"b.josh\" import \"d.josh\"";

    ImportsResult result = preprocessor.listImports("main.josh", entry);

    assertFalse(result.hasErrors(), "should list: " + result.getErrors());
    List<ImportRecord> imports = result.getImports().orElseThrow();
    // b.josh, c.josh (via b), d.josh, c.josh (via d) — c.josh appears twice, not deduplicated.
    assertEquals(4, imports.size());
    long countC = imports.stream()
        .filter(r -> r.getResolvedPath().equals("c.josh"))
        .count();
    assertEquals(2, countC);
  }

  @Test
  void listImportsReportsMissingImportAsError() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of());
    String entry = ENTITY_A + " import \"missing.josh\"";

    ImportsResult result = preprocessor.listImports("main.josh", entry);

    assertTrue(result.hasErrors());
    assertTrue(result.getErrors().get(0).getMessage().contains("missing.josh"));
    assertTrue(result.getImports().isEmpty());
  }

  @Test
  void listImportsReportsCircularImportAsError() {
    JoshImportPreprocessor preprocessor = preprocessorFor(Map.of(
        "a.josh", ENTITY_A + " import \"b.josh\"",
        "b.josh", ENTITY_B + " import \"a.josh\""
    ));

    ImportsResult result = preprocessor.listImports("a.josh", ENTITY_A + " import \"b.josh\"");

    assertTrue(result.hasErrors());
    assertTrue(result.getErrors().get(0).getMessage().contains("Circular import"));
  }

}
