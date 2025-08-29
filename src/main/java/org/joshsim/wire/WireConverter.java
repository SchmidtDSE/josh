/**
 * Logic for converting between NamedMap objects and wire format strings.
 *
 * <p>This module provides functionality to serialize NamedMap objects into wire format strings
 * and deserialize wire format strings back into NamedMap objects. This enables inter-language
 * communication and data serialization between different parts of the system.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.joshsim.compat.CompatibilityLayer;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;


/**
 * Utility class for converting between NamedMap objects and wire format strings.
 *
 * <p>This class provides bidirectional conversion functionality for NamedMap objects.
 * The wire format is: name:key1=value1\tkey2=value2... where special characters
 * in values (tabs and newlines) are replaced with spaces to ensure safe parsing.</p>
 */
public class WireConverter {

  /**
   * Serializes a NamedMap to wire format string.
   *
   * <p>Converts the provided NamedMap into a tab-delimited string of key-value pairs,
   * prefixed with the name from the NamedMap. The format is: name:key1=value1\tkey2=value2...
   * Special characters in values (tabs and newlines) are replaced with spaces to ensure
   * safe parsing.</p>
   *
   * @param namedMap The NamedMap containing name and target map to serialize
   * @return A string representation of the NamedMap in wire format
   * @throws IllegalArgumentException if namedMap is null
   */
  public static String serializeToString(NamedMap namedMap) {
    if (namedMap == null) {
      throw new IllegalArgumentException("NamedMap cannot be null");
    }

    CompatibilityLayer compatibilityLayer = CompatibilityLayerKeeper.get();
    CompatibleStringJoiner joiner = compatibilityLayer.createStringJoiner("\t");

    for (String key : namedMap.getTarget().keySet()) {
      String value = namedMap.getTarget().get(key);
      String valueSafe = value.replaceAll("\t", "    ").replaceAll("\n", "    ");
      String assignment = String.format("%s=%s", key, valueSafe);
      joiner.add(assignment);
    }

    return String.format("%s:%s", namedMap.getName(), joiner.toString());
  }

  /**
   * Deserializes a wire format string to a NamedMap.
   *
   * <p>Parses a wire format string back into a NamedMap object. The expected format
   * is: name:key1=value1\tkey2=value2... This method performs the inverse operation
   * of serializeToString.</p>
   *
   * @param wireFormat The wire format string to deserialize
   * @return A NamedMap object containing the parsed name and key-value pairs
   * @throws IllegalArgumentException if wireFormat is null, empty, or malformed
   */
  public static NamedMap deserializeFromString(String wireFormat) {
    if (wireFormat == null) {
      throw new IllegalArgumentException("Wire format string cannot be null");
    }
    if (wireFormat.trim().isEmpty()) {
      throw new IllegalArgumentException("Wire format string cannot be empty");
    }

    // Find the first colon to separate name from data
    int colonIndex = wireFormat.indexOf(':');
    if (colonIndex == -1) {
      throw new IllegalArgumentException("Wire format must contain a colon separator");
    }
    if (colonIndex == 0) {
      throw new IllegalArgumentException("Wire format must have a non-empty name before colon");
    }

    String name = wireFormat.substring(0, colonIndex);
    String dataSection = wireFormat.substring(colonIndex + 1);

    Map<String, String> target = new HashMap<>();

    // Handle empty data section (just name:)
    if (dataSection.isEmpty()) {
      return new NamedMap(name, target);
    }

    // Use Stream to split by tabs, filter empty pairs, and collect to map
    target = Arrays.stream(dataSection.split("\t"))
        .filter(pair -> !pair.isEmpty())
        .collect(Collectors.toMap(
            pair -> {
              int equalsIndex = pair.indexOf('=');
              if (equalsIndex == -1) {
                throw new IllegalArgumentException(
                    String.format("Invalid key-value pair format: '%s'", pair)
                );
              }
              String key = pair.substring(0, equalsIndex);
              if (key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be empty in key-value pair");
              }
              return key;
            },
            pair -> {
              int equalsIndex = pair.indexOf('=');
              return pair.substring(equalsIndex + 1);
            }
        ));

    return new NamedMap(name, target);
  }


}
