/**
 * JVM-specific implementation of ExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.lang.io.strategy.CsvExportFacade;
import org.joshsim.lang.io.strategy.GeotiffExportFacade;
import org.joshsim.lang.io.strategy.MapExportSerializeStrategy;
import org.joshsim.lang.io.strategy.NetcdfExportFacade;
import org.joshsim.lang.io.strategy.ParameterizedCsvExportFacade;
import org.joshsim.lang.io.strategy.ParameterizedNetcdfExportFacade;
import org.joshsim.pipeline.job.config.TemplateResult;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.joshsim.util.MinioClientSingleton;
import org.joshsim.util.MinioOptions;

/**
 * Factory implementation for creating ExportFacade instances in a JVM environment.
 */
public class JvmExportFacadeFactory implements ExportFacadeFactory {

  private final int replicate;
  private final MapExportSerializeStrategy serializeStrategy;
  private final Optional<PatchBuilderExtents> extents;
  private final Optional<BigDecimal> width;
  private final TemplateStringRenderer templateRenderer;
  private final MinioOptions minioOptions;
  private TemplateResult lastTemplateResult;

  /**
   * Create a new JvmExportFacadeFactory with only grid-space.
   *
   * <p>Creates a new export facade factory which does not try to add latitude and longitude to
   * returned records, disallowing use of geotiffs and netCDF as export formats.</p>
   *
   * @param replicate The replicate number to use in filenames.
   * @param templateRenderer The template renderer for processing export path templates (nullable).
   * @param minioOptions The MinIO configuration options (nullable).
   */
  public JvmExportFacadeFactory(int replicate, TemplateStringRenderer templateRenderer,
                                MinioOptions minioOptions) {
    this.replicate = replicate;
    this.templateRenderer = templateRenderer;
    this.minioOptions = minioOptions;
    serializeStrategy = new MapSerializeStrategy();
    extents = Optional.empty();
    width = Optional.empty();
  }

  /**
   * Create a new JvmExportFacadeFactory with only grid-space (legacy constructor).
   *
   * <p>Creates a new export facade factory which does not try to add latitude and longitude to
   * returned records, disallowing use of geotiffs and netCDF as export formats.</p>
   *
   * @param replicate The replicate number to use in filenames.
   * @deprecated Use constructor with TemplateStringRenderer parameter instead
   */
  @Deprecated
  public JvmExportFacadeFactory(int replicate) {
    this(replicate, (TemplateStringRenderer) null, (MinioOptions) null);
  }

  /**
   * Create a new JvmExportFacadeFactory with access to Earth-space.
   *
   * <p>Creates a new export facade factory which adds latitude and longitude to returned records,
   * allowing use of geotiffs and netCDF as export formats.</p>
   *
   * @param replicate The replicate number to use in filenames.
   * @param extents The extents of the grid in the simulation in Earth-space.
   * @param width The width and height of each patch in meters.
   * @param templateRenderer The template renderer for processing export path templates (nullable).
   * @param minioOptions The MinIO configuration options (nullable).
   */
  public JvmExportFacadeFactory(int replicate, PatchBuilderExtents extents, BigDecimal width,
                                TemplateStringRenderer templateRenderer,
                                MinioOptions minioOptions) {
    this.replicate = replicate;
    this.templateRenderer = templateRenderer;
    this.minioOptions = minioOptions;
    this.extents = Optional.of(extents);
    this.width = Optional.of(width);
    MapSerializeStrategy inner = new MapSerializeStrategy();
    serializeStrategy = new MapWithLatLngSerializeStrategy(extents, width, inner);
  }

  /**
   * Create a new JvmExportFacadeFactory with access to Earth-space (legacy constructor).
   *
   * <p>Creates a new export facade factory which adds latitude and longitude to returned records,
   * allowing use of geotiffs and netCDF as export formats.</p>
   *
   * @param replicate The replicate number to use in filenames.
   * @param extents The extents of the grid in the simulation in Earth-space.
   * @param width The width and height of each patch in meters.
   * @deprecated Use constructor with TemplateStringRenderer parameter instead
   */
  @Deprecated
  public JvmExportFacadeFactory(int replicate, PatchBuilderExtents extents, BigDecimal width) {
    this(replicate, extents, width, null, null);
  }

  @Override
  public ExportFacade build(ExportTarget target) {
    return build(target, Optional.empty());
  }

  @Override
  public ExportFacade build(ExportTarget target, Iterable<String> header) {
    return build(target, Optional.of(header));
  }

  @Override
  public ExportFacade build(ExportTarget target, Optional<Iterable<String>> header) {
    return switch (target.getFileType()) {
      case "csv" -> buildForCsv(target, header);
      case "nc" -> buildForNetcdf(target, header);
      case "tif", "tiff" -> buildForGeotiff(target, header);
      default -> throw new IllegalArgumentException("Not supported: " + target.getFileType());
    };
  }

  @Override
  public String getPath(String template) {
    if (templateRenderer != null) {
      // Use new consolidated template processing for both strategy detection and path creation
      TemplateResult result = templateRenderer.renderTemplate(template);
      result.validateForExportType(template); // Validate template for export type
      this.lastTemplateResult = result;
      return result.getProcessedTemplate();
    } else {
      // Fallback to legacy template processing for backward compatibility
      return getPathLegacy(template);
    }
  }

  /**
   * Legacy template processing logic for backward compatibility.
   *
   * <p>This method contains the original template processing logic from before
   * TemplateStringRenderer was introduced. It is kept for backward compatibility
   * when templateRenderer is null.</p>
   *
   * @param template The template string to process
   * @return The processed template string
   */
  private String getPathLegacy(String template) {
    // For GeoTIFF only, preserve replicate template behavior for separate files
    if (template.contains(".tif") || template.contains(".tiff")) {
      String replicateStr = ((Integer) replicate).toString();
      String withReplicate = template.replaceAll("\\{replicate\\}", replicateStr);
      String withStep = withReplicate.replaceAll("\\{step\\}", "__step__");
      String withVariable = withStep.replaceAll("\\{variable\\}", "__variable__");
      return withVariable;
    }

    // For tabular and NetCDF formats, remove replicate template (consolidated files)
    String withStep = template.replaceAll("\\{step\\}", "__step__");
    String withVariable = withStep.replaceAll("\\{variable\\}", "__variable__");
    return withVariable.replaceAll("\\{replicate\\}", "");
  }

  @Override
  public int getReplicateNumber() {
    return replicate;
  }

  /**
   * Determine if information is avialable to translate to Earth longitude and latitude.
   *
   * @returns True if there is enough information to determine Earth-space coordinates or false if
   *     only grid-space is available.
   */
  private boolean hasGeo() {
    return extents.isPresent() && width.isPresent();
  }

  /**
   * Determines if the current export target requires parameterized output (multi-file strategy).
   *
   * <p>Returns true if the last processed template contained {replicate}, indicating that
   * separate files should be created per replicate for memory efficiency.</p>
   *
   * @param target The export target (used for legacy fallback detection)
   * @return True if parameterized output is required
   */
  private boolean requiresParameterizedOutput(ExportTarget target) {
    if (lastTemplateResult != null) {
      return lastTemplateResult.requiresParameterizedOutput();
    } else {
      // Legacy fallback: check if target path contains {replicate}
      return target.getPath().contains("{replicate}");
    }
  }

  /**
   * Creates the appropriate OutputStreamStrategy based on the target protocol.
   *
   * @param target The export target containing protocol and path information
   * @param appendMode If true, append to existing file (only applies to local files)
   * @return OutputStreamStrategy for the specified protocol
   * @throws IllegalArgumentException if protocol is unsupported or MinIO is not configured
   */
  private OutputStreamStrategy createOutputStreamStrategy(ExportTarget target,
                                                           boolean appendMode) {
    String protocol = target.getProtocol();

    if (protocol.isEmpty() || protocol.equals("file")) {
      // Local file system
      return new LocalOutputStreamStrategy(target.getPath(), appendMode);

    } else if (protocol.equals("minio")) {
      // MinIO direct streaming
      if (minioOptions == null || !minioOptions.isMinioOutput()) {
        throw new IllegalArgumentException(
          "MinIO protocol 'minio://" + target.getHost() + target.getPath() + "' "
          + "requires MinIO configuration (--minio-endpoint, --minio-access-key, etc.)"
        );
      }

      String bucketName = target.getHost();
      String objectPath = target.getPath();

      return new MinioOutputStreamStrategy(
        MinioClientSingleton.getInstance(minioOptions),
        bucketName,
        objectPath
      );

    } else {
      throw new IllegalArgumentException("Unsupported protocol: " + protocol);
    }
  }

  /**
   * Creates the appropriate OutputStreamStrategy based on the target protocol.
   *
   * <p>Convenience overload that defaults to non-append mode.</p>
   *
   * @param target The export target containing protocol and path information
   * @return OutputStreamStrategy for the specified protocol
   * @throws IllegalArgumentException if protocol is unsupported or MinIO is not configured
   */
  private OutputStreamStrategy createOutputStreamStrategy(ExportTarget target) {
    return createOutputStreamStrategy(target, false);
  }

  /**
   * Creates an OutputStreamStrategy from a path string by parsing it into an ExportTarget.
   *
   * <p>This method is used by ReplicateOutputStreamGenerator to create strategies for
   * replicate-specific file paths.</p>
   *
   * @param path The full path string (may include protocol like "minio://")
   * @return OutputStreamStrategy for the specified path
   * @throws IllegalArgumentException if path is invalid or protocol is unsupported
   */
  private OutputStreamStrategy createOutputStreamStrategyForPath(String path) {
    ExportTarget target = ExportTargetParser.parse(path);
    return createOutputStreamStrategy(target, false);
  }

  /**
   * Build an ExportFacade that writes to a CSV file.
   *
   * @param target Record describing where the export should be written and format details.
   * @param header An optional list of column headers to include in the CSV. If empty, headers are
   *     not included.
   * @return CsvExportFacade configured to write to the file path specified in the target.
   * @throws IllegalArgumentException if the target is invalid.
   */
  private ExportFacade buildForCsv(ExportTarget target, Optional<Iterable<String>> header) {
    if (requiresParameterizedOutput(target)) {
      return buildParameterizedCsv(target, header);
    } else {
      return buildConsolidatedCsv(target, header);
    }
  }

  /**
   * Build a consolidated CSV export facade (single file with replicate column).
   *
   * @param target The export target configuration
   * @param header Optional header columns for the CSV
   * @return CsvExportFacade for consolidated export
   */
  private ExportFacade buildConsolidatedCsv(ExportTarget target,
                                            Optional<Iterable<String>> header) {
    OutputStreamStrategy outputStreamStrategy = createOutputStreamStrategy(target, true);

    if (header.isPresent()) {
      return new CsvExportFacade(outputStreamStrategy, serializeStrategy, header.get());
    } else {
      return new CsvExportFacade(outputStreamStrategy, serializeStrategy);
    }
  }

  /**
   * Build a parameterized CSV export facade (separate files per replicate).
   *
   * @param target The export target configuration
   * @param header Optional header columns for the CSV
   * @return ParameterizedCsvExportFacade for multi-file export
   */
  private ExportFacade buildParameterizedCsv(ExportTarget target,
                                             Optional<Iterable<String>> header) {
    // Reconstruct the full path with protocol for the template
    String fullPath = reconstructFullPath(target);
    ReplicateOutputStreamGenerator streamGenerator = new ReplicateOutputStreamGenerator(
        fullPath,
        this::createOutputStreamStrategyForPath
    );

    if (header.isPresent()) {
      return new ParameterizedCsvExportFacade(streamGenerator, serializeStrategy, header.get());
    } else {
      return new ParameterizedCsvExportFacade(streamGenerator, serializeStrategy);
    }
  }

  /**
   * Build an ExportFacade that writes to a netCDF file.
   *
   * @param target Record describing where the export should be written and format details.
   * @param header List of variables to be included. This is currently required for netCDF.
   * @return ExportFacade configured to write to the file path specified in the target.
   * @throws IllegalArgumentException if the target is invalid.
   */
  private ExportFacade buildForNetcdf(ExportTarget target, Optional<Iterable<String>> header) {
    if (!hasGeo()) {
      throw new IllegalArgumentException("Writing netCDF requires Earth coordinates.");
    }

    if (header.isEmpty()) {
      throw new IllegalArgumentException("Variable names must be specified for netCDF.");
    }

    if (requiresParameterizedOutput(target)) {
      return buildParameterizedNetcdf(target, header);
    } else {
      return buildConsolidatedNetcdf(target, header);
    }
  }

  /**
   * Build a consolidated NetCDF export facade (single file with replicate dimension).
   *
   * @param target The export target configuration
   * @param header Variable names to include in the NetCDF file
   * @return NetcdfExportFacade for consolidated export
   */
  private ExportFacade buildConsolidatedNetcdf(ExportTarget target,
                                               Optional<Iterable<String>> header) {
    OutputStreamStrategy outputStreamStrategy = createOutputStreamStrategy(target);

    List<String> variablesList = new ArrayList<>();
    header.get().forEach(variablesList::add);
    return new NetcdfExportFacade(
        outputStreamStrategy,
        serializeStrategy,
        variablesList
    );
  }

  /**
   * Build a parameterized NetCDF export facade (separate files per replicate).
   *
   * @param target The export target configuration
   * @param header Variable names to include in each NetCDF file
   * @return ParameterizedNetcdfExportFacade for multi-file export
   */
  private ExportFacade buildParameterizedNetcdf(ExportTarget target,
                                                Optional<Iterable<String>> header) {
    // Reconstruct the full path with protocol for the template
    String fullPath = reconstructFullPath(target);
    ReplicateOutputStreamGenerator streamGenerator = new ReplicateOutputStreamGenerator(
        fullPath,
        this::createOutputStreamStrategyForPath
    );

    List<String> variablesList = new ArrayList<>();
    header.get().forEach(variablesList::add);
    return new ParameterizedNetcdfExportFacade(streamGenerator, serializeStrategy, variablesList);
  }

  private ExportFacade buildForGeotiff(ExportTarget target, Optional<Iterable<String>> header) {
    if (!hasGeo()) {
      throw new IllegalArgumentException("Writing netCDF requires Earth coordinates.");
    }

    if (header.isEmpty()) {
      throw new IllegalArgumentException("Variable names must be specified for geotiff.");
    }

    List<String> variablesList = new ArrayList<>();
    header.get().forEach(variablesList::add);

    return new GeotiffExportFacade(
        (reference) -> getPathForReference(target.getPath(), reference),
        serializeStrategy,
        variablesList,
        extents.orElseThrow(),
        width.orElseThrow()
    );
  }

  /**
   * Replaces placeholders in the given path template with values from the provided StreamReference.
   *
   * @param base The template string containing placeholders to be replaced.
   * @param ref The StreamReference object containing values for step and variable placeholders.
   * @return The formatted string with placeholders replaced by corresponding values.
   */
  private OutputStream getPathForReference(String base, GeotiffExportFacade.StreamReference ref) {
    String step = ref.getStep();
    String variable = ref.getVariable();
    String withStep = base.replaceAll("\\_\\_step\\_\\_", step);
    String path = withStep.replaceAll("\\_\\_variable\\_\\_", variable);

    try {
      return new FileOutputStream(path);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Could not open file for geotiff: " + e);
    }
  }

  /**
   * Reconstructs the full path including protocol from an ExportTarget.
   *
   * <p>This is needed for ReplicateOutputStreamGenerator to properly handle MinIO URLs.</p>
   *
   * @param target The export target to reconstruct the path from
   * @return The full path string including protocol
   */
  private String reconstructFullPath(ExportTarget target) {
    String protocol = target.getProtocol();
    if (protocol.isEmpty()) {
      // No protocol specified, return bare path for local file
      return target.getPath();
    } else if (protocol.equals("file")) {
      // Explicit file:// protocol - reconstruct full URI
      // Note: host should be empty for file:// URIs (file:///path)
      return "file://" + target.getHost() + target.getPath();
    } else {
      // Other protocols (minio, etc.)
      return protocol + "://" + target.getHost() + target.getPath();
    }
  }

}
