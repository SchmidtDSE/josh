/**
 * Structures to provide access to input / output operations when running in the JVM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.joshsim.util.MinioOptions;


/**
 * Builder for creating JvmInputOutputLayer instances with default values.
 */
public class JvmInputOutputLayerBuilder {

  private int replicate = 0;
  private PatchBuilderExtents extents = null;
  private BigDecimal width = null;
  private InputGetterStrategy inputStrategy = new JvmWorkingDirInputGetter();
  private TemplateStringRenderer templateRenderer = null;
  private MinioOptions minioOptions = null;
  private Map<String, String> customTags = Collections.emptyMap();
  private PathTemplateResolver pathTemplateResolver = null;

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
   * Set a custom template string renderer.
   *
   * @param templateRenderer The renderer for processing template strings in export paths.
   * @return This builder instance for chaining.
   */
  public JvmInputOutputLayerBuilder withTemplateRenderer(TemplateStringRenderer templateRenderer) {
    this.templateRenderer = templateRenderer;
    return this;
  }

  /**
   * Set MinIO configuration options.
   *
   * @param minioOptions The MinIO configuration options.
   * @return This builder instance for chaining.
   */
  public JvmInputOutputLayerBuilder withMinioOptions(MinioOptions minioOptions) {
    this.minioOptions = minioOptions;
    return this;
  }

  /**
   * Set custom tags for template resolution.
   *
   * @param customTags Map of custom tag names to their values for debug path templates.
   * @return This builder instance for chaining.
   */
  public JvmInputOutputLayerBuilder withCustomTags(Map<String, String> customTags) {
    this.customTags = customTags != null ? customTags : Collections.emptyMap();
    return this;
  }

  /**
   * Set a shared PathTemplateResolver for consistent timestamps across jobs.
   *
   * @param pathTemplateResolver The shared path template resolver instance.
   * @return This builder instance for chaining.
   */
  public JvmInputOutputLayerBuilder withPathTemplateResolver(
      PathTemplateResolver pathTemplateResolver) {
    this.pathTemplateResolver = pathTemplateResolver;
    return this;
  }

  /**
   * Build the JvmInputOutputLayer instance.
   *
   * @return A new JvmInputOutputLayer instance with the configured parameters.
   */
  public JvmInputOutputLayer build() {
    return new JvmInputOutputLayer(replicate, extents, width, inputStrategy, templateRenderer,
                                    minioOptions, customTags, pathTemplateResolver);
  }
}
