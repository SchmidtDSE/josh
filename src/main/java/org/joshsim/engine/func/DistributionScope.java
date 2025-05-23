/**
 * Structures to describe a scope which contains an entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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

    List<EngineValue> transformedValues = StreamSupport.stream(values.spliterator(), false)
        .map((x) -> new EntityScope(x.getAsEntity()))
        .map(innerResolver::get)
        .map((x) -> x.orElseThrow())
        .collect(Collectors.toList());

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
   * @param target the Distriubtion to sample for an Entity from which to extract attribute names.
   * @return Set of attribute names found in the entity's event handlers or set attributes.
   */
  private Set<String> getAttributes(Distribution target) {
    return StreamSupport
            .stream(target.sample().getAsEntity().getAttributeNames().spliterator(), false)
            .collect(Collectors.toSet());
  }
}
