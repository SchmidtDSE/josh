/**
 * Enumeration of possible outcomes from wire response parsing strategies.
 *
 * <p>This enum distinguishes between different types of parsing outcomes to allow
 * proper handling of lines that should be ignored versus lines that don't match.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

/**
 * Enumeration of possible outcomes from wire response parsing strategies.
 */
public enum WireParseOutcome {
  /** The line doesn't match this strategy's pattern - try other strategies. */
  NO_MATCH,
  /** The line matches this strategy's pattern and should be ignored. */
  IGNORED,
  /** The line matches this strategy's pattern and was successfully parsed. */
  PARSED
}