/**
 * Compatibility implementation of StringJoiner for wasm / JS runtimes.
 *
 * @license BSD-3-Clause
 */

package org.teavm.classlib.java.util;


import java.util.StringJoiner;

/**
 * A compatibility implementation of the UtilityStringJoiner interface for WASM / JS runtimes.
 */
public class TStringJoiner {

  private final String delim;
  private boolean first;
  private String value;

  /**
   * Constructs a new UtilityStringJoiner instance with the specified delimiter.
   *
   * @param delim the delimiter to be used between joined strings
   */
  public TStringJoiner(CharSequence delim) {
    this.delim = delim.toString();
    value = "";
    first = true;
  }

  /**
   * Add a new string to the collection of strings to be joined.
   *
   * @param piece The new string to be added to the collection of strings where each has this
   *     joiner's delimiter in-between.
   */
  public TStringJoiner add(CharSequence piece) {
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