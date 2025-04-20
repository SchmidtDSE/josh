package org.joshsim.engine.geometry;

/**
 * Strategy for building grid structures.
 *
 * <p>Strategy for creating a rectangular grid of patches either in Earth space or in Grid space
 * depending on execution settings of the simulation.</p>
 */
public interface PatchBuilder {

  /**
   * Builds and returns a PatchSet with transformed coordinates if needed.
   *
   * @return a new PatchSet instance
   */
  PatchSet build();

}
