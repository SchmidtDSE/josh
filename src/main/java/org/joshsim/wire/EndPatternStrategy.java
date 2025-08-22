/**
 * Strategy for parsing END response patterns from wire format.
 *
 * <p>This strategy handles wire format response lines that indicate replicate completion
 * using the pattern [end N] where N is the replicate number.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strategy for parsing END response patterns from wire format.
 *
 * <p>This strategy parses response lines matching the pattern [end N] where N is
 * the replicate number that completed. It creates WireResponse objects of type END.</p>
 */
public class EndPatternStrategy implements WireResponseParserStrategy {

  private static final Pattern END_PATTERN = Pattern.compile("^\\[end (\\d+)\\]$");

  @Override
  public WireParseResult tryParse(String line) {
    if (line == null || line.trim().isEmpty()) {
      return WireParseResult.noMatch();
    }

    String trimmed = line.trim();
    Matcher matcher = END_PATTERN.matcher(trimmed);

    if (!matcher.matches()) {
      return WireParseResult.noMatch();
    }

    try {
      int replicateNumber = Integer.parseInt(matcher.group(1));
      WireResponse response =
          new WireResponse(WireResponse.ResponseType.END, replicateNumber);
      return new WireParseResult(response);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid replicate number in END response: " + line, e);
    }
  }
}
