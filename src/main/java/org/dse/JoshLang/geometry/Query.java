/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.geometry;

/**
 * Interface representing a spatial and temporal query.
 * Provides methods to retrieve geometry and time step information for querying entities.
 */
public interface Query {
    /**
     * Gets the geometry associated with this query.
     *
     * @return the geometry that defines the spatial bounds of the query
     */
    Geometry getGeometry();

    /**
     * Gets the time step associated with this query.
     *
     * @return the time step index for this query
     */
    int getStep();
}