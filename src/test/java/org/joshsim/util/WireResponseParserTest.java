/**
 * Unit tests for WireResponseParser utility class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joshsim.util.WireResponseParser.ParsedResponse;
import org.joshsim.util.WireResponseParser.ResponseType;
import org.junit.jupiter.api.Test;


/**
 * Test cases for WireResponseParser functionality.
 */
public class WireResponseParserTest {

  @Test
  public void testParseEndResponse() {
    ParsedResponse result = WireResponseParser.parseEngineResponse("[end 5]");
    
    assertEquals(ResponseType.END, result.getType());
    assertEquals(5, result.getReplicateNumber());
    assertNull(result.getDataLine());
    assertEquals(0, result.getStepCount());
    assertNull(result.getErrorMessage());
  }

  @Test
  public void testParseProgressResponse() {
    ParsedResponse result = WireResponseParser.parseEngineResponse("[progress 42]");
    
    assertEquals(ResponseType.PROGRESS, result.getType());
    assertEquals(-1, result.getReplicateNumber());
    assertNull(result.getDataLine());
    assertEquals(42, result.getStepCount());
    assertNull(result.getErrorMessage());
  }

  @Test
  public void testParseErrorResponse() {
    ParsedResponse result = WireResponseParser.parseEngineResponse("[error] Something went wrong");
    
    assertEquals(ResponseType.ERROR, result.getType());
    assertEquals(-1, result.getReplicateNumber());
    assertNull(result.getDataLine());
    assertEquals(0, result.getStepCount());
    assertEquals("Something went wrong", result.getErrorMessage());
  }

  @Test
  public void testParseDatumResponse() {
    ParsedResponse result = WireResponseParser.parseEngineResponse(
        "[2] target:key1=value1\tkey2=value2");
    
    assertEquals(ResponseType.DATUM, result.getType());
    assertEquals(2, result.getReplicateNumber());
    assertEquals("target:key1=value1\tkey2=value2", result.getDataLine());
    assertEquals(0, result.getStepCount());
    assertNull(result.getErrorMessage());
  }

  @Test
  public void testParseEmptyReplicateResponse() {
    ParsedResponse result = WireResponseParser.parseEngineResponse("[3]");
    
    assertNull(result); // Empty replicate responses should be ignored
  }

  @Test
  public void testParseEmptyDataResponse() {
    ParsedResponse result = WireResponseParser.parseEngineResponse("[1] ");
    
    assertNull(result); // Empty data should be ignored
  }

  @Test
  public void testParseNullInput() {
    ParsedResponse result = WireResponseParser.parseEngineResponse(null);
    assertNull(result);
  }

  @Test
  public void testParseEmptyInput() {
    ParsedResponse result = WireResponseParser.parseEngineResponse("");
    assertNull(result);
  }

  @Test
  public void testParseWhitespaceInput() {
    ParsedResponse result = WireResponseParser.parseEngineResponse("   ");
    assertNull(result);
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
    ParsedResponse result = WireResponseParser.parseEngineResponse("[end 0]");
    
    assertEquals(ResponseType.END, result.getType());
    assertEquals(0, result.getReplicateNumber());
  }

  @Test
  public void testParseProgressWithZeroStep() {
    ParsedResponse result = WireResponseParser.parseEngineResponse("[progress 0]");
    
    assertEquals(ResponseType.PROGRESS, result.getType());
    assertEquals(0, result.getStepCount());
  }

  @Test
  public void testParseDatumWithComplexData() {
    String complexData = "patch:variable1=123.45\tvariable2=test value"
        + "\tvariable3=true";
    ParsedResponse result = WireResponseParser.parseEngineResponse("[1] " + complexData);
    
    assertEquals(ResponseType.DATUM, result.getType());
    assertEquals(1, result.getReplicateNumber());
    assertEquals(complexData, result.getDataLine());
  }

  @Test
  public void testParseErrorWithLongMessage() {
    String longMessage = "This is a very long error message that contains" 
        + " multiple words and spaces";
    ParsedResponse result = WireResponseParser.parseEngineResponse("[error] " + longMessage);
    
    assertEquals(ResponseType.ERROR, result.getType());
    assertEquals(longMessage, result.getErrorMessage());
  }

  @Test
  public void testParseResponseWithLeadingWhitespace() {
    ParsedResponse result = WireResponseParser.parseEngineResponse("  [progress 10]  ");
    
    assertEquals(ResponseType.PROGRESS, result.getType());
    assertEquals(10, result.getStepCount());
  }

  @Test
  public void testToStringMethod() {
    ParsedResponse endResponse = WireResponseParser.parseEngineResponse("[end 5]");
    String toString = endResponse.toString();
    
    // Verify toString contains key information
    assertEquals(true, toString.contains("END"));
    assertEquals(true, toString.contains("5"));
  }
}