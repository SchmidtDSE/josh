/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.core.entity.meta;

import org.dse.JoshLang.core.distribution.Distribution;
import org.dse.JoshLang.core.geometry.Geometry;

/**
 * Represents an external resource entity in the system.
 * Provides access to distributed values based on a geometry.
 */
public interface ExternalResource extends Entity {
    /**
     * Gets distribution values for the specified geometry.
     *
     * @param geometry the geometry to query values for
     * @return the distribution of values for the given geometry
     */
    Distribution getValues(Geometry geometry);
}