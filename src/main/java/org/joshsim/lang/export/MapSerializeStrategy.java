package org.joshsim.lang.export;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.value.type.EngineValue;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapSerializeStrategy implements ExportSerializeStrategy<Map<String, String>> {

  @Override
  public Stream<Map<String, String>> getRecords(Stream<Entity> target) {
    return target.map(this::getMap);
  }

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
