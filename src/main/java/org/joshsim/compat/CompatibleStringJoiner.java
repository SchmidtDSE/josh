package org.joshsim.compat;


/**
 *
 */
public interface CompatibleStringJoiner {

  /**
   * Add a new string to the collection of strings to be joined.
   *
   * @param piece The new string to be added to the collection of strings where each has this
   *     joiner's delimiter in-between.
   */
  CompatibleStringJoiner add(CharSequence newPiece);

}
