package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Unit tests for the {@link InspectImportsCommand} class.
 *
 * <p>These tests verify that the command resolves and lists every {@code import} reachable from an
 * entry Josh file (including nested imports) and reports resolution failures with a nonzero exit
 * code.</p>
 */
public class InspectImportsCommandTest {

  private static final String ORGANISM_A =
      "start organism A\n  height.init = 1 m\nend organism\n";
  private static final String ORGANISM_B =
      "start organism B\n  height.init = 2 m\nend organism\n";
  private static final String ORGANISM_C =
      "start organism C\n  height.init = 3 m\nend organism\n";

  private ByteArrayOutputStream outContent;
  private ByteArrayOutputStream errContent;
  private PrintStream originalOut;
  private PrintStream originalErr;

  @TempDir
  Path tempDir;

  /**
   * Sets up output stream capture for both stdout and stderr.
   */
  @BeforeEach
  public void setUp() {
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    originalOut = System.out;
    originalErr = System.err;
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  /**
   * Restores the original stdout and stderr streams.
   */
  @AfterEach
  public void tearDown() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  public void testListsImportsAsJson() throws IOException {
    Path entry = writeEntryWithImports();

    InspectImportsCommand command = new InspectImportsCommand();
    setField(command, "entryFile", entry.toFile());

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString();
    assertTrue(output.contains("\"path\": \"b.josh\""), output);
    assertTrue(output.contains("\"path\": \"sub/c.josh\""), output);
    assertTrue(output.contains("\"resolvedPath\""), output);
    assertTrue(output.contains("\"sourceFile\""), output);
    assertTrue(output.contains("\"line\""), output);
  }

  @Test
  public void testListsImportsAsPlainText() throws IOException {
    Path entry = writeEntryWithImports();

    // Parsed through picocli rather than by setting a field, so the flag semantics are covered.
    int exitCode = new CommandLine(new InspectImportsCommand())
        .execute(entry.toString(), "--no-json");
    assertEquals(0, exitCode);

    String output = outContent.toString();
    assertTrue(output.contains("Imports:"), output);
    assertTrue(output.contains("- b.josh"), output);
    assertTrue(output.contains("- sub/c.josh"), output);
  }

  @Test
  public void testJsonFlagIsNotInverted() throws IOException {
    // Regression: `--json` used to emit plain text, because picocli sets a matched boolean flag to
    // the opposite of its default and the option was declared with defaultValue = "true".
    Path entry = writeEntryWithImports();

    int exitCode = new CommandLine(new InspectImportsCommand())
        .execute(entry.toString(), "--json");

    assertEquals(0, exitCode);
    String output = outContent.toString();
    assertTrue(output.contains("\"imports\""), output);
    assertFalse(output.contains("Imports:"), output);
  }

  @Test
  public void testJsonIsStillTheDefault() throws IOException {
    Path entry = writeEntryWithImports();

    int exitCode = new CommandLine(new InspectImportsCommand()).execute(entry.toString());

    assertEquals(0, exitCode);
    assertTrue(outContent.toString().contains("\"imports\""), outContent.toString());
  }

  @Test
  public void testPlainAliasSelectsPlainText() throws IOException {
    Path entry = writeEntryWithImports();

    int exitCode = new CommandLine(new InspectImportsCommand())
        .execute(entry.toString(), "--plain");

    assertEquals(0, exitCode);
    assertTrue(outContent.toString().contains("Imports:"), outContent.toString());
  }

  @Test
  public void testConflictingFormatFlagsRejected() throws IOException {
    Path entry = writeEntryWithImports();

    int exitCode = new CommandLine(new InspectImportsCommand())
        .execute(entry.toString(), "--json", "--no-json");

    assertEquals(4, exitCode);
    assertTrue(errContent.toString().contains("mutually exclusive"), errContent.toString());
  }

  @Test
  public void testNoImportsListsEmpty() throws IOException {
    Path entry = tempDir.resolve("main.josh");
    Files.writeString(entry, ORGANISM_A, StandardCharsets.UTF_8);

    InspectImportsCommand command = new InspectImportsCommand();
    setField(command, "entryFile", entry.toFile());

    Integer result = command.call();
    assertEquals(0, result);

    String output = outContent.toString();
    assertTrue(output.contains("\"imports\": []"), output);
  }

  @Test
  public void testMissingImportReturnsError() throws IOException {
    Path entry = tempDir.resolve("main.josh");
    Files.writeString(entry, ORGANISM_A + "import \"missing.josh\"\n", StandardCharsets.UTF_8);

    InspectImportsCommand command = new InspectImportsCommand();
    setField(command, "entryFile", entry.toFile());

    Integer result = command.call();
    assertEquals(3, result);

    String errOutput = errContent.toString();
    assertTrue(errOutput.contains("missing.josh"), errOutput);
  }

  @Test
  public void testFileNotFoundReturnsError() {
    InspectImportsCommand command = new InspectImportsCommand();
    setField(command, "entryFile", tempDir.resolve("does-not-exist.josh").toFile());

    Integer result = command.call();
    assertEquals(1, result);

    String errOutput = errContent.toString();
    assertTrue(errOutput.contains("Could not find file"), errOutput);
  }

  private Path writeEntryWithImports() throws IOException {
    Files.writeString(tempDir.resolve("b.josh"), ORGANISM_B, StandardCharsets.UTF_8);
    Path subDir = tempDir.resolve("sub");
    Files.createDirectories(subDir);
    Files.writeString(subDir.resolve("c.josh"), ORGANISM_C, StandardCharsets.UTF_8);
    Path entry = tempDir.resolve("main.josh");
    Files.writeString(
        entry,
        ORGANISM_A + "import \"b.josh\"\nimport \"sub/c.josh\"\n",
        StandardCharsets.UTF_8
    );
    return entry;
  }

  /**
   * Helper method to set private fields using reflection.
   *
   * @param target The object to modify.
   * @param fieldName The name of the field to set.
   * @param value The value to set.
   */
  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }
}
