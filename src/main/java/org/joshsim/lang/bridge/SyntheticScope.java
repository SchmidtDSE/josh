/**
 * Structures to describe a scope which has synthetic attributes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.func.CombinedAttributeNameIterable;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Scope which adds synthetic attributes to an entity.
 *
 * <p>Create a scope which adds synthetic attributes that allow referring to various entities within
 * a hierarchy like current, prior, here, and meta. These are Josh language keywords that are
 * provided as convienence.</p>
 */
public class SyntheticScope implements Scope {

  public static final Set<String> SYNTHETIC_ATTRS = Set.of("current", "prior", "here", "meta");

  private final ShadowingEntity inner;
  private final EngineValueFactory valueFactory;

  /**
   * Create a scope decorator around this entity.
   *
   * @param inner ShadowingEntity to use for the root.
   */
  public SyntheticScope(ShadowingEntity inner) {
    this.inner = inner;
    this.valueFactory = EngineValueFactory.getDefault();
  }

  /**
   * Create a scope decorator around this entity with a specified EngineValueFactory.
   *
   * @param inner ShadowingEntity to use for the root.
   * @param valueFactory EngineValueFactory to generate EngineValue instances.
   */
  public SyntheticScope(ShadowingEntity inner, EngineValueFactory valueFactory) {
    this.inner = inner;
    this.valueFactory = valueFactory;
  }

  @Override
  public EngineValue get(String name) {
    Optional<EngineValue> syntheticValue = getSynthetic(name);
    if (syntheticValue.isPresent()) {
      return syntheticValue.get();
    }

    Optional<EngineValue> currentValue = inner.getAttributeValue(name);
    return currentValue.orElseThrow();
  }

  @Override
  public boolean has(String name) {
    if (inner.hasAttribute(name)) {
      return true;
    } else {
      return SYNTHETIC_ATTRS.contains(name);
    }
  }

  @Override
  public Set<String> getAttributes() {
    Set<String> newSet = new HashSet<>(SYNTHETIC_ATTRS);
    newSet.addAll(inner.getAttributeNames());
    return newSet;
  }

  private Optional<EngineValue> getSynthetic(String name) {
    return switch (name) {
      case "current" -> Optional.of(valueFactory.build(inner));
      case "prior" -> Optional.of(valueFactory.build(new PriorShadowingEntityDecorator(inner)));
      case "here" -> Optional.of(valueFactory.build(inner.getHere()));
      case "meta" -> Optional.of(valueFactory.build(inner.getMeta()));
      default -> Optional.empty();
    };
  }

}
