/**
 * Decorator allowing a shadowing entity to provide prior values like a frozen entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.Optional;
import java.util.Set;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Decorator allowing a shadowing entity to provide prior attribute values.
 */
public class PriorShadowingEntityDecorator implements Entity {

  private final ShadowingEntity inner;

  /**
   * Constructs a PriorShadowingEntityDecorator with the specified inner ShadowingEntity.
   *
   * @param inner the ShadowingEntity to be decorated and used to provide prior attribute values.
   */
  public PriorShadowingEntityDecorator(ShadowingEntity inner) {
    this.inner = inner;
  }

  @Override
  public Optional<EngineGeometry> getGeometry() {
    return inner.getGeometry();
  }

  @Override
  public String getName() {
    return inner.getName();
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    return inner.getPriorAttribute(name);
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
    return inner.freeze();
  }

  @Override
  public Optional<GeoKey> getKey() {
    return inner.getKey();
  }
}
