/**
 * JVM-specific implementation of ExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.geometry.PatchBuilderExtents;


/**
 * Factory implementation for creating ExportFacade instances in a JVM environment.
 */
public class JvmExportFacadeFactory implements ExportFacadeFactory {

  private final ExportSerializeStrategy<Map<String, String>> serializeStrategy;
  
  /**
   * Create a new JvmExportFacadeFactory with only grid-space.
   *
   * <p>Creates a new export facade factory which does not try to add latitude and longitude to
   * returned records, disallowing use of geotiffs and netCDF as export formats.</p>
   */
  public JvmExportFacadeFactory() {
    serializeStrategy = new MapSerializeStrategy();
  }

  /**
   * Create a new JvmExportFacadeFactory with access to Earth-space.
   *
   * <p>Creates a new export facade factory which adds latitude and longitude to returned records,
   * allowing use of geotiffs and netCDF as export formats.</p>
   */
  public JvmExportFacadeFactory(PatchBuilderExtents extents, BigDecimal width) {
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
      default -> throw new IllegalArgumentException("Not supported: " + target.getFileType());
    };
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

    String path = target.getPath();
    OutputStreamStrategy outputStreamStrategy = new LocalOutputStreamStrategy(path);

    if (header.isPresent()) {
      List<String> variablesList = new ArrayList<>();
      header.get().forEach(variablesList::add);
      return new NetcdfExportFacade(
          outputStreamStrategy,
          serializeStrategy,
          variablesList
      );
    } else {
      throw new IllegalArgumentException("Variable names must be specified for netCDF.");
    }
  }
}
