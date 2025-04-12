/**
 * Strategy for table-like serialization.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.value.type.EngineValue;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Strategy to serialize Entities into a flat table-like structure.
 */
public class MapSerializeStrategy implements ExportSerializeStrategy<Map<String, String>> {

  @Override
  public Stream<Map<String, String>> getRecords(Stream<Entity> target) {
    return target.map(this::getMap);
  }

  /**
   * Creates a map of attribute names and their corresponding string values from the given Entity.
   *
   * <p>Creates a map of attribute names and their corresponding string values from the given Entity
   * where the attributes whose names start with "export." are included in the resulting map.</p>
   *
   * @param entity the Entity from which the attribute names and values are retrieved
   * @return A map where the keys are attribute names starting with "export." and the values are
   *     their corresponding string values. If the attribute value is missing, an empty string is
   *     used.
   */
  private Map<String, String> getMap(Entity entity) {
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
