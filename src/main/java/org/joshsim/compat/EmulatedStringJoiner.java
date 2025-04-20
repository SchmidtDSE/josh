/**
 * Compatibility implementation of StringJoiner for wasm / JS runtimes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;


/**
 * A compatibility implementation of the UtilityStringJoiner interface for WASM / JS runtimes.
 */
public class EmulatedStringJoiner implements CompatibleStringJoiner {

  private final String delim;
  private boolean first;
  private String value;

  /**
   * Constructs a new UtilityStringJoiner instance with the specified delimiter.
   *
   * @param delim the delimiter to be used between joined strings
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