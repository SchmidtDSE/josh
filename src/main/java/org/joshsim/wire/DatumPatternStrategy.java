/**
 * Strategy for parsing datum response patterns from wire format.
 *
 * <p>This strategy handles wire format response lines that contain data points
 * using the pattern [N] data where N is the replicate number and data is the payload.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strategy for parsing datum response patterns from wire format.
 *
 * <p>This strategy parses response lines matching the pattern [N] data where N is
 * the replicate number and data is the wire format data payload. It creates
 * ParsedResponse objects of type DATUM. Lines with empty data are ignored.</p>
 */
public class DatumPatternStrategy implements WireResponseParserStrategy {

  private static final Pattern DATUM_PATTERN = Pattern.compile("^\\[(\\d+)\\] (.*)$");

  @Override
  public WireParseResult tryParse(String line) {
    if (line == null || line.trim().isEmpty()) {
      return WireParseResult.noMatch();
    }

    String trimmed = line.trim();
    Matcher matcher = DATUM_PATTERN.matcher(trimmed);

    if (!matcher.matches()) {
      return WireParseResult.noMatch();
    }

    try {
      int replicateNumber = Integer.parseInt(matcher.group(1));
      String dataLine = matcher.group(2);

      if (dataLine == null || dataLine.trim().isEmpty()) {
        // Ignore empty data - return IGNORED result
        return new WireParseResult();
      }

      ParsedResponse response = new ParsedResponse(replicateNumber, dataLine);
      return new WireParseResult(response);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid replicate number in DATUM response: " + line, e);
    }
  }
}
