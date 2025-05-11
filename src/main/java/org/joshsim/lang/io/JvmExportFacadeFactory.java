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


/**
 * Factory implementation for creating ExportFacade instances in a JVM environment.
 */
public class JvmExportFacadeFactory implements ExportFacadeFactory {

  private final int replicate;
  private final MapExportSerializeStrategy serializeStrategy;
  private final Optional<PatchBuilderExtents> extents;
  private final Optional<BigDecimal> width;

  /**
   * Create a new JvmExportFacadeFactory with only grid-space.
   *
   * <p>Creates a new export facade factory which does not try to add latitude and longitude to
   * returned records, disallowing use of geotiffs and netCDF as export formats.</p>
   *
   * @param replicate The replicate number to use in filenames.
   */
  public JvmExportFacadeFactory(int replicate) {
    this.replicate = replicate;
    serializeStrategy = new MapSerializeStrategy();
    extents = Optional.empty();
    width = Optional.empty();
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
   */
  public JvmExportFacadeFactory(int replicate, PatchBuilderExtents extents, BigDecimal width) {
    this.replicate = replicate;
    this.extents = Optional.of(extents);
    this.width = Optional.of(width);
    MapSerializeStrategy inner = new MapSerializeStrategy();
    serializeStrategy = new MapWithLatLngSerializeStrategy(extents, width, inner);
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
    String replicateStr = ((Integer) replicate).toString();
    return template.replaceAll("\\{replicate\\}", replicateStr);
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

    String path = target.getPath();
    OutputStreamStrategy outputStreamStrategy = new LocalOutputStreamStrategy(path);

    if (header.isPresent()) {
      return new CsvExportFacade(outputStreamStrategy, serializeStrategy, header.get());
    } else {
      return new CsvExportFacade(outputStreamStrategy, serializeStrategy);
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
    String withStep = base.replaceAll("\\{step\\}", step);
    String path = withStep.replaceAll("\\{variable\\}", variable);

    try {
      return new FileOutputStream(path);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Could not open file for geotiff: " + e);
    }
  }

}
