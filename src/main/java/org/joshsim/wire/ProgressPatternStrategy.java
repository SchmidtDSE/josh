/**
 * Strategy for parsing progress response patterns from wire format.
 *
 * <p>This strategy handles wire format response lines that contain progress updates
 * using the pattern [progress N] where N is the current step count.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strategy for parsing progress response patterns from wire format.
 *
 * <p>This strategy parses response lines matching the pattern [progress N]
 * where N is the current step count. It creates ParsedResponse objects of type PROGRESS.</p>
 */
public class ProgressPatternStrategy implements WireResponseParserStrategy {

  private static final Pattern PROGRESS_PATTERN = Pattern.compile("^\\[progress (\\d+)\\]$");

  @Override
  public WireParseResult tryParse(String line) {
    if (line == null || line.trim().isEmpty()) {
      return WireParseResult.noMatch();
    }

    String trimmed = line.trim();
    Matcher matcher = PROGRESS_PATTERN.matcher(trimmed);
    
    if (!matcher.matches()) {
      return WireParseResult.noMatch();
    }

    try {
      long stepCount = Long.parseLong(matcher.group(1));
      ParsedResponse response = new ParsedResponse(stepCount);
      return new WireParseResult(response);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid step count in PROGRESS response: " + line, e);
    }
  }
}