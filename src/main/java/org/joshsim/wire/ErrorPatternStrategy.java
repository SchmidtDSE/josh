/**
 * Strategy for parsing error response patterns from wire format.
 *
 * <p>This strategy handles wire format response lines that contain error messages
 * using the pattern [error] message where message is the error description.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strategy for parsing error response patterns from wire format.
 *
 * <p>This strategy parses response lines matching the pattern [error] message
 * where message contains the error description. It creates ParsedResponse objects
 * of type ERROR.</p>
 */
public class ErrorPatternStrategy implements WireResponseParserStrategy {

  private static final Pattern ERROR_PATTERN = Pattern.compile("^\\[error\\] (.+)$");

  @Override
  public WireParseResult tryParse(String line) {
    if (line == null || line.trim().isEmpty()) {
      return WireParseResult.noMatch();
    }

    String trimmed = line.trim();
    Matcher matcher = ERROR_PATTERN.matcher(trimmed);
    
    if (!matcher.matches()) {
      return WireParseResult.noMatch();
    }

    String errorMessage = matcher.group(1);
    ParsedResponse response = new ParsedResponse(errorMessage);
    return new WireParseResult(response);
  }
}