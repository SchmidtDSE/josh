package org.joshsim.util;

import picocli.CommandLine.Option;

/**
 * Output options for commands that need to print messages.
 */
public class OutputOptions {
  @Option(names = "--suppress-info", description = "Suppress standard output messages")
  boolean suppressInfo;

  @Option(names = "--suppress-errors", description = "Suppress error messages")
  boolean suppressErrors;

  /**
   * Prints an informational message to the standard output if not suppressed.
   *
   * @param message the informational message to print
   */
  public void printInfo(String message) {
    if (!suppressInfo) {
      System.out.println(message);
    }
  }

  /**
   * Prints an error to the standard output if not suppressed.
   *
   * @param message the informational message to print
   */
  public void printError(String message) {
    if (!suppressErrors) {
      System.err.println(message);
    }
  }
}
