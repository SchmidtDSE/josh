/**
 * StringJoiner for WASM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;


/**
 * A WebAssembly-compatible implementation of CompatibleStringJoiner.
 *
 * <p>This implementation provides string joining functionality for WebAssembly environments where
 * Java's StringJoiner is not available.</p>
 */
public class EmulatedStringJoiner implements CompatibleStringJoiner {

  private final String delim;
  private boolean first;
  private String value;

  /**
   * Constructs a new EmulatedStringJoiner with the specified delimiter.
   *
   * @param delim The delimiter to be used between joined strings
   */
  public EmulatedStringJoiner(CharSequence delim) {
    this.delim = delim.toString();
    value = "";
    first = true;
  }

  @Override
  public EmulatedStringJoiner add(CharSequence piece) {
    String pieceStr = piece.toString();
    if (first) {
      value = pieceStr;
      first = false;
    } else {
      value = value + delim + pieceStr;
    }

    return this;
  }

  @Override
  public String toString() {
    return value;
  }

}
