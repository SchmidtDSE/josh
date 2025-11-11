/**
 * Resolves path template variables for export and debug output.
 *
 * <p>This class handles template variable substitution in file paths,
 * supporting both system variables ({replicate}, {step}, {variable})
 * and custom tags provided via command-line arguments.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves template variables in path strings.
 *
 * <p>Template variables are specified in {name} format and include:
 * <ul>
 *   <li>{replicate} - The replicate number</li>
 *   <li>{timestamp} - Auto-generated timestamp (yyyyMMdd_HHmmss format)</li>
 *   <li>{user} - Custom tag for user identification</li>
 *   <li>{editor} - Custom tag for editor/configuration name</li>
 *   <li>Any other custom tags provided via --custom-tag arguments</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * Map&lt;String, String&gt; tags = Map.of("user", "nick", "editor", "optimistic");
 * PathTemplateResolver resolver = new PathTemplateResolver(tags);
 * String path = resolver.resolve("minio://bucket/{user}/{editor}/debug_{replicate}.txt", 1);
 * // Result: "minio://bucket/nick/optimistic/debug_1.txt"
 * </pre>
 */
public class PathTemplateResolver {

  private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{([^}]+)\\}");
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

  private final Map<String, String> customTags;
  private final String timestamp;

  /**
   * Creates a new PathTemplateResolver with custom tags.
   *
   * @param customTags Map of custom tag names to their values (nullable)
   */
  public PathTemplateResolver(Map<String, String> customTags) {
    this.customTags = customTags != null ? customTags : Collections.emptyMap();
    // Generate timestamp once per resolver instance to ensure consistency across all paths
    this.timestamp = TIMESTAMP_FORMAT.format(Instant.now());
  }

  /**
   * Creates a new PathTemplateResolver with no custom tags.
   */
  public PathTemplateResolver() {
    this(Collections.emptyMap());
  }

  /**
   * Resolves all template variables in the given path template.
   *
   * @param template The template string containing variables in {name} format
   * @param replicate The replicate number to substitute for {replicate}
   * @return The resolved path with all template variables replaced
   * @throws IllegalArgumentException if template contains unknown variables
   */
  public String resolve(String template, int replicate) {
    if (template == null || template.isEmpty()) {
      return template;
    }

    String result = template;

    // Replace {replicate} with the actual replicate number
    result = result.replace("{replicate}", String.valueOf(replicate));

    // Replace {timestamp} with the auto-generated timestamp
    result = result.replace("{timestamp}", timestamp);

    // Replace custom tags
    for (Map.Entry<String, String> entry : customTags.entrySet()) {
      String placeholder = "{" + entry.getKey() + "}";
      result = result.replace(placeholder, entry.getValue());
    }

    // Check for any remaining unresolved templates
    Matcher matcher = TEMPLATE_PATTERN.matcher(result);
    if (matcher.find()) {
      String unresolvedVar = matcher.group(1);
      // Skip {step} and {variable} as they are handled elsewhere
      if (!unresolvedVar.equals("step") && !unresolvedVar.equals("variable")) {
        throw new IllegalArgumentException(
            "Unknown template variable: {" + unresolvedVar + "} in path: " + template
            + ". Available custom tags: " + customTags.keySet()
        );
      }
    }

    return result;
  }

  /**
   * Gets the custom tags map.
   *
   * @return Unmodifiable view of the custom tags
   */
  public Map<String, String> getCustomTags() {
    return Collections.unmodifiableMap(customTags);
  }

  @Override
  public String toString() {
    return "PathTemplateResolver{customTags=" + customTags + "}";
  }
}
