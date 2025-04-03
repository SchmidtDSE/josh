package org.joshsim.lang.bridge;

import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.type.EngineValue;


public class PriorShadowingEntityDecorator implements Entity {

  private final ShadowingEntity inner;
  
  public PriorShadowingEntityDecorator(ShadowingEntity inner) {
    this.inner = inner;
  }

  @Override
  public Optional<Geometry> getGeometry() {
    return inner.getGeometry();
  }

  @Override
  public String getName() {
    return inner.getName();
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    return Optional.of(inner.getPriorAttribute(name));
  }

  @Override
  public Iterable<String> getAttributeNames() {
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
