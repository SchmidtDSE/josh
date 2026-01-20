
/**
 * Builder to help build a mapping strategy.
 *
 * <p>Structure describing bounds to be used in a mapping function.</p>
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

  /**
   * Create a new mapping builder.
   */
  public MappingBuilder() {
    valueFactory = Optional.empty();
    domain = Optional.empty();
    range = Optional.empty();
    mapBehaviorArgument = Optional.empty();
  }

  /**
   * Set the value factory to be used in constructing returned and supporting values.
   *
   * @param valueFactory The value factory to use.
   * @return This builder instance for method chaining.
   */
  public MappingBuilder setValueFactory(EngineValueFactory valueFactory) {
    this.valueFactory = Optional.of(valueFactory);
    return this;
  }

  /**
   * Set the domain from which values provided will be mapped.
   *
   * @param domain The domain bounds to use.
   * @return This builder instance for method chaining.
   */
  public MappingBuilder setDomain(MapBounds domain) {
    this.domain = Optional.of(domain);
    return this;
  }

  /**
   * Set the range to which values provided will be mapped.
   *
   * @param range The range bounds to use.
   * @return This builder instance for method chaining.
   */
  public MappingBuilder setRange(MapBounds range) {
    this.range = Optional.of(range);
    return this;
  }

  /**
   * Set the mapping behavior argument that controls aspects of the mapping.
   *
   * @param mapBehaviorArgument The behavior argument to use.
   * @return This builder instance for method chaining.
   */
  public MappingBuilder setMapBehaviorArgument(EngineValue mapBehaviorArgument) {
    this.mapBehaviorArgument = Optional.of(mapBehaviorArgument);
    return this;
  }

  /**
   * Build a mapping strategy based on the provided strategy name and builder settings.
   *
   * <p>Accepts both base forms ("linear", "quadratic", "sigmoid") and adverb forms
   * ("linearly", "quadratically", "sigmoidally") as used in Josh language syntax.</p>
   *
   * @param strategyName The name of the mapping strategy to build.
   * @return The constructed mapping strategy.
   * @throws IllegalArgumentException if the strategy name is unknown or required fields are
   *     missing.
   */
  public MapStrategy build(String strategyName) {
    // Normalize strategy name - accept both base and adverb forms
    String normalizedName = normalizeStrategyName(strategyName);

    return switch (normalizedName) {
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

  /**
   * Normalize strategy name by converting adverb forms to base forms.
   *
   * @param strategyName The strategy name (may be "linear" or "linearly", etc.)
   * @return The normalized base form ("linear", "quadratic", or "sigmoid")
   */
  private String normalizeStrategyName(String strategyName) {
    return switch (strategyName) {
      case "linearly" -> "linear";
      case "quadratically" -> "quadratic";
      case "sigmoidally" -> "sigmoid";
      default -> strategyName;
    };
  }
}
