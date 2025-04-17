/**
 * Structures to check for accidental shadowing of reserved word.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Set;
import java.util.StringTokenizer;


/**
 * Utility to check variable names to see if they shadow reserved words.
 */
public class ReservedWordChecker {

  private static final Set<String> ERROR_WORDS = Set.of("prior", "current", "here", "meta");
  
  /**
   * Checks a variable declaration to ensure it does not shadow any reserved words.
   *
   * @param name The name of the variable to check.
   * @throws IllegalArgumentException if the variable name shadows a reserved word.
   */
  public static void checkVariableDeclaration(String name) {
    StringTokenizer tokenizer = new StringTokenizer(name, ".");
    String front = tokenizer.asIterator().next().toString();
    if (ERROR_WORDS.contains(front)) {
      String message = String.format("Cannot shadow %s.", name);
      throw new IllegalArgumentException(message);
    }
  }
  
}