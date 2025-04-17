/**
 * Utility layer which support regular browser-based runtimes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;


/**
 * Utility layer which support regular browser-based runtimes (JS and WASM).
 */
public class JsUtilityLayer implements UtilityLayer {

  @Override
  public UtilityStringJoiner buildStringJoiner(String delim) {
    return new JsStringJoiner(delim);
  }

}
