/**
 * Structures to provide access to input / output operations when running in the JVM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;


/**
 * Interface for a strategy providing JVM-specific input / output operations.
 */
public class JvmInputOutputLayer implements InputOutputLayer {

  private final JvmExportFacadeFactory exportFactory;
  private final InputGetterStrategy inputStrategy;

  /**
   * Create a new input / output layer with all parameters explicitly specified.
   *
   * @param replicate The replicate number to use in filenames.
   * @param extents The extents of the grid in the simulation in Earth-space (null for grid-only).
   * @param width The width and height of each patch in meters (null for grid-only).
   * @param inputStrategy The strategy for input file access.
   * @param templateRenderer The renderer for processing template strings (null for legacy mode).
   */
  public JvmInputOutputLayer(int replicate, PatchBuilderExtents extents, BigDecimal width,
                             InputGetterStrategy inputStrategy,
                             TemplateStringRenderer templateRenderer) {
    if (extents != null && width != null) {
      this.exportFactory = new JvmExportFacadeFactory(replicate, extents, width, templateRenderer);
    } else {
      this.exportFactory = new JvmExportFacadeFactory(replicate, templateRenderer);
    }
    this.inputStrategy = inputStrategy;
  }

  /**
   * Create a new input / output layer with all parameters explicitly specified (legacy
   * constructor).
   *
   * @param replicate The replicate number to use in filenames.
   * @param extents The extents of the grid in the simulation in Earth-space (null for grid-only).
   * @param width The width and height of each patch in meters (null for grid-only).
   * @param inputStrategy The strategy for input file access.
   * @deprecated Use constructor with TemplateStringRenderer parameter instead
   */
  @Deprecated
  public JvmInputOutputLayer(int replicate, PatchBuilderExtents extents, BigDecimal width,
                             InputGetterStrategy inputStrategy) {
    this(replicate, extents, width, inputStrategy, null);
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
