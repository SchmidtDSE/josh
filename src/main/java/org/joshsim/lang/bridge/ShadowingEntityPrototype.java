package org.joshsim.lang.bridge;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EmbeddedParentEntityPrototype;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.engine.EngineValueFactory;


/**
 * Create a decorator that builds a ShadowingEntity from a scope.
 */
public class ShadowingEntityPrototype implements EntityPrototype {

  private final EngineValueFactory valueFactory;
  private final EntityPrototype inner;
  private final Scope scope;

  /**
   * Decorate a prototype with parent information.
   *
   * @param valueFactory Factory to use in constructing engine values.
   * @param inner Prototype for entity to be decorated as a ShadowingEntity.
   * @param scope The Scope from which to read required attributes.
   */
  public ShadowingEntityPrototype(EngineValueFactory valueFactory, EntityPrototype inner,
        Scope scope) {
    this.valueFactory = valueFactory;
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
    return build(0L);
  }

  /**
   * Build entity with explicit sequence ID.
   *
   * @param sequenceId The sequence ID for this entity.
   * @return A wrapped entity with shadowing behavior.
   */
  public MutableEntity build(long sequenceId) {
    // Check if inner is EmbeddedParentEntityPrototype to call build(sequenceId)
    if (inner instanceof EmbeddedParentEntityPrototype) {
      return wrap(((EmbeddedParentEntityPrototype) inner).build(sequenceId));
    } else {
      return wrap(inner.build());
    }
  }

  @Override
  public MutableEntity buildSpatial(Entity parent) {
    return wrap(inner.buildSpatial(parent));
  }

  @Override
  public MutableEntity buildSpatial(Entity parent, long sequenceId) {
    return wrap(inner.buildSpatial(parent, sequenceId));
  }

  @Override
  public MutableEntity buildSpatial(EngineGeometry parent) {
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
        valueFactory,
        entityToWrap,
        scope.get("here").getAsEntity(),
        scope.get("meta").getAsEntity()
    );
  }

}
