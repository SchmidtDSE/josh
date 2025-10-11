package org.joshsim.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
   *   <li>Filters empty strings to handle cases like "1,,3" gracefully</li>
   *   <li>Trims whitespace around each number</li>
   * </ul>
   *
   * @param outputSteps Comma-separated string of step numbers (e.g., "5,7,8,9,20")
   * @return Optional containing the set of steps to export, or empty if all steps
   *     should be exported
   * @throws IllegalArgumentException if the output steps format is invalid
   */
  public static Optional<Set<Integer>> parseForCli(String outputSteps) {
    if (outputSteps == null || outputSteps.trim().isEmpty()) {
      return Optional.empty();
    }
    try {
      Set<Integer> steps = Arrays.stream(outputSteps.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(Integer::parseInt)
          .collect(Collectors.toSet());
      if (steps.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(steps);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid output-steps format: " + outputSteps
          + ". Expected comma-separated integers (e.g., '5,7,8,9,20')");
    }
  }

  /**
   * Parses comma-separated output steps string into a Set of integers for WebAssembly/Remote use.
   *
   * <p>This method provides the same parsing logic as {@link #parseForCli(String)} but
   * throws RuntimeException instead of IllegalArgumentException for consistency with
   * WebAssembly and remote execution error handling patterns.</p>
   *
   * @param outputSteps Comma-separated string of step numbers (e.g., "5,7,8,9,20")
   * @return Optional containing the set of steps to export, or empty if all steps
   *     should be exported
   * @throws RuntimeException if the output steps format is invalid
   */
  public static Optional<Set<Integer>> parseForWasmOrRemote(String outputSteps) {
    if (outputSteps == null || outputSteps.trim().isEmpty()) {
      return Optional.empty();
    }
    try {
      Set<Integer> steps = Arrays.stream(outputSteps.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(Integer::parseInt)
          .collect(Collectors.toSet());
      if (steps.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(steps);
    } catch (NumberFormatException e) {
      throw new RuntimeException("Invalid output steps format: " + outputSteps
          + ". Expected comma-separated integers (e.g., '5,7,8,9,20')");
    }
  }
}
