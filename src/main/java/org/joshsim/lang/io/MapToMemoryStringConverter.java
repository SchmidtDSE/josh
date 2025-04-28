
/**
 * Logic for converting map data to memory-passing string format.
 *
 * <p>This module provides functionality to convert map data into a string format
 * that can be efficiently passed between different parts of the system, particularly
 * useful for inter-language communication and data serialization.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Map;
import org.joshsim.compat.CompatibilityLayer;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;

/**
 * Utility class for converting map data to a memory-passing string format.
 * 
 * <p>This class provides functionality to convert a map of key-value pairs into
 * a formatted string that can be efficiently passed between different parts of
 * the system. The output format is name:key1=value1\tkey2=value2...</p>
 */
public class MapToMemoryStringConverter {

  /**
   * Converts a named map to a memory-passing string format.
   *
   * <p>Converts the provided map into a tab-delimited string of key-value pairs,
   * prefixed with the provided name. The format is: name:key1=value1\tkey2=value2...</p>
   *
   * <p>Special characters in values (tabs and newlines) are replaced with spaces
   * to ensure safe parsing.</p>
   *
   * @param name The identifier name to prefix the converted string
   * @param target The map of key-value pairs to convert
   * @return A string representation of the map in memory-passing format
   */
  public static String convert(String name, Map<String, String> target) {
    CompatibilityLayer compatibilityLayer = CompatibilityLayerKeeper.get();
    CompatibleStringJoiner joiner = compatibilityLayer.createStringJoiner("\t");

    for (String key : target.keySet()) {
      String value = target.get(key);
      String valueSafe = value.replaceAll("\t", "    ").replaceAll("\n", "    ");
      String assignment = String.format("%s=%s", key, valueSafe);
      joiner.add(assignment);
    }

    return String.format("%s:%s", name, joiner.toString());
  }

}
