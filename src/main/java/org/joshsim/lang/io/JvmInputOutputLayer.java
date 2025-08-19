/**
 * Structures to provide access to input / output operations when running in the JVM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;


/**
 * Interface for a strategy providing JVM-specific input / output operations.
 */
public class JvmInputOutputLayer implements InputOutputLayer {

  private final JvmExportFacadeFactory exportFactory;
  private final InputGetterStrategy inputStrategy;

  /**
   * Create a new input / output layer with grid-space only asusming zero-th replicate.
   */
  public JvmInputOutputLayer() {
    exportFactory = new JvmExportFacadeFactory(0);
    inputStrategy = new JvmWorkingDirInputGetter();
  }

  /**
   * Create a new input / output layer with grid-space only.
   *
   * @param replicate The replicate number to use in filenames.
   */
  public JvmInputOutputLayer(int replicate) {
    exportFactory = new JvmExportFacadeFactory(replicate);
    inputStrategy = new JvmWorkingDirInputGetter();
  }

  /**
   * Create a new input / output layer with access to Earth-space.
   *
   * @param replicate The replicate number to use in filenames.
   * @param extents The extents of the grid in the simulation in Earth-space.
   * @param width The width and height of each patch in meters.
   */
  public JvmInputOutputLayer(int replicate, PatchBuilderExtents extents, BigDecimal width) {
    exportFactory = new JvmExportFacadeFactory(replicate, extents, width);
    inputStrategy = new JvmWorkingDirInputGetter();
  }

  /**
   * Create a new input / output layer with grid-space only and custom input strategy.
   *
   * @param replicate The replicate number to use in filenames.
   * @param inputStrategy The strategy for input file access.
   */
  public JvmInputOutputLayer(int replicate, InputGetterStrategy inputStrategy) {
    exportFactory = new JvmExportFacadeFactory(replicate);
    this.inputStrategy = inputStrategy;
  }

  /**
   * Create a new input / output layer with access to Earth-space and custom input strategy.
   *
   * @param replicate The replicate number to use in filenames.
   * @param extents The extents of the grid in the simulation in Earth-space.
   * @param width The width and height of each patch in meters.
   * @param inputStrategy The strategy for input file access.
   */
  public JvmInputOutputLayer(int replicate, PatchBuilderExtents extents, BigDecimal width,
                             InputGetterStrategy inputStrategy) {
    exportFactory = new JvmExportFacadeFactory(replicate, extents, width);
    this.inputStrategy = inputStrategy;
  }

  @Override
  public ExportFacadeFactory getExportFacadeFactory() {
    return exportFactory;
  }

  @Override
  public InputGetterStrategy getInputStrategy() {
    return inputStrategy;
  }

}
