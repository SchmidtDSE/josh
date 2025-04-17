/**
 * Compatibility implementation of StringJoiner for regular JVM runtimes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.util.StringJoiner;


/**
 * UtilityStringJoiner that uses an JVM StringJoiner.
 */
public class JvmStringJoiner implements UtilityStringJoiner {

  private final StringJoiner jvmStringJoiner;

  /**
   * Constructs a new UtilityStringJoiner instance with the specified delimiter.
   *
   * @param delim the delimiter to be used between joined strings
   */
  public JvmStringJoiner(String delim) {
    jvmStringJoiner = new StringJoiner(delim);
  }

  @Override
  public void add(String piece) {
    jvmStringJoiner.add(piece);
  }

  @Override
  public String compile() {
    return jvmStringJoiner.toString();
  }
}
