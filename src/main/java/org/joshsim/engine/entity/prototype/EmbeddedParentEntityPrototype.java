package org.joshsim.engine.entity.prototype;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.Geometry;


/**
 * Create a decorator that embeds a parent for an entity prototype.
 *
 * <p>Decorator around an entity prototype which supplies a parent or a parent's geometry in
 * construction if needed and is not given to the build method.</p>
 */
public class EmbeddedParentEntityPrototype implements EntityPrototype {

  private final EntityPrototype inner;
  private final Entity parent;

  /**
   * Decorate a prototype with parent information.
   *
   * @param inner Prototype for entity to be decorated and which will be a child of parent.
   * @param parent Entity from which to get geometry data.
   */
  public EmbeddedParentEntityPrototype(EntityPrototype inner, Entity parent) {
    this.inner = inner;
    this.parent = parent;
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
    if (inner.requiresParent()) {
      return inner.buildSpatial(parent);
    } else if (inner.requiresGeometry()) {
      return inner.buildSpatial(parent.getGeometry().orElseThrow());
    } else {
      return inner.build();
    }
  }

  @Override
  public MutableEntity buildSpatial(Entity parent) {
    return inner.buildSpatial(parent);
  }

  @Override
  public MutableEntity buildSpatial(Geometry parent) {
    return inner.buildSpatial(parent);
  }

  @Override
  public boolean requiresParent() {
    return false;
  }

  @Override
  public boolean requiresGeometry() {
    return false;
  }


}
