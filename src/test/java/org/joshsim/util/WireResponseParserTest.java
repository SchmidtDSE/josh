/**
 * Unit tests for WireResponseParser utility class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.joshsim.wire.ParsedResponse;
import org.joshsim.wire.ParsedResponse.ResponseType;
import org.joshsim.wire.WireResponseParser;
import org.junit.jupiter.api.Test;


/**
 * Test cases for WireResponseParser functionality.
 */
public class WireResponseParserTest {

  @Test
  public void testParseEndResponse() {
    Optional<ParsedResponse> optionalResult = WireResponseParser.parseEngineResponse("[end 5]");

    assertTrue(optionalResult.isPresent());
    ParsedResponse result = optionalResult.get();
    assertEquals(ResponseType.END, result.getType());
    assertEquals(5, result.getReplicateNumber());
    assertNull(result.getDataLine());
    assertEquals(0, result.getStepCount());
    assertNull(result.getErrorMessage());
  }

  @Test
  public void testParseProgressResponse() {
    Optional<ParsedResponse> optionalResult =
        WireResponseParser.parseEngineResponse("[progress 42]");

    assertTrue(optionalResult.isPresent());
    ParsedResponse result = optionalResult.get();
    assertEquals(ResponseType.PROGRESS, result.getType());
    assertEquals(-1, result.getReplicateNumber());
    assertNull(result.getDataLine());
    assertEquals(42, result.getStepCount());
    assertNull(result.getErrorMessage());
  }

  @Test
  public void testParseErrorResponse() {
    Optional<ParsedResponse> optionalResult = WireResponseParser.parseEngineResponse(
        "[error] Something went wrong");

    assertTrue(optionalResult.isPresent());
    ParsedResponse result = optionalResult.get();
    assertEquals(ResponseType.ERROR, result.getType());
    assertEquals(-1, result.getReplicateNumber());
    assertNull(result.getDataLine());
    assertEquals(0, result.getStepCount());
    assertEquals("Something went wrong", result.getErrorMessage());
  }

  @Test
  public void testParseDatumResponse() {
    Optional<ParsedResponse> optionalResult = WireResponseParser.parseEngineResponse(
        "[2] target:key1=value1\tkey2=value2");

    assertTrue(optionalResult.isPresent());
    ParsedResponse result = optionalResult.get();
    assertEquals(ResponseType.DATUM, result.getType());
    assertEquals(2, result.getReplicateNumber());
    assertEquals("target:key1=value1\tkey2=value2", result.getDataLine());
    assertEquals(0, result.getStepCount());
    assertNull(result.getErrorMessage());
  }

  @Test
  public void testParseEmptyReplicateResponse() {
    Optional<ParsedResponse> result = WireResponseParser.parseEngineResponse("[3]");

    assertFalse(result.isPresent()); // Empty replicate responses should be ignored
  }

  @Test
  public void testParseEmptyDataResponse() {
    Optional<ParsedResponse> result = WireResponseParser.parseEngineResponse("[1] ");

    assertFalse(result.isPresent()); // Empty data should be ignored
  }

  @Test
  public void testParseNullInput() {
    Optional<ParsedResponse> result = WireResponseParser.parseEngineResponse(null);
    assertFalse(result.isPresent());
  }

  @Test
  public void testParseEmptyInput() {
    Optional<ParsedResponse> result = WireResponseParser.parseEngineResponse("");
    assertFalse(result.isPresent());
  }

  @Test
  public void testParseWhitespaceInput() {
    Optional<ParsedResponse> result = WireResponseParser.parseEngineResponse("   ");
    assertFalse(result.isPresent());
  }

  @Test
  public void testParseInvalidFormat() {
    assertThrows(IllegalArgumentException.class, () -> {
      WireResponseParser.parseEngineResponse("invalid format");
    });
  }

  @Test
  public void testParseInvalidBracketFormat() {
    assertThrows(IllegalArgumentException.class, () -> {
      WireResponseParser.parseEngineResponse("[invalid] format");
    });
  }

  @Test
  public void testParseEndWithZeroReplicate() {
    Optional<ParsedResponse> optionalResult = WireResponseParser.parseEngineResponse("[end 0]");

    assertTrue(optionalResult.isPresent());
    ParsedResponse result = optionalResult.get();
    assertEquals(ResponseType.END, result.getType());
    assertEquals(0, result.getReplicateNumber());
  }

  @Test
  public void testParseProgressWithZeroStep() {
    Optional<ParsedResponse> optionalResult =
        WireResponseParser.parseEngineResponse("[progress 0]");

    assertTrue(optionalResult.isPresent());
    ParsedResponse result = optionalResult.get();
    assertEquals(ResponseType.PROGRESS, result.getType());
    assertEquals(0, result.getStepCount());
  }

  @Test
  public void testParseDatumWithComplexData() {
    String complexData = "patch:variable1=123.45\tvariable2=test value"
        + "\tvariable3=true";
    Optional<ParsedResponse> optionalResult = WireResponseParser.parseEngineResponse(
        "[1] " + complexData);

    assertTrue(optionalResult.isPresent());
    ParsedResponse result = optionalResult.get();
    assertEquals(ResponseType.DATUM, result.getType());
    assertEquals(1, result.getReplicateNumber());
    assertEquals(complexData, result.getDataLine());
  }

  @Test
  public void testParseErrorWithLongMessage() {
    String longMessage = "This is a very long error message that contains"
        + " multiple words and spaces";
    Optional<ParsedResponse> optionalResult = WireResponseParser.parseEngineResponse(
        "[error] " + longMessage);

    assertTrue(optionalResult.isPresent());
    ParsedResponse result = optionalResult.get();
    assertEquals(ResponseType.ERROR, result.getType());
    assertEquals(longMessage, result.getErrorMessage());
  }

  @Test
  public void testParseResponseWithLeadingWhitespace() {
    Optional<ParsedResponse> optionalResult = WireResponseParser.parseEngineResponse(
        "  [progress 10]  ");

    assertTrue(optionalResult.isPresent());
    ParsedResponse result = optionalResult.get();
    assertEquals(ResponseType.PROGRESS, result.getType());
    assertEquals(10, result.getStepCount());
  }

  @Test
  public void testToStringMethod() {
    Optional<ParsedResponse> optionalResult = WireResponseParser.parseEngineResponse("[end 5]");

    assertTrue(optionalResult.isPresent());
    ParsedResponse endResponse = optionalResult.get();
    String toString = endResponse.toString();

    // Verify toString contains key information and is formatted properly
    assertTrue(toString.contains("END"));
    assertTrue(toString.contains("5"));
    // Check that the new formatting is applied (parameters on separate conceptual lines)
    assertTrue(toString.contains("type="));
    assertTrue(toString.contains("replicate="));
  }

  @Test
  public void testParseEngineResponseWithEnd() {
    // Test parsing end response
    Optional<ParsedResponse> optionalResult = WireResponseParser.parseEngineResponse("[end 5]");

    assertTrue(optionalResult.isPresent());
    ParsedResponse result = optionalResult.get();
    assertEquals(ResponseType.END, result.getType());
    assertEquals(5, result.getReplicateNumber());
  }

  @Test
  public void testParseEngineResponseWithIgnoredLine() {
    // Test that parseEngineResponse returns empty Optional for ignored lines
    Optional<ParsedResponse> result = WireResponseParser.parseEngineResponse("[3]");

    assertFalse(result.isPresent());
  }
}
