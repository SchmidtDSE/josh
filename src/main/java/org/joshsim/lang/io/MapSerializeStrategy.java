/**
 * Strategy for table-like serialization.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.io.strategy.MapExportSerializeStrategy;

/**
 * Strategy to serialize Entities into a flat table-like structure.
 */
public class MapSerializeStrategy implements MapExportSerializeStrategy {

  @Override
  public Map<String, String> getRecord(Entity entity) {
    int estimatedSize = entity.getAttributeNames().size() / 4 + 1;
    Map<String, String> result = new HashMap<>(estimatedSize);

    for (String name : entity.getAttributeNames()) {
      if (name.startsWith("export.")) {
        String key = name.replaceFirst("export\\.", "");
        Optional<EngineValue> value = entity.getAttributeValue(name);
        String valueStr = value.isPresent() ? value.get().getAsString() : "";
        result.put(key, valueStr);
      }
    }

    if (entity.getGeometry().isPresent()) {
      EngineGeometry geometry = entity.getGeometry().get();
      result.put("position.x", geometry.getCenterX().toString());
      result.put("position.y", geometry.getCenterY().toString());
    }

    return result;
  }

}
