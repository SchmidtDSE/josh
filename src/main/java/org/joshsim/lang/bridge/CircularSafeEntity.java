/**
 * Minimal wrapper that catches circular dependency errors and returns stored values.
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
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Minimal wrapper that catches circular dependency errors and returns stored values.
 *
 * <p>This class reuses the same pattern as SyntheticScope.get() lines 82-116.
 * When a circular dependency is detected (indicated by the error message
 * "Encountered a loop when resolving"), it bypasses resolution and gets the stored value
 * from the inner entity directly.</p>
 *
 * <p>This is primarily used to wrap the meta (simulation) entity when accessed via
 * {@code meta} in patch or organism code, allowing nested attribute access like
 * {@code meta.fire.trigger.coverThreshold} to work without circular dependency errors.</p>
 */
public class CircularSafeEntity implements Entity {

  private final Entity inner;

  /**
   * Create a new CircularSafeEntity wrapper.
   *
   * @param inner The entity to wrap with circular dependency protection.
   */
  public CircularSafeEntity(Entity inner) {
    this.inner = inner;
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    try {
      return inner.getAttributeValue(name);
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().contains("Encountered a loop when resolving")) {
        // Bypass resolution and get stored value (same as SyntheticScope.get() logic)
        if (inner instanceof ShadowingEntity shadowingEntity) {
          MutableEntity innerEntity = shadowingEntity.getInner();
          if (innerEntity != null) {
            return innerEntity.getAttributeValue(name);
          }
        }
        throw e;
      }
      throw e;
    }
  }

  @Override
  public Optional<EngineValue> getAttributeValue(int index) {
    try {
      return inner.getAttributeValue(index);
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().contains("Encountered a loop when resolving")) {
        if (inner instanceof ShadowingEntity shadowingEntity) {
          MutableEntity innerEntity = shadowingEntity.getInner();
          if (innerEntity != null) {
            return innerEntity.getAttributeValue(index);
          }
        }
        throw e;
      }
      throw e;
    }
  }

  // Delegate all other methods to the inner entity

  @Override
  public Optional<EngineGeometry> getGeometry() {
    return inner.getGeometry();
  }

  @Override
  public String getName() {
    return inner.getName();
  }

  @Override
  public Optional<Integer> getAttributeIndex(String name) {
    return inner.getAttributeIndex(name);
  }

  @Override
  public Map<String, Integer> getAttributeNameToIndex() {
    return inner.getAttributeNameToIndex();
  }

  @Override
  public String[] getIndexToAttributeName() {
    return inner.getIndexToAttributeName();
  }

  @Override
  public Set<String> getAttributeNames() {
    return inner.getAttributeNames();
  }

  @Override
  public EntityType getEntityType() {
    return inner.getEntityType();
  }

  @Override
  public Entity freeze() {
    return new CircularSafeEntity(inner.freeze());
  }

  @Override
  public Optional<GeoKey> getKey() {
    return inner.getKey();
  }

  @Override
  public long getSequenceId() {
    return inner.getSequenceId();
  }

  @Override
  public Map<String, List<EventHandlerGroup>> getResolvedHandlers() {
    return inner.getResolvedHandlers();
  }
}
