/**
 * Structures to provide access to input / output operations when running in the JVM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.math.BigDecimal;
import java.util.Map;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.joshsim.util.MinioOptions;


/**
 * Interface for a strategy providing JVM-specific input / output operations.
 */
public class JvmInputOutputLayer implements InputOutputLayer {

  private final JvmExportFacadeFactory exportFactory;
  private final org.joshsim.lang.io.debug.JvmDebugFacadeFactory debugFactory;
  private final InputGetterStrategy inputStrategy;

  /**
   * Create a new input / output layer with all parameters explicitly specified.
   *
   * @param replicate The replicate number to use in filenames.
   * @param extents The extents of the grid in the simulation in Earth-space (null for grid-only).
   * @param width The width and height of each patch in meters (null for grid-only).
   * @param inputStrategy The strategy for input file access.
   * @param templateRenderer The renderer for processing template strings (null for legacy mode).
   * @param minioOptions The MinIO configuration options (null if not using MinIO).
   * @param customTags Custom tags for debug path template resolution (nullable).
   */
  public JvmInputOutputLayer(int replicate, PatchBuilderExtents extents, BigDecimal width,
                             InputGetterStrategy inputStrategy,
                             TemplateStringRenderer templateRenderer,
                             MinioOptions minioOptions,
                             Map<String, String> customTags) {
    if (extents != null && width != null) {
      this.exportFactory = new JvmExportFacadeFactory(replicate, extents, width,
                                                       templateRenderer, minioOptions);
    } else {
      this.exportFactory = new JvmExportFacadeFactory(replicate, templateRenderer,
                                                       minioOptions);
    }

    // Create PathTemplateResolver for debug facade with custom tags
    PathTemplateResolver debugTemplateResolver = new PathTemplateResolver(customTags);

    this.debugFactory = new org.joshsim.lang.io.debug.JvmDebugFacadeFactory(
        replicate,
        minioOptions,
        debugTemplateResolver
    );
    this.inputStrategy = inputStrategy;
  }

  /**
   * Create a new input / output layer without custom tags (legacy constructor).
   *
   * @param replicate The replicate number to use in filenames.
   * @param extents The extents of the grid in the simulation in Earth-space (null for grid-only).
   * @param width The width and height of each patch in meters (null for grid-only).
   * @param inputStrategy The strategy for input file access.
   * @param templateRenderer The renderer for processing template strings (null for legacy mode).
   * @param minioOptions The MinIO configuration options (null if not using MinIO).
   * @deprecated Use constructor with customTags parameter instead
   */
  @Deprecated
  public JvmInputOutputLayer(int replicate, PatchBuilderExtents extents, BigDecimal width,
                             InputGetterStrategy inputStrategy,
                             TemplateStringRenderer templateRenderer,
                             MinioOptions minioOptions) {
    this(replicate, extents, width, inputStrategy, templateRenderer, minioOptions, null);
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
    this(replicate, extents, width, inputStrategy, null, null, null);
  }

  @Override
  public ExportFacadeFactory getExportFacadeFactory() {
    return exportFactory;
  }

  @Override
  public org.joshsim.lang.io.debug.DebugFacadeFactory getDebugFacadeFactory() {
    return debugFactory;
  }

  @Override
  public InputGetterStrategy getInputStrategy() {
    return inputStrategy;
  }

}
