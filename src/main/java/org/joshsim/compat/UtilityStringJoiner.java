package org.joshsim.compat;


/**
 * Utility to join multiple strings together.
 *
 * <p>Utility to join multiple strings together, taking advantage of platform-specific speed boosts
 * where possible.</p>
 */
public interface UtilityStringJoiner {

  /**
   * Add a new string to the collection of strings to be joined.
   *
   * @param piece The new string to be added to the collection of strings where each has this
   *     joiner's delimeter in-between.
   */
  void add(String piece);

  /**
   * Get the complete joined string.
   *
   * @return String which has all of the pieces given in it but with this joiner's delimiter in
   *     between each.
   */
  String compile();

}
