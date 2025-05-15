/**
 * Builder to help build a mapping strategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.mapping;
import java.util.Optional;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Structure describing bounds to be used in a mapping function.
 *
 * <p>Structure describing bounds to be used in a mapping function such as a function's domain or
 * its range.</p>
 */
public class MappingBuilder {

  private Optional<EngineValueFactory> valueFactory;
  private Optional<MapBounds> domain;
  private Optional<MapBounds> range;
  private Optional<EngineValue> mapBehaviorArgument;

  public MappingBuilder() {
    valueFactory = Optional.empty();
    domain = Optional.empty();
    range = Optional.empty();
    mapBehaviorArgument = Optional.empty();
  }

  public MappingBuilder setValueFactory(EngineValueFactory valueFactory) {
    this.valueFactory = Optional.ofNullable(valueFactory);
    return this;
  }

  public MappingBuilder setDomain(MapBounds domain) {
    this.domain = Optional.ofNullable(domain);
    return this;
  }

  public MappingBuilder setRange(MapBounds range) {
    this.range = Optional.ofNullable(range);
    return this;
  }

  public MappingBuilder setMapBehaviorArgument(EngineValue mapBehaviorArgument) {
    this.mapBehaviorArgument = Optional.ofNullable(mapBehaviorArgument);
    return this;
  }
  
  public MapStrategy build(String strategyName) {
    return switch (strategyName) {
      case "linear" -> new LinearMapStrategy(
          valueFactory.orElseThrow(),
          domain.orElseThrow(),
          range.orElseThrow()
      );
      case "quadratic" -> new QuadraticMapStrategy(
          valueFactory.orElseThrow(),
          domain.orElseThrow(),
          range.orElseThrow(),
          mapBehaviorArgument.orElseThrow().getAsBoolean()
      );
      case "sigmoid" -> new SigmoidMapStrategy(
          valueFactory.orElseThrow(),
          domain.orElseThrow(),
          range.orElseThrow(),
          mapBehaviorArgument.orElseThrow().getAsBoolean()
      );
      default -> throw new IllegalArgumentException("Unknown mapping: " + strategyName);
    };
  }
  
}
