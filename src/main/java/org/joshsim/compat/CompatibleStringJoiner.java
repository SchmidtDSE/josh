/**
 * Interface for a cross-VM compatability string joiner.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

/**
 * Platform-compatible string joiner.
 *
 * <p>This interface provides an abstraction for string joining functionality that can be
 * implemented across different runtime environments, such as the JVM and WebAssembly. It supports
 * adding strings sequentially and joining them with a specified delimiter.</p>
 */
public interface CompatibleStringJoiner {

  /**
   * Add a new string to the collection of strings to be joined.
   *
   * @param piece The new string to be added to the collection of strings where each has this
   *     joiner's delimiter in-between.
   */
  CompatibleStringJoiner add(CharSequence piece);

}
