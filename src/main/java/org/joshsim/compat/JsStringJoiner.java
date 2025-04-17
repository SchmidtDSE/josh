/**
 * Compatibility implementation of StringJoiner for wasm / JS runtimes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;


/**
 * A compatibility implementation of the UtilityStringJoiner interface for WASM / JS runtimes.
 */
public class JsStringJoiner implements UtilityStringJoiner {

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

  @Override
  public void add(String piece) {
    if (first) {
      value = piece;
      first = false;
    } else {
      value = value + delim + piece;
    }
  }

  @Override
  public String compile() {
    return value;
  }

}
