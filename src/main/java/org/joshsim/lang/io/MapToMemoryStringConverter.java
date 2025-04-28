package org.joshsim.lang.io;

import java.util.Map;
import org.joshsim.compat.CompatibilityLayer;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;


public class MapToMemoryStringConverter {

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
