package org.joshsim.lang.bridge;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.geometry.Geometry;


/**
 * Create a decorator that builds a ShadowingEntity from a scope.
 */
public class ShadowingEntityPrototype implements EntityPrototype {

  private final EntityPrototype inner;
  private final Scope scope;

  /**
   * Decorate a prototype with parent information.
   *
   * @param inner Prototype for entity to be decorated as a ShadowingEntity.
   * @param scope The Scope from which to read required attributes.
   */
  public ShadowingEntityPrototype(EntityPrototype inner, Scope scope) {
    this.inner = inner;
    this.scope = scope;
  }

  @Override
  public String getIdentifier() {
    return inner.getIdentifier();
  }

  @Override
  public EntityType getEntityType() {
    return inner.getEntityType();
  }

  @Override
  public MutableEntity build() {
    return wrap(inner.build());
  }

  @Override
  public MutableEntity buildSpatial(Entity parent) {
    return wrap(inner.buildSpatial(parent));
  }

  @Override
  public MutableEntity buildSpatial(Geometry parent) {
    return wrap(inner.buildSpatial(parent));
  }

  @Override
  public boolean requiresParent() {
    return inner.requiresParent();
  }

  @Override
  public boolean requiresGeometry() {
    return inner.requiresGeometry();
  }

  private MutableEntity wrap(MutableEntity entityToWrap) {
    return new ShadowingEntity(
        entityToWrap,
        scope.get("here").getAsEntity(),
        scope.get("meta").getAsEntity()
    );
  }

}
