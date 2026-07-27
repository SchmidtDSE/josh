/**
 * Output format selection for commands that can emit JSON or plain text.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import picocli.CommandLine.Option;

/**
 * Output format selection for commands that can emit JSON or plain text.
 *
 * <p>JSON is the default. Plain text is selected by its own flag rather than by negating
 * {@code --json}, because picocli sets a matched boolean flag to the <em>opposite</em> of its
 * default value: an option declared as {@code --json} with {@code defaultValue = "true"} turns JSON
 * off when it is passed, which is the opposite of what its name says. Marking such an option
 * {@code negatable} does not help, as the negated form inverts in the same way.</p>
 *
 * <p>Both flags here default to false, where picocli's behavior is unambiguous: a matched flag
 * becomes true. Passing both flags is a usage error rather than a silent preference for one.</p>
 */
public class JsonOutputOptions {

  @Option(
      names = "--json",
      description = "Output in JSON format. This is the default and the flag is accepted for "
          + "explicitness."
  )
  private boolean jsonRequested;

  @Option(
      names = {"--no-json", "--plain"},
      description = "Output plain text instead of JSON."
  )
  private boolean plainRequested;

  /**
   * Determines whether the caller asked for both formats at once.
   *
   * @return true if both {@code --json} and {@code --no-json} were given, which is a usage error.
   */
  public boolean hasConflict() {
    return jsonRequested && plainRequested;
  }

  /**
   * Determines whether output should be JSON.
   *
   * @return true for JSON, which is the default, or false when plain text was requested.
   */
  public boolean isJson() {
    return !plainRequested;
  }

  /**
   * Describes the conflicting-flag condition for error reporting.
   *
   * @return a message naming the mutually exclusive flags.
   */
  public String getConflictMessage() {
    return "--json and --no-json are mutually exclusive";
  }
}
