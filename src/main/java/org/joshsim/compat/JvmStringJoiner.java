/**
 * StringJoiner for JVM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.util.StringJoiner;

/**
 * A JVM-compatible implementation of CompatibleStringJoiner.
 *
 * <p>This implementation provides string joining functionality for JVM environments using Java's
 * built-in StringJoiner class.</p>
 */
public class JvmStringJoiner implements CompatibleStringJoiner {

  private final StringJoiner inner;

  /**
   * Creates a new JvmStringJoiner with the specified delimiter.
   *
   * @param delimiter The delimiter to be used between joined strings
   */
  public JvmStringJoiner(String delimiter) {
    inner = new StringJoiner(delimiter);
  }

  @Override
  public CompatibleStringJoiner add(CharSequence newPiece) {
    inner.add(newPiece);
    return this;
  }

  @Override
  public String toString() {
    return inner.toString();
  }

}
