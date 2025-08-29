/**
 * Strategy for parsing empty response patterns from wire format.
 *
 * <p>This strategy handles wire format response lines that contain only replicate numbers
 * without data using the pattern [N]. These lines are typically ignored as they represent
 * empty data points.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strategy for parsing empty response patterns from wire format.
 *
 * <p>This strategy parses response lines matching the pattern [N] where N is
 * a replicate number but no data follows. These responses are intended to be
 * ignored, so this strategy returns empty Optional to signal they should be skipped.</p>
 */
public class EmptyPatternStrategy implements WireResponseParserStrategy {

  private static final Pattern EMPTY_PATTERN = Pattern.compile("^\\[(\\d+)\\]$");

  @Override
  public WireParseResult tryParse(String line) {
    if (line == null || line.trim().isEmpty()) {
      return WireParseResult.noMatch();
    }

    String trimmed = line.trim();
    Matcher matcher = EMPTY_PATTERN.matcher(trimmed);

    if (matcher.matches()) {
      // Empty pattern should be ignored - return IGNORED result
      return new WireParseResult();
    }

    return WireParseResult.noMatch();
  }
}
