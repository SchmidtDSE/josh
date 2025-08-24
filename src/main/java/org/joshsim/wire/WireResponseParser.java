/**
 * Utility for parsing streaming responses from Josh remote servers.
 *
 * <p>This class provides functionality to parse wire format responses from remote Josh
 * servers, converting streaming response lines into structured data objects. It mirrors
 * the functionality of parseEngineResponse from parse.js in the JavaScript codebase.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Parser for streaming response lines from remote Josh execution engines.
 *
 * <p>This utility class parses individual lines from streaming HTTP responses sent by
 * remote JoshSimServer instances. It supports various response types including data
 * points, progress updates, completion notifications, and error messages.</p>
 *
 * <p>The parser uses a strategy pattern with multiple parsing strategies that are
 * applied sequentially until one successfully parses the line or all strategies
 * have been attempted.</p>
 */
public class WireResponseParser {

  private static final List<WireResponseParserStrategy> STRATEGIES = Arrays.asList(
      new EndPatternStrategy(),
      new EmptyPatternStrategy(),
      new ErrorPatternStrategy(),
      new ProgressPatternStrategy(),
      new DatumPatternStrategy()
  );

  /**
   * Parses a single response line from the remote engine.
   *
   * <p>This method analyzes a line from the streaming HTTP response and determines
   * its type and content. It uses a series of parsing strategies to handle different
   * response formats:</p>
   * <ul>
   *   <li>[end N] - Replicate N has completed</li>
   *   <li>[progress N] - Current step is N</li>
   *   <li>[error] message - An error occurred with the given message</li>
   *   <li>[N] data - Data point from replicate N</li>
   *   <li>[N] - Empty data point from replicate N (ignored)</li>
   * </ul>
   *
   * @param line The response line to parse
   * @return Optional containing WireResponse if parsing was successful, empty if ignored
   * @throws IllegalArgumentException if the line format is invalid
   */
  public static Optional<WireResponse> parseEngineResponse(String line) {
    if (line == null || line.trim().isEmpty()) {
      return Optional.empty();
    }

    for (WireResponseParserStrategy strategy : STRATEGIES) {
      WireParseResult result = strategy.tryParse(line);

      switch (result.getOutcome()) {
        case PARSED -> {
          return Optional.of(result.getParsedResponse());
        }
        case IGNORED -> {
          return Optional.empty();
        }
        case NO_MATCH -> {
          // Continue to next strategy
        }
        default -> {
          // Should never happen with current enum values
        }
      }
    }

    // If no strategy successfully parsed the line, it's an error
    throw new IllegalArgumentException("Invalid engine response format: " + line);
  }

}
