package org.joshsim.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for parsing output phases parameters across different execution modes.
 *
 * <p>This utility provides consistent parsing logic for comma-separated output phase
 * specifications used throughout the Josh simulation platform. It handles validation,
 * error reporting, and conversion to appropriate data structures. Valid phases are
 * {@code spinup}, {@code observed}, and {@code spindown}.</p>
 */
public final class OutputPhasesParser {

  private static final Set<String> VALID_PHASES = Set.of("spinup", "observed", "spindown");

  private OutputPhasesParser() {
    // Prevent instantiation of utility class
  }

  /**
   * Parses comma-separated output phases string into a Set of phase names.
   *
   * <p>This method handles various input formats gracefully:</p>
   * <ul>
   *   <li>Empty/null/whitespace-only strings return Optional.empty() (export all phases)</li>
   *   <li>Comma-separated phases like "observed,spindown" return Set of those phases</li>
   *   <li>Filters empty strings to handle cases like "observed,,spindown" gracefully</li>
   *   <li>Trims whitespace around each phase and lowercases it</li>
   * </ul>
   *
   * @param outputPhases Comma-separated string of phase names (e.g., "observed,spindown")
   * @return Optional containing the set of phases to export, or empty if all phases
   *     should be exported
   * @throws IllegalArgumentException if the output phases format is invalid
   */
  public static Optional<Set<String>> parseForCli(String outputPhases) {
    if (outputPhases == null || outputPhases.trim().isEmpty()) {
      return Optional.empty();
    }
    Set<String> phases = Arrays.stream(outputPhases.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(s -> s.toLowerCase(java.util.Locale.ROOT))
        .collect(Collectors.toSet());
    if (phases.isEmpty()) {
      return Optional.empty();
    }
    for (String phase : phases) {
      if (!VALID_PHASES.contains(phase)) {
        throw new IllegalArgumentException("Invalid output-phases value: " + phase
            + ". Valid values are: spinup, observed, spindown");
      }
    }
    return Optional.of(phases);
  }

  /**
   * Parses comma-separated output phases string into a Set of phase names for WebAssembly/Remote
   * use.
   *
   * <p>This method provides the same parsing logic as {@link #parseForCli(String)} but
   * throws RuntimeException instead of IllegalArgumentException for consistency with
   * WebAssembly and remote execution error handling patterns.</p>
   *
   * @param outputPhases Comma-separated string of phase names (e.g., "observed,spindown")
   * @return Optional containing the set of phases to export, or empty if all phases
   *     should be exported
   * @throws RuntimeException if the output phases format is invalid
   */
  public static Optional<Set<String>> parseForWasmOrRemote(String outputPhases) {
    if (outputPhases == null || outputPhases.trim().isEmpty()) {
      return Optional.empty();
    }
    Set<String> phases = Arrays.stream(outputPhases.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(s -> s.toLowerCase(java.util.Locale.ROOT))
        .collect(Collectors.toSet());
    if (phases.isEmpty()) {
      return Optional.empty();
    }
    for (String phase : phases) {
      if (!VALID_PHASES.contains(phase)) {
        throw new RuntimeException("Invalid output phases value: " + phase
            + ". Valid values are: spinup, observed, spindown");
      }
    }
    return Optional.of(phases);
  }
}
