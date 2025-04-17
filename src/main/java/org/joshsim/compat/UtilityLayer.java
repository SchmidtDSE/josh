package org.joshsim.compat;


/**
 * Layer which accesses support utilities which are platform specific.
 *
 * <p>Layer which accesses support utilities which are platform specific, specifically which may
 * differ between browser-based (JS / WASM) execution and regular (JVM) execution.</p>
 */
public interface UtilityLayer {

  /**
   * Build a utility which allows for joining multiple strings together with a delimeter.
   *
   * @param delim The delimiter to place between strings.
   * @return The utility to join strings efficiently.
   */
  UtilityStringJoiner buildStringJoiner(String delim);

}
