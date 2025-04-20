package org.joshsim.geo.external.core;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.geotools.coverage.grid.GridCoverage2D;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.joshsim.geo.geometry.EarthGeometry;

/**
 * Implementation of ExternalLayer that uses GridCoverageReader to read files.
 */
public class GridCoverageExternalLayer implements ExternalLayer {
  private final EngineValueCaster caster;
  private final Units units;
  protected final GridCoverageReader reader;

  /**
   * Creates a new grid coverage external layer.
   *
   * @param units the units to use for the values extracted
   * @param caster the caster for converting values
   * @param reader the reader to use for accessing grid coverage data
   */
  public GridCoverageExternalLayer(
        Units units,
        EngineValueCaster caster,
        GridCoverageReader reader
  ) {
    this.units = units;
    this.caster = caster;
    this.reader = reader;
  }

  @Override
  public Units getUnits() {
    return units;
  }

  @Override
  public EngineValueCaster getCaster() {
    return caster;
  }

  @Override
  public RealizedDistribution fulfill(Request request) {
    try {
      EarthGeometry geometry = request.getGeometry().orElseThrow().getOnEarth();
      GridCoverage2D gridCoverage = reader.getCoverageFromIo(request.getPath(), geometry);
      List<BigDecimal> decimalValuesWithinGeometry =
          reader.extractValuesFromCoverage(gridCoverage, geometry);

      RealizedDistribution realizedDistribution = RealizedDistribution.fromDecimalValues(
          getCaster(),
          decimalValuesWithinGeometry,
          getUnits()
      );

      return realizedDistribution;
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file: " + request.getPath(), e);
    }
  }
}
