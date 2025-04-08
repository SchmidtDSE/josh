package org.joshsim.engine.external.cog;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.coverage.grid.GridCoverage;
import org.joshsim.engine.external.core.ExternalLayer;
import org.joshsim.engine.external.core.Request;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.joshsim.engine.value.type.DecimalScalar;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.RealizedDistribution;

/**
 * Concrete implementation of ExternalLayer that uses CogReader to read COG files.
 */
public class CogExternalLayer implements ExternalLayer {
  private final EngineValueCaster caster;
  private final Units units;

  /**
   * Creates a new COG external layer.
   *
   * @param units the units to use for the values extracted from COG
   */
  public CogExternalLayer(Units units, EngineValueCaster caster) {
    this.units = units;
    this.caster = caster;
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
      EngineGeometry geometry = request.getGeometry().orElseThrow();
      GridCoverage gridCoverage = CogReader.getCoverageFromDisk(request.getPath(), geometry);
      List<BigDecimal> decimalValuesWithinGeometry =
          CogReader.extractValuesFromCoverage(gridCoverage, geometry);

      RealizedDistribution realizedDistribution = RealizedDistribution.fromDecimalValues(
          caster,
          decimalValuesWithinGeometry,
          units
      );

      return realizedDistribution;

    } catch (IOException e) {
      throw new RuntimeException("Failed to read COG file: " + request.getPath(), e);
    }
  }
}
