/**
 * Utility for working with Josh string literal tokens.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

/**
 * Helpers for the {@code STR_} lexer token whose text includes its surrounding double quotes.
 */
public final class StringLiteralUtil {

  private StringLiteralUtil() {}

  /**
   * Strip the surrounding double quotes from a raw {@code STR_} token's text.
   *
   * <p>The {@code STR_} token ({@code '"' ~["]* '"'}) carries its quotes in {@code getText()}. This
   * removes them so a stored literal matches the (unquoted) value an entity attribute holds. Input
   * without both surrounding quotes is returned unchanged.</p>
   *
   * @param raw the raw token text, possibly quoted
   * @return the text with a single pair of surrounding double quotes removed, if present
   */
  public static String stripQuotes(String raw) {
    boolean quoted = raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"");
    return quoted ? raw.substring(1, raw.length() - 1) : raw;
  }
}
