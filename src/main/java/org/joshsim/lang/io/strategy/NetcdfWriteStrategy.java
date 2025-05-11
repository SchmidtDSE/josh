/**
 * Logic to write to a netCDF file.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * ExportWriteStrategy for writing netCDF files.
 *
 * <p>Strategy to write to netCDF files by gathering all records in memory and then, at close,
 * writing to the actual output stream by going through a file indicated by Files'
 * createTempFile.</p>
 */
public class NetcdfWriteStrategy extends PendingRecordWriteStrategy {

  private final List<String> variables;

  /**
   * Create a new netCDF write strategy.
   *
   * @param variables List of variable names to write to the netCDF file. All other variables will
   *     be ignored except for position.longitude and position.latitude.
   */
  public NetcdfWriteStrategy(List<String> variables) {
    super();
    this.variables = variables;
  }

  @Override
  protected List<String> getRequiredVariables() {
    List<String> variablesRequired = new ArrayList<>();
    variablesRequired.addAll(variables);
    variablesRequired.add("position.longitude");
    variablesRequired.add("position.latitude");
    variablesRequired.add("step");
    return variablesRequired;
  }

  /**
   * Write the pending results to a netCDF file.
   *
   * <p>Write the pending results to a netCDF file where all values will be converted to double
   * except for step which will be a long.</p>
   *
   * @param records The records to be written where each element is a record with one or more
   *     variables to be written. Importantly, all records will have position.longitude and
   *     position.latitude in degrees as well a step which is an integer referring to the simulation
   *     step count when the record snapshot was taken.
   * @param outputStream The stream to whcih the netCDF file will be written.
   */
  @Override
  public void writeAll(List<Map<String, String>> records, OutputStream outputStream) {
    
  }
}
