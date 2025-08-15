/**
 * Formatter for configuration variable discovery output.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for formatting configuration variable discovery output.
 *
 * <p>This class provides consistent formatting for discovered configuration variables
 * across all discovery endpoints (CLI, server, JS export). Variables are formatted
 * one per line with defaults shown in parentheses when present.</p>
 *
 * <div>Output format examples:
 *   <ul>
 *     <li>Variables with defaults: {@code testVar1(5m)}</li>
 *     <li>Variables without defaults: {@code testVar2}</li>
 *   </ul>
 * </div>
 */
public class ConfigDiscoverabilityOutputFormatter {

  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private ConfigDiscoverabilityOutputFormatter() {
    // Utility class - no instantiation
  }

  /**
   * Formats an iterable of discovered config variables into a string.
   *
   * <p>Each variable is formatted on its own line using the {@link DiscoveredConfigVar#describe()}
   * method. Variables are sorted alphabetically by their name for consistent output.</p>
   *
   * @param discoveredVars Iterable of discovered config variables to format
   * @return Formatted string with one variable per line, or empty string if no variables
   */
  public static String format(Iterable<DiscoveredConfigVar> discoveredVars) {
    return String.join("\n", formatAsLines(discoveredVars));
  }

  /**
   * Formats an iterable of discovered config variables into lines for programmatic use.
   *
   * <p>Returns a list of formatted strings, one per variable, sorted alphabetically.
   * This method is useful when the caller needs to process individual lines rather
   * than a single concatenated string.</p>
   *
   * @param discoveredVars Iterable of discovered config variables to format
   * @return List of formatted strings, one per variable, sorted alphabetically
   */
  public static List<String> formatAsLines(Iterable<DiscoveredConfigVar> discoveredVars) {
    List<String> descriptions = new ArrayList<>();
    for (DiscoveredConfigVar var : discoveredVars) {
      descriptions.add(var.describe());
    }

    // Sort for consistent output ordering
    Collections.sort(descriptions);

    return descriptions;
  }
}
