/**
 * Strategy for table-like serialization.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Strategy to serialize Entities into a flat table-like structure.
 */
public class MapSerializeStrategy implements ExportSerializeStrategy<Map<String, String>> {

  @Override
  public Map<String, String> getRecord(Entity entity) {
    Map<String, String> result = entity.getAttributeNames().stream()
        .filter((x) -> x.startsWith("export."))
        .collect(Collectors.toMap(
            (x) -> x.replaceFirst("export\\.", ""),
            (x) -> {
              Optional<EngineValue> value = entity.getAttributeValue(x);
              return value.isPresent() ? value.get().getAsString() : "";
            }
        ));

    if (entity.getGeometry().isPresent()) {
      EngineGeometry geometry = entity.getGeometry().get();
      result.put("position.x", geometry.getCenterX().toString());
      result.put("position.y", geometry.getCenterY().toString());
    }

    return result;
  }

}
