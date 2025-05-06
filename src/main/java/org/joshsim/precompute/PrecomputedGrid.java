/**
 * Interface for pre-computed data grids like those loaded from jshd files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;


public interface PreomputedGrid {

  EngineValue getAt(PatchKey, long timestep)

}
