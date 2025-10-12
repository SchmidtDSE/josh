/**
 * Reused structures for a write strategy which gathers records and writes at once.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Strategy method which gathers records and writes all records at the same time.
 */
public abstract class PendingRecordWriteStrategy implements StringMapWriteStrategy {

  private Optional<List<String>> variables;
  private OutputStream outputStream;
  private List<Map<String, String>> pendingRecords;

  /**
   * Create a new strategy method for writing all records at once.
   */
  public PendingRecordWriteStrategy() {
    this.variables = Optional.empty();
    pendingRecords = new ArrayList<>();
  }

  /**
   * Add new records to the pending records list.
   *
   * @param record The new record to be added to the pending records list.
   * @param outputStream The stream to which the netCDF file should be written after being
   *     generated, overwriting prior pending output stream.
   */
  @Override
  public void write(Map<String, String> record, OutputStream outputStream) throws IOException {
    if (variables.isEmpty()) {
      variables = Optional.of(getRequiredVariables());
    }

    this.outputStream = outputStream;
    pendingRecords.add(record);
  }

  @Override
  public void flush() {
    // Ignored
  }

  @Override
  public void close() {
    writeAll(pendingRecords, outputStream);
    pendingRecords = new ArrayList<>();
    outputStream = null;
  }

  /**
   * Write all records.
   *
   * @param records The records to be written.
   * @param outputStream The stream to which they should be written.
   */
  protected abstract void writeAll(List<Map<String, String>> records, OutputStream outputStream);

  /**
   * Identifies and retrieves the list of variables that are required for the strategy.
   *
   * @return A list of variable names that are essential for processing within the strategy.
   */
  protected abstract List<String> getRequiredVariables();

  /**
   * Check that a variable name is present on a record.
   *
   * @param record The record in which to check for the variable.
   * @param varName The name of the variable to check for.
   */
  private void checkPresent(Map<String, String> record, String varName) {
    if (!record.containsKey(varName)) {
      throw new RuntimeException("Record does not contain variable " + varName);
    }
  }
}
