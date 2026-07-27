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
 * Unit tests for the {@link InspectExternalsCommand} class.
 *
 * <p>These tests verify that the command reports the external resources a Josh model reads across
 * every read form, resolves imports before reporting, and surfaces failures with a nonzero exit
 * code.</p>
 */
public class InspectExternalsCommandTest {

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
  public void testDiscoversBareRead() throws IOException {
    Path entry = writePatch("  light.step = external solar\n");

    assertEquals(0, run(entry));

    String output = outContent.toString();
    assertTrue(output.contains("\"solar\""), output);
  }

  @Test
  public void testDiscoversEveryReadForm() throws IOException {
    // One entry exercising all seven grammar alternatives that name an external resource, so a
    // newly added read form that is not visited shows up as a missing name here.
    Path entry = writePatch(
        "  a.step = external bare\n"
        + "  b.step = external indexed at index 0\n"
        + "  c.step = external coord at year 2020 year\n"
        + "  d.step = external dated at time \"2020-09-01\"\n"
        + "  e.step = first year of external firstQuery\n"
        + "  f.step = last year of external lastQuery\n"
        + "  g.step = length of external lengthQuery\n"
        + "  h.step = unit of external unitQuery\n"
    );

    assertEquals(0, run(entry));

    String output = outContent.toString();
    for (String expected : new String[] {
        "bare", "indexed", "coord", "dated", "firstQuery", "lastQuery", "lengthQuery", "unitQuery"
    }) {
      assertTrue(output.contains("\"" + expected + "\""), expected + " missing from: " + output);
    }
  }

  @Test
  public void testDiscoversNestedReadInCoordinateExpression() throws IOException {
    // The coordinate expression itself reads a second resource; both must be reported.
    Path entry = writePatch("  a.step = external rainfall at year (first year of external temp)\n");

    assertEquals(0, run(entry));

    String output = outContent.toString();
    assertTrue(output.contains("\"rainfall\""), output);
    assertTrue(output.contains("\"temp\""), output);
  }

  @Test
  public void testDiscoversReadFromImportedFile() throws IOException {
    Files.writeString(
        tempDir.resolve("shared.josh"),
        "start organism Grass\n  height.step = external solar\nend organism\n",
        StandardCharsets.UTF_8
    );
    Path entry = tempDir.resolve("main.josh");
    Files.writeString(
        entry,
        "import \"shared.josh\"\n\nstart patch Default\n  a.step = external local\nend patch\n",
        StandardCharsets.UTF_8
    );

    assertEquals(0, run(entry));

    String output = outContent.toString();
    assertTrue(output.contains("\"solar\""), "imported read missing from: " + output);
    assertTrue(output.contains("\"local\""), output);
  }

  @Test
  public void testResultsAreSortedAndDeduplicated() throws IOException {
    Path entry = writePatch(
        "  a.step = external zebra\n"
        + "  b.step = external apple\n"
        + "  c.step = external zebra\n"
    );

    assertEquals(0, run(entry));

    String output = outContent.toString();
    assertTrue(output.indexOf("\"apple\"") < output.indexOf("\"zebra\""), output);
    assertEquals(
        output.indexOf("\"zebra\""),
        output.lastIndexOf("\"zebra\""),
        "zebra should appear once: " + output
    );
  }

  @Test
  public void testDeclarationAloneIsNotAread() throws IOException {
    // A `start external` stanza declares a source; it does not read one, so it needs no data file.
    Path entry = tempDir.resolve("main.josh");
    Files.writeString(
        entry,
        "start external Declared\n\n  source.units = \"count\"\n\nend external\n",
        StandardCharsets.UTF_8
    );

    assertEquals(0, run(entry));

    String output = outContent.toString();
    assertTrue(output.contains("\"externals\": []"), output);
    assertFalse(output.contains("Declared"), output);
  }

  @Test
  public void testNoExternalsListsEmpty() throws IOException {
    Path entry = writePatch("  a.step = 1 count\n");

    assertEquals(0, run(entry));

    assertTrue(outContent.toString().contains("\"externals\": []"), outContent.toString());
  }

  @Test
  public void testPlainTextOutput() throws IOException {
    Path entry = writePatch("  light.step = external solar\n");

    InspectExternalsCommand command = new InspectExternalsCommand();
    setField(command, "entryFile", entry.toFile());
    setField(command, "plainRequested", true);

    assertEquals(0, command.call());

    String output = outContent.toString();
    assertTrue(output.contains("Externals:"), output);
    assertTrue(output.contains("- solar"), output);
  }

  @Test
  public void testJsonFlagIsNotInverted() throws IOException {
    // Parsed through picocli rather than reflection: a boolean flag defaulting to true is toggled
    // by picocli unless it is negatable, which previously made `--json` emit plain text.
    Path entry = writePatch("  light.step = external solar\n");

    int exitCode = new CommandLine(new InspectExternalsCommand())
        .execute(entry.toString(), "--json");

    assertEquals(0, exitCode);
    assertTrue(outContent.toString().contains("\"externals\""), outContent.toString());
  }

  @Test
  public void testNoJsonFlagSelectsPlainText() throws IOException {
    Path entry = writePatch("  light.step = external solar\n");

    int exitCode = new CommandLine(new InspectExternalsCommand())
        .execute(entry.toString(), "--no-json");

    assertEquals(0, exitCode);
    String output = outContent.toString();
    assertTrue(output.contains("Externals:"), output);
    assertFalse(output.contains("\"externals\""), output);
  }

  @Test
  public void testUnparseableFileReturnsError() throws IOException {
    // A syntax error must fail loudly rather than reporting "no externals", which would silently
    // tell a caller that a broken model needs no data files.
    Path entry = tempDir.resolve("main.josh");
    Files.writeString(entry, "start test\n", StandardCharsets.UTF_8);

    assertEquals(3, run(entry));
    String errOutput = errContent.toString();
    assertTrue(errOutput.contains("line 1"), errOutput);
    assertFalse(outContent.toString().contains("\"externals\""), outContent.toString());
  }

  @Test
  public void testConflictingFormatFlagsRejected() throws IOException {
    Path entry = writePatch("  light.step = external solar\n");

    int exitCode = new CommandLine(new InspectExternalsCommand())
        .execute(entry.toString(), "--json", "--no-json");

    assertEquals(4, exitCode);
    assertTrue(errContent.toString().contains("mutually exclusive"), errContent.toString());
  }

  @Test
  public void testMissingImportReturnsError() throws IOException {
    Path entry = tempDir.resolve("main.josh");
    Files.writeString(entry, "import \"missing.josh\"\n", StandardCharsets.UTF_8);

    assertEquals(3, run(entry));
    assertTrue(errContent.toString().contains("missing.josh"), errContent.toString());
  }

  @Test
  public void testFileNotFoundReturnsError() {
    InspectExternalsCommand command = new InspectExternalsCommand();
    setField(command, "entryFile", tempDir.resolve("does-not-exist.josh").toFile());

    assertEquals(1, command.call());
    assertTrue(errContent.toString().contains("Could not find file"), errContent.toString());
  }

  private Integer run(Path entry) {
    InspectExternalsCommand command = new InspectExternalsCommand();
    setField(command, "entryFile", entry.toFile());
    return command.call();
  }

  private Path writePatch(String body) throws IOException {
    Path entry = tempDir.resolve("main.josh");
    Files.writeString(
        entry,
        "start patch Default\n" + body + "end patch\n",
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
