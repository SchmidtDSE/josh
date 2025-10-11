/**
 * Structures to describe a scope which contains an entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.Distribution;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.interpret.ValueResolver;


/**
 * Simple scope which contains an entity.
 */
public class DistributionScope implements Scope {

  private final EngineValueFactory valueFactory;
  private final Distribution value;
  private final Set<String> expectedAttrs;

  /**
   * Create a scope decorator around this distribution.
   *
   * @param valueFactory Factory to use for creating transformed EngineValues.
   * @param value Distribution to use for current.
   */
  public DistributionScope(EngineValueFactory valueFactory, Distribution value) {
    this.valueFactory = valueFactory;
    this.value = value;
    this.expectedAttrs = getAttributes(value);
  }

  @Override
  public EngineValue get(String name) {
    Iterable<EngineValue> values = value.getContents(value.getSize().orElseThrow(), false);

    ValueResolver innerResolver = new ValueResolver(valueFactory, name);

    // Transform values using direct iteration instead of streams
    List<EngineValue> transformedValues = new ArrayList<>();
    for (EngineValue val : values) {
      EntityScope scope = new EntityScope(val.getAsEntity());
      Optional<EngineValue> resolved = innerResolver.get(scope);
      transformedValues.add(resolved.orElseThrow());
    }

    return valueFactory.buildRealizedDistribution(
        transformedValues,
        transformedValues.get(0).getUnits()
    );
  }

  @Override
  public boolean has(String name) {
    return expectedAttrs.contains(name);
  }


  @Override
  public Set<String> getAttributes() {
    return expectedAttrs;
  }

  /**
   * Extract all attribute names from a sampled entity's event handlers or set attributes.
   *
   * <p>Returns a copy of the attribute names set to avoid issues if the original set
   * is immutable or if we need to avoid sharing references.</p>
   *
   * @param target the Distriubtion to sample for an Entity from which to extract attribute names.
   * @return Set of attribute names found in the entity's event handlers or set attributes.
   */
  private Set<String> getAttributes(Distribution target) {
    Set<String> attributeNames = target.sample().getAsEntity().getAttributeNames();
    return new HashSet<>(attributeNames);
  }
}
