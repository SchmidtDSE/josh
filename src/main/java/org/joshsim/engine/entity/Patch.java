/**
 * Structures describing a cell within a simulation
 * 
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

/**
 * Spatial entity representing a patch in a simulation.
 *
 * <p>A patch is a spatial unit that can contain other entities which operates effectively as a cell
 * within the JoshSim gridded simulation.
 * </p>
 */
public interface Patch extends SpatialEntity {}