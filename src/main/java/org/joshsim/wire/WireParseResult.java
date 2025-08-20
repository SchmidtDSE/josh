/**
 * Result wrapper for wire response parsing strategies.
 *
 * <p>This class encapsulates the result of attempting to parse a wire format response line
 * using a specific parsing strategy. It contains the parsed response data when successful
 * and indicates the outcome of the parsing attempt.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

/**
 * Result wrapper for wire response parsing strategies.
 *
 * <p>This class contains the result of a parse attempt by a wire response parsing strategy.
 * It indicates whether the line was parsed, ignored, or didn't match the strategy's pattern.</p>
 */
public class WireParseResult {
  private final WireParseOutcome outcome;
  private final ParsedResponse parsedResponse;

  /**
   * Constructor for successful parse results.
   *
   * @param parsedResponse The successfully parsed response data
   */
  public WireParseResult(ParsedResponse parsedResponse) {
    if (parsedResponse == null) {
      throw new IllegalArgumentException("ParsedResponse cannot be null");
    }
    this.outcome = WireParseOutcome.PARSED;
    this.parsedResponse = parsedResponse;
  }

  /**
   * Constructor for ignored results.
   */
  public WireParseResult() {
    this.outcome = WireParseOutcome.IGNORED;
    this.parsedResponse = null;
  }

  private WireParseResult(WireParseOutcome outcome) {
    this.outcome = outcome;
    this.parsedResponse = null;
  }

  /**
   * Creates a result indicating no match with this strategy.
   *
   * @return A WireParseResult indicating no match
   */
  public static WireParseResult noMatch() {
    return new WireParseResult(WireParseOutcome.NO_MATCH);
  }

  /**
   * Gets the parsing outcome.
   *
   * @return The outcome of the parsing attempt
   */
  public WireParseOutcome getOutcome() {
    return outcome;
  }

  /**
   * Gets the parsed response data (only valid for PARSED outcomes).
   *
   * @return The parsed response data, or null if not parsed
   */
  public ParsedResponse getParsedResponse() {
    return parsedResponse;
  }

  @Override
  public String toString() {
    return String.format("WireParseResult{outcome=%s, parsedResponse=%s}", outcome, parsedResponse);
  }
}
