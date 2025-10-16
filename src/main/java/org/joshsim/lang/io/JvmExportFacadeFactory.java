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

/**
 * Factory implementation for creating ExportFacade instances in a JVM environment.
 */
public class JvmExportFacadeFactory implements ExportFacadeFactory {

  private final int replicate;
  private final MapExportSerializeStrategy serializeStrategy;
  private final Optional<PatchBuilderExtents> extents;
  private final Optional<BigDecimal> width;
  private final TemplateStringRenderer templateRenderer;
  private TemplateResult lastTemplateResult;

  /**
   * Create a new JvmExportFacadeFactory with only grid-space.
   *
   * <p>Creates a new export facade factory which does not try to add latitude and longitude to
   * returned records, disallowing use of geotiffs and netCDF as export formats.</p>
   *
   * @param replicate The replicate number to use in filenames.
   * @param templateRenderer The template renderer for processing export path templates (nullable).
   */
  public JvmExportFacadeFactory(int replicate, TemplateStringRenderer templateRenderer) {
    this.replicate = replicate;
    this.templateRenderer = templateRenderer;
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
    this(replicate, (TemplateStringRenderer) null);
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
   */
  public JvmExportFacadeFactory(int replicate, PatchBuilderExtents extents, BigDecimal width,
                                TemplateStringRenderer templateRenderer) {
    this.replicate = replicate;
    this.templateRenderer = templateRenderer;
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
    this(replicate, extents, width, null);
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
   * Build an ExportFacade that writes to a CSV file.
   *
   * @param target Record describing where the export should be written and format details.
   *     The protocol must be empty, as only the local file system is supported for CSV exports.
   * @param header An optional list of column headers to include in the CSV. If empty, headers are
   *     not included.
   * @return CsvExportFacade configured to write to the file path specified in the target.
   * @throws IllegalArgumentException if the target's protocol is not empty or the target is
   *     invalid.
   */
  private ExportFacade buildForCsv(ExportTarget target, Optional<Iterable<String>> header) {
    if (!target.getProtocol().isEmpty()) {
      String message = "Only local file system is supported for CSV at this time.";
      throw new IllegalArgumentException(message);
    }

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
    String path = target.getPath();
    OutputStreamStrategy outputStreamStrategy = new LocalOutputStreamStrategy(path, true);

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
    String path = target.getPath();
    ReplicateOutputStreamGenerator streamGenerator = new ReplicateOutputStreamGenerator(path);

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
   *     The protocol must be empty, as only the local file system is supported at this time.
   * @param header List of variables to be included. This is currently required for netCDF.
   * @return ExportFacade configured to write to the file path specified in the target.
   * @throws IllegalArgumentException if the target's protocol is not empty or the target is
   *     invalid.
   */
  private ExportFacade buildForNetcdf(ExportTarget target, Optional<Iterable<String>> header) {
    if (!target.getProtocol().isEmpty()) {
      String message = "Only local file system is supported for netcdf at this time.";
      throw new IllegalArgumentException(message);
    }

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
    String path = target.getPath();
    OutputStreamStrategy outputStreamStrategy = new LocalOutputStreamStrategy(path);

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
    String path = target.getPath();
    ReplicateOutputStreamGenerator streamGenerator = new ReplicateOutputStreamGenerator(path);

    List<String> variablesList = new ArrayList<>();
    header.get().forEach(variablesList::add);
    return new ParameterizedNetcdfExportFacade(streamGenerator, serializeStrategy, variablesList);
  }

  private ExportFacade buildForGeotiff(ExportTarget target, Optional<Iterable<String>> header) {
    if (!target.getProtocol().isEmpty()) {
      String message = "Only local file system is supported for netcdf at this time.";
      throw new IllegalArgumentException(message);
    }

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

}
