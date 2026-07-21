package org.joshsim.util;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for parsing output steps parameters across different execution modes.
 *
 * <p>This utility provides consistent parsing logic for comma-separated output step
 * specifications used throughout the Josh simulation platform. It handles validation,
 * error reporting, and conversion to appropriate data structures.</p>
 */
public final class OutputStepsParser {

  private OutputStepsParser() {
    // Prevent instantiation of utility class
  }

  /**
   * Parses comma-separated output steps string into a Set of integers.
   *
   * <p>This method handles various input formats gracefully:</p>
   * <ul>
   *   <li>Empty/null/whitespace-only strings return Optional.empty() (export all steps)</li>
   *   <li>Comma-separated integers like "5,7,8,9,20" return Set of those integers</li>
   *   <li>Each comma-separated token may instead be an inclusive range like "5-9", expanding to
   *       every integer in {@code [5, 9]} — useful for large step counts (e.g. "0-100")</li>
   *   <li>Filters empty strings to handle cases like "1,,3" gracefully</li>
   *   <li>Trims whitespace around each token</li>
   * </ul>
   *
   * @param outputSteps Comma-separated string of step numbers and/or inclusive ranges (e.g.,
   *     "5,7-9,20")
   * @return Optional containing the set of steps to export, or empty if all steps
   *     should be exported
   * @throws IllegalArgumentException if the output steps format is invalid
   */
  public static Optional<Set<Integer>> parseForCli(String outputSteps) {
    try {
      return parseTokens(outputSteps);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid output-steps format: " + outputSteps
          + ". Expected comma-separated integers or inclusive ranges (e.g., '5,7-9,20')");
    }
  }

  /**
   * Parses comma-separated output steps string into a Set of integers for WebAssembly/Remote use.
   *
   * <p>This method provides the same parsing logic as {@link #parseForCli(String)} but
   * throws RuntimeException instead of IllegalArgumentException for consistency with
   * WebAssembly and remote execution error handling patterns.</p>
   *
   * @param outputSteps Comma-separated string of step numbers and/or inclusive ranges (e.g.,
   *     "5,7-9,20")
   * @return Optional containing the set of steps to export, or empty if all steps
   *     should be exported
   * @throws RuntimeException if the output steps format is invalid
   */
  public static Optional<Set<Integer>> parseForWasmOrRemote(String outputSteps) {
    try {
      return parseTokens(outputSteps);
    } catch (NumberFormatException e) {
      throw new RuntimeException("Invalid output steps format: " + outputSteps
          + ". Expected comma-separated integers or inclusive ranges (e.g., '5,7-9,20')");
    }
  }

  /**
   * Parse the tokens common to both the CLI and WASM/remote formats.
   *
   * @param outputSteps Comma-separated string of step numbers and/or inclusive ranges.
   * @return Optional containing the set of steps to export, or empty if all steps should be
   *     exported.
   * @throws NumberFormatException if any token is not a valid integer or {@code low-high} range.
   */
  private static Optional<Set<Integer>> parseTokens(String outputSteps) {
    if (outputSteps == null || outputSteps.trim().isEmpty()) {
      return Optional.empty();
    }

    Set<Integer> steps = new HashSet<>();
    for (String token : outputSteps.split(",")) {
      String trimmed = token.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (trimmed.contains("-")) {
        String[] bounds = trimmed.split("-", 2);
        int low = Integer.parseInt(bounds[0].trim());
        int high = Integer.parseInt(bounds[1].trim());
        if (low > high) {
          throw new NumberFormatException("Range low bound exceeds high bound: " + trimmed);
        }
        for (int step = low; step <= high; step++) {
          steps.add(step);
        }
      } else {
        steps.add(Integer.parseInt(trimmed));
      }
    }

    return steps.isEmpty() ? Optional.empty() : Optional.of(steps);
  }
}
