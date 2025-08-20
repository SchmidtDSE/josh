/**
 * Structures to provide access to input / output operations when running in the JVM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;


/**
 * Builder for creating JvmInputOutputLayer instances with default values.
 */
public class JvmInputOutputLayerBuilder {

  private int replicate = 0;
  private PatchBuilderExtents extents = null;
  private BigDecimal width = null;
  private InputGetterStrategy inputStrategy = new JvmWorkingDirInputGetter();

  /**
   * Set the replicate number to use in filenames.
   *
   * @param replicate The replicate number.
   * @return This builder instance for chaining.
   */
  public JvmInputOutputLayerBuilder withReplicate(int replicate) {
    this.replicate = replicate;
    return this;
  }

  /**
   * Set the extents and width for Earth-space access.
   *
   * @param extents The extents of the grid in the simulation in Earth-space.
   * @param width The width and height of each patch in meters.
   * @return This builder instance for chaining.
   */
  public JvmInputOutputLayerBuilder withEarthSpace(PatchBuilderExtents extents, BigDecimal width) {
    this.extents = extents;
    this.width = width;
    return this;
  }

  /**
   * Set a custom input strategy.
   *
   * @param inputStrategy The strategy for input file access.
   * @return This builder instance for chaining.
   */
  public JvmInputOutputLayerBuilder withInputStrategy(InputGetterStrategy inputStrategy) {
    this.inputStrategy = inputStrategy;
    return this;
  }

  /**
   * Build the JvmInputOutputLayer instance.
   *
   * @return A new JvmInputOutputLayer instance with the configured parameters.
   */
  public JvmInputOutputLayer build() {
    return new JvmInputOutputLayer(replicate, extents, width, inputStrategy);
  }
}
