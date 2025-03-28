package org.joshsim.engine.entity;

import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Geometric entity that serves as a reference to a generic geometry.
 */
public class ReferenceGeometryEntity extends RootSpatialEntity {

    /**
     * Constructs a RootSpatialEntity with the specified geometry.
     *
     * @param geometry The geometry associated with this entity.
     * @param eventHandlerGroups A map of event keys to their corresponding event handler groups.
     * @param attributes A map of attribute names to their corresponding engine values.
     */
    public ReferenceGeometryEntity(Geometry geometry) {
        super(geometry, "reference", new HashMap<>(), new HashMap<>());
    }

}
