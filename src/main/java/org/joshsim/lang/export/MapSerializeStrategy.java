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
import org.joshsim.engine.value.type.EngineValue;

/**
 * Strategy to serialize Entities into a flat table-like structure.
 */
public class MapSerializeStrategy implements ExportSerializeStrategy<Map<String, String>> {

  @Override
  public Map<String, String> getRecord(Entity entity) {
    return entity.getAttributeNames().stream()
        .filter((x) -> x.startsWith("export."))
        .collect(Collectors.toMap(
            (x) -> x,
            (x) -> {
              Optional<EngineValue> value = entity.getAttributeValue(x);
              return value.isPresent() ? value.get().getAsString() : "";
            }
        ));
  }

}
