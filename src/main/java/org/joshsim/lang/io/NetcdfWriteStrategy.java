/**
 * Logic to write to a netCDF file.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * ExportWriteStrategy for writing netCDF files.
 *
 * <p>Strategy to write to netCDF files by gathering all records in memory and then, at close,
 * writing to the actual output stream by going through a file indicated by Files'
 * createTempFile.</p>
 */
public class NetcdfWriteStrategy implements ExportWriteStrategy<Map<String, String>> {

  private final List<String> variables;
  
  private OutputStream outputStream;
  private List<Map<String, String>> pendingRecords;

  /**
   * Create a new netCDF write strategy.
   *
   * @param variables List of variable names to write to the netCDF file. All other variables will
   *     be ignored except for position.longitude and position.latitude.
   */
  public NetcdfWriteStrategy(List<String> variables) {
    this.variables = variables;
      pendingRecords = new ArrayList<>();
  }

  /**
   * Add new records to the pending records list.
   *
   * @param record The new record to be added to the pending records list.
   * @param outputStream The stream to which the netCDF file should be written after being
   *     generated, overwritting prior pending output stream.
   */
  @Override
  public void write(Map<String, String> record, OutputStream outputStream) throws IOException {
    this.outputStream = outputStream;
    pendingRecords.add(record);
  }

  @Override
  public void flush() {
    // Ignored
  }

  @Override
  public void close() {
    File tempFile = writeToTempFile();
    redirectFileToStream(tempFile, outputStream);
    pendingRecords = new ArrayList<>();
    outputStream = null;
  }

  /**
   * Write all pending records to a new temporary file.
   *
   * @returns The temporary file where all pending records are written before being redirected to
   *     the output stream.
   */
  private File writeToTempFile() {
    
  }

  /**
   * Send the contents of the tempFile to outputStream.
   *
   * <p>Send the contents of the temporary file where the netCDF was written to the outputStream
   * before deleting that temporary file.</p>
   *
   * @param tempFile The contents where the netCDF file can be found which should be sent to the
   *     output stream. After this operation is complete, this method will delete this temporary
   *     file.
   * @param outputStream The stream to which the netCDF file contents should be written.
   */
  private void redirectFileToStream(File tempFile, OutputSteram outputStream) {
    
  }
}