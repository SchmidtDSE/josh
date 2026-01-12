/**
 * Parses metadata from Josh test files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.conformance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses metadata from Josh test files.
 *
 * <p>Expected format in .josh files:
 * <pre>
 * # @category: lifecycle
 * # @subcategory: events
 * # @priority: critical
 * # @issue: #123
 * # @description: Test description here
 * </pre>
 */
class TestMetadata {
  final String category;
  final String subcategory;
  final String priority;
  final String issue;
  final String description;

  /**
   * Constructs a TestMetadata instance with the specified fields.
   *
   * @param category The test category (e.g., "lifecycle", "types", "spatial")
   * @param subcategory The test subcategory (e.g., "events", "conversions")
   * @param priority The test priority (e.g., "critical", "high", "medium", "low")
   * @param issue The related issue number (e.g., "#123")
   * @param description The human-readable test description
   */
  TestMetadata(String category, String subcategory, String priority,
               String issue, String description) {
    this.category = category;
    this.subcategory = subcategory;
    this.priority = priority;
    this.issue = issue;
    this.description = description;
  }

  /**
   * Parses metadata from a Josh test file.
   *
   * <p>Reads the comment header at the top of the file and extracts metadata
   * key-value pairs. Parsing stops at the first non-comment line.
   *
   * @param file The path to the Josh test file to parse
   * @return A TestMetadata object with extracted fields (null for missing fields)
   * @throws Exception If there is an error reading the file
   */
  static TestMetadata parse(Path file) throws Exception {
    Pattern pattern = Pattern.compile("^#\\s*@(\\w+):\\s*(.+)$");

    String category = null;
    String subcategory = null;
    String priority = null;
    String issue = null;
    String description = null;

    for (String line : Files.readAllLines(file)) {
      Matcher matcher = pattern.matcher(line);
      if (matcher.matches()) {
        String key = matcher.group(1);
        String value = matcher.group(2).trim();

        switch (key) {
          case "category":
            category = value;
            break;
          case "subcategory":
            subcategory = value;
            break;
          case "priority":
            priority = value;
            break;
          case "issue":
            issue = value;
            break;
          case "description":
            description = value;
            break;
          default:
            // Ignore unknown metadata keys
            break;
        }
      }
      // Stop reading after first non-comment line
      if (!line.trim().startsWith("#") && !line.trim().isEmpty()) {
        break;
      }
    }

    return new TestMetadata(category, subcategory, priority, issue, description);
  }
}
