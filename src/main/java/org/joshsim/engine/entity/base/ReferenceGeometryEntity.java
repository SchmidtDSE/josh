package org.joshsim.engine.entity.base;

import java.util.Collections;
import java.util.HashMap;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;

/**
 * Geometric entity that serves as a reference to a generic geometry.
 */
public class ReferenceGeometryEntity extends RootSpatialEntity {

  /**
   * Constructs a RootSpatialEntity with the specified geometry.
   *
   * @param geometry The geometry associated with this entity.
   */
  public ReferenceGeometryEntity(EngineGeometry geometry) {
    // ReferenceGeometryEntity has no handlers, so pass empty map
    super(geometry, "reference", Collections.emptyMap(), new HashMap<>(), Collections.emptyMap());
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.REFERENCE;
  }
}
