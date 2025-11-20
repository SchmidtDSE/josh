/**
 * Wrapper entity that provides synthetic attributes for the meta (simulation) entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Wrapper entity that intercepts attribute access for synthetic attributes on meta entity.
 *
 * <p>This class enables accessing synthetic attributes like `year` and `stepCount` as if they
 * were attributes OF the meta entity, rather than attributes in the current scope. This allows
 * code like {@code meta.year} to work correctly.</p>
 *
 * <p>When {@code meta.year} is accessed:
 * <ul>
 *   <li>The meta entity is wrapped in this MetaScopeEntity</li>
 *   <li>The .year access calls getAttributeValue("year")</li>
 *   <li>This wrapper intercepts and returns the synthetic year value from SyntheticScope</li>
 * </ul>
 * </p>
 *
 * <p>Without this wrapper, {@code meta.year} would fail because year is not an attribute OF
 * the meta entity - it's a synthetic attribute in the current scope.</p>
 */
public class MetaScopeEntity implements Entity {

  private final Entity wrapped;
  private final SyntheticScope syntheticScope;

  /**
   * Create a new MetaScopeEntity wrapper.
   *
   * @param wrapped The actual meta (simulation) entity to wrap.
   * @param syntheticScope The SyntheticScope to get synthetic attribute values from.
   */
  public MetaScopeEntity(Entity wrapped, SyntheticScope syntheticScope) {
    this.wrapped = wrapped;
    this.syntheticScope = syntheticScope;
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    // Intercept synthetic attributes and return them from the scope
    if ("year".equals(name) || "stepCount".equals(name) || "step".equals(name)) {
      return syntheticScope.getSyntheticForMeta(name, wrapped);
    }
    // Delegate all other attributes to the wrapped meta entity
    return wrapped.getAttributeValue(name);
  }

  @Override
  public Optional<EngineValue> getAttributeValue(int index) {
    // Index-based access always goes to the wrapped entity
    // (synthetic attributes don't have indices)
    return wrapped.getAttributeValue(index);
  }

  // Delegate all other Entity methods to wrapped

  @Override
  public Optional<EngineGeometry> getGeometry() {
    return wrapped.getGeometry();
  }

  @Override
  public String getName() {
    return wrapped.getName();
  }

  @Override
  public Optional<Integer> getAttributeIndex(String name) {
    return wrapped.getAttributeIndex(name);
  }

  @Override
  public Map<String, Integer> getAttributeNameToIndex() {
    return wrapped.getAttributeNameToIndex();
  }

  @Override
  public String[] getIndexToAttributeName() {
    return wrapped.getIndexToAttributeName();
  }

  @Override
  public Set<String> getAttributeNames() {
    // Include synthetic attributes in the attribute names
    Set<String> names = new java.util.HashSet<>(wrapped.getAttributeNames());
    names.add("year");
    names.add("stepCount");
    names.add("step");
    return names;
  }

  @Override
  public EntityType getEntityType() {
    return wrapped.getEntityType();
  }

  @Override
  public Entity freeze() {
    return new MetaScopeEntity(wrapped.freeze(), syntheticScope);
  }

  @Override
  public Optional<GeoKey> getKey() {
    return wrapped.getKey();
  }

  @Override
  public long getSequenceId() {
    return wrapped.getSequenceId();
  }

  @Override
  public Map<String, List<EventHandlerGroup>> getResolvedHandlers() {
    return wrapped.getResolvedHandlers();
  }
}
