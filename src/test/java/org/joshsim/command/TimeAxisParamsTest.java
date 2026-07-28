/**
 * Tests for TimeAxisParams.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.joshsim.command.TimeAxisParams.Field;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;


/**
 * Unit tests for {@link TimeAxisParams}.
 *
 * <p>The declared time axis crosses three boundaries on its way from a dispatcher to the
 * {@code preprocess} command, so these tests pin the names on both sides of each one.</p>
 */
class TimeAxisParamsTest {

  private static final TimeAxisParams DECLARED = TimeAxisParams.of(
      "count", "2015", "year", "86", "1", "", "");

  @Test
  void fieldNamesAreCamelCased() {
    assertEquals("timeType", Field.TYPE.getFieldName());
    assertEquals("timeStart", Field.START.getFieldName());
    assertEquals("timeIncrement", Field.INCREMENT.getFieldName());
    assertEquals("timeInstant", Field.INSTANT.getFieldName());
  }

  @Test
  void cliFlagsAreKebabCased() {
    assertEquals("--time-type", Field.TYPE.getCliFlag());
    assertEquals("--time-start", Field.START.getCliFlag());
    assertEquals("--time-increment", Field.INCREMENT.getCliFlag());
    assertEquals("--time-instant", Field.INSTANT.getCliFlag());
  }

  @Test
  void emptyDeclaresNothing() {
    assertFalse(TimeAxisParams.EMPTY.isDeclared());
    assertEquals("", TimeAxisParams.EMPTY.get(Field.START));
    assertTrue(TimeAxisParams.EMPTY.toFormFields().isEmpty());
    assertEquals("", TimeAxisParams.EMPTY.toCliFlags());
  }

  @Test
  void nullsAreTreatedAsBlank() {
    TimeAxisParams params = TimeAxisParams.of(null, null, null, null, null, null, null);
    assertFalse(params.isDeclared());
    assertEquals("", params.get(Field.INTERVAL));
  }

  @Test
  void typeAloneDoesNotDeclareAnAxis() {
    // Matches the CLI contract: --time-type only shapes an axis something else establishes.
    assertFalse(TimeAxisParams.of("ISO", "", "", "", "", "", "").isDeclared());
  }

  @Test
  void incrementAloneDoesNotDeclareAnAxis() {
    assertFalse(TimeAxisParams.of("", "", "", "", "5", "", "").isDeclared());
  }

  @Test
  void anyCoordinateFieldDeclaresAnAxis() {
    assertTrue(TimeAxisParams.of("", "2015", "", "", "", "", "").isDeclared());
    assertTrue(TimeAxisParams.of("", "", "year", "", "", "", "").isDeclared());
    assertTrue(TimeAxisParams.of("", "", "", "3", "", "", "").isDeclared());
    assertTrue(TimeAxisParams.of("", "", "", "", "", "P1M", "").isDeclared());
    assertTrue(TimeAxisParams.of("", "", "", "", "", "", "2024").isDeclared());
  }

  @Test
  void typeDefaultsToCountWhenNotDeclared() {
    assertEquals("count", TimeAxisParams.EMPTY.getType());
    assertEquals("ISO", TimeAxisParams.of("ISO", "2024-01-01", "", "3", "", "P1Y", "").getType());
  }

  @Test
  void formFieldsOmitBlankValues() {
    Map<String, String> fields = DECLARED.toFormFields();
    assertEquals(
        Map.of("timeType", "count", "timeStart", "2015", "timeUnit", "year",
            "timeCount", "86", "timeIncrement", "1"),
        fields);
  }

  @Test
  void formFieldsRoundTripThroughLookup() {
    // The dispatch seam: HttpPreprocessTarget encodes, JoshSimPreprocessBatchHandler decodes.
    Map<String, String> encoded = DECLARED.toFormFields();
    TimeAxisParams decoded = TimeAxisParams.fromLookup(
        field -> encoded.getOrDefault(field.getFieldName(), ""));

    for (Field field : Field.values()) {
      assertEquals(DECLARED.get(field), decoded.get(field), field.getFieldName());
    }
    assertTrue(decoded.isDeclared());
  }

  @Test
  void cliFlagsCarryOnlyDeclaredValues() {
    assertEquals(
        "--time-type=count --time-start=2015 --time-unit=year --time-count=86 --time-increment=1",
        DECLARED.toCliFlags());
    assertEquals("--time-type=ISO --time-instant=2024-06-01",
        TimeAxisParams.of("ISO", "", "", "", "", "", "2024-06-01").toCliFlags());
  }

  @Test
  void cliFlagsParseAsPreprocessOptions() {
    // preprocess-entrypoint.sh appends this string verbatim to a `preprocess` invocation, so a flag
    // name that drifted from PreprocessCommand would only fail inside a pod. Parse it here instead.
    TimeAxisParams iso = TimeAxisParams.of("ISO", "2026-01-01", "", "900", "", "P1M", "");
    String[] args = commandArgs(iso.toCliFlags());

    ParseResult parsed = new CommandLine(new PreprocessCommand()).parseArgs(args);

    assertEquals("ISO", parsed.matchedOptionValue("--time-type", ""));
    assertEquals("2026-01-01", parsed.matchedOptionValue("--time-start", ""));
    assertEquals("900", parsed.matchedOptionValue("--time-count", ""));
    assertEquals("P1M", parsed.matchedOptionValue("--time-interval", ""));
    assertFalse(parsed.hasMatchedOption("--time-unit"));
    assertFalse(parsed.hasMatchedOption("--time-instant"));
  }

  @Test
  void countCliFlagsParseAsPreprocessOptions() {
    String[] args = commandArgs(DECLARED.toCliFlags());

    ParseResult parsed = new CommandLine(new PreprocessCommand()).parseArgs(args);

    assertEquals("count", parsed.matchedOptionValue("--time-type", ""));
    assertEquals("year", parsed.matchedOptionValue("--time-unit", ""));
    assertEquals("1", parsed.matchedOptionValue("--time-increment", ""));
  }

  /** Prefixes the required preprocess positionals onto a flag string, as the entrypoint does. */
  private static String[] commandArgs(String flags) {
    String positional = "sim.josh Main data.nc temperature celsius out.jshd";
    return (positional + " " + flags).split(" ");
  }
}
