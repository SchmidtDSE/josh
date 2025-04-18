/**
 * Compatibility implementation of StringJoiner for wasm / JS runtimes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.core.Rename;

@Rename("java.util.StringJoiner")


/**
 * A compatibility implementation of the UtilityStringJoiner interface for WASM / JS runtimes.
 */
public class JsStringJoiner {

  private final String delim;
  private boolean first;
  private String value;

  /**
   * Constructs a new UtilityStringJoiner instance with the specified delimiter.
   *
   * @param delim the delimiter to be used between joined strings
   */
  public JsStringJoiner(String delim) {
    this.delim = delim;
    value = "";
    first = true;
  }

  /**
   * Add a new string to the collection of strings to be joined.
   *
   * @param piece The new string to be added to the collection of strings where each has this
   *     joiner's delimeter in-between.
   */
  public void add(String piece) {
    if (first) {
      value = piece;
      first = false;
    } else {
      value = value + delim + piece;
    }
  }

  @Override
  public String toString() {
    return value;
  }

}