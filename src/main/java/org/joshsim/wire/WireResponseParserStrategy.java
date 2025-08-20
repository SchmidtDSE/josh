/**
 * Strategy interface for parsing wire format response lines.
 *
 * <p>This interface defines the contract for parsing strategies that can attempt to parse
 * specific types of wire format response lines. Each strategy focuses on a particular
 * response pattern and returns an Optional result indicating success or failure.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

/**
 * Strategy interface for parsing wire format response lines.
 *
 * <p>Implementations of this interface provide parsing logic for specific types of
 * wire format response lines from remote Josh servers. Each strategy attempts to parse
 * a line and returns an Optional result - empty if the line doesn't match the strategy's
 * pattern, or containing a WireParseResult if parsing was successful.</p>
 */
public interface WireResponseParserStrategy {

  /**
   * Attempts to parse a wire format response line.
   *
   * <p>This method analyzes the provided line and attempts to parse it according to
   * the strategy's specific pattern. It returns a WireParseResult indicating whether
   * the line was successfully parsed, should be ignored, or doesn't match this pattern.</p>
   *
   * @param line The wire format response line to parse
   * @return WireParseResult indicating the outcome of the parsing attempt
   * @throws IllegalArgumentException if the line format matches the pattern but is invalid
   */
  WireParseResult tryParse(String line);
}
