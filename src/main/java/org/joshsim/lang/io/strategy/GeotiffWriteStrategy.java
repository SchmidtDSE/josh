/**
 * Strategy to write to an individual geotiff.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;


/**
 * Strategy to write a single geotiff.
 *
 * <p>Strategy to write a single band geotiff where there is one geotiff per timestep, variable,
 * and replicate combination. There is one GeotiffWriteStrategy instantiated per output geotiff.
 * This is written via Apache SIS and will convert position.latitude, position.longitude, and
 * the variable specified to doubles from Strings before writing.</p>
 */
public class GeotiffWriteStrategy extends PendingRecordWriteStrategy {

  private final String variable;

  /**
   * Constructs a GeotiffWriteStrategy with the specified variable.
   *
   * @param variable The variable to be written within this strategy.
   */
  public GeotiffWriteStrategy(String variable) {
    this.variable = variable;
  }

  @Override
  protected void writeAll(List<Map<String, String>> records, OutputStream outputStream) {

  }

  @Override
  protected List<String> getRequiredVariables() {
    return List.of("position.latitude", "position.longitude", variable);
  }
}
