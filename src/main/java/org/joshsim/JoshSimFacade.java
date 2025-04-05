/**
 * Facade for interacting with the Josh platform via code.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import org.joshsim.lang.parse.JoshParser;
import org.joshsim.lang.parse.ParseResult;


/**
 * Entry point into the Josh platform when used as a library.
 *
 * <p>Facade which helps facilitate common operations within the Josh simulation platform when used
 * as a library as opposed to as an interactive / command-line tool.</p>
 */
public class JoshSimFacade {

  /**
   * Parse a Josh script.
   *
   * <p>Parse a Josh script such as to to check for syntax errors or generate an AST in support of
   * developer tools.</p>
   *
   * @param code String code to parse as a Josh source.
   * @returns the result of parsing the code where hasErrors and getErrors can report on syntax
   *     issues found.
   */
  public static ParseResult parse(String code) {
    JoshParser parser = new JoshParser();
    return parser.parse(code);
  }

}
