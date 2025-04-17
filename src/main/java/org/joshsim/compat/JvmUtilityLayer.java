/**
 * Utility layer which support regular JVM runtimes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;


/**
 * Utility layer which support regular JVM runtimes.
 */
public class JvmUtilityLayer implements UtilityLayer {

  @Override
  public UtilityStringJoiner buildStringJoiner(String delim) {
    return new JvmStringJoiner(delim);
  }

}
