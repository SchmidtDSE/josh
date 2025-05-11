/**
 * Logic to write to a netCDF file.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.write.NetcdfFileFormat;
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
   *     generated, overwriting prior pending output stream.
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
   * @return The temporary file where all pending records are written before being redirected to
   *     the output stream.
   */
  private File writeToTempFile() {
    try {
      File tempFile = File.createTempFile("netcdf", ".nc");
      System.out.println("Temp file created at: " + tempFile.getAbsolutePath());
      System.out.println("Temp file exists: " + tempFile.exists());
      System.out.println("Temp file can write: " + tempFile.canWrite());
      tempFile.deleteOnExit();

      // Create NetCDF writer with the temporary file
      NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf4(
          NetcdfFileFormat.NETCDF4,
          tempFile.getAbsolutePath(),
          Nc4ChunkingStrategy.factory(Nc4Chunking.Strategy.standard, 0, true)
      );

      // Add dimensions
      int numRecords = pendingRecords.size();
      Dimension timeDim = Dimension.builder()
          .setName("time")
          .setLength(numRecords)
          .build();
      builder.addDimension(timeDim);

      // Add variables including time, latitude, and longitude
      builder.addVariable("time", DataType.DOUBLE, "time");
      builder.addVariable("latitude", DataType.DOUBLE, "time");
      builder.addVariable("longitude", DataType.DOUBLE, "time");
      
      for (String varName : variables) {
        Variable.Builder<?> varBuilder = Variable.builder()
            .setName(varName)
            .setDataType(DataType.DOUBLE);
        builder.addVariable(varName, DataType.DOUBLE, "time");
      }

      // Build and get the writer
      try (NetcdfFormatWriter writer = builder.build()) {
        System.out.println("Writer created successfully");
        // Write time data
        Array timeData = Array.factory(DataType.DOUBLE, new int[]{numRecords});
        double[] timeArray = (double[]) timeData.get1DJavaArray(DataType.DOUBLE);
        
        // Write latitude data
        Array latData = Array.factory(DataType.DOUBLE, new int[]{numRecords});
        double[] latArray = (double[]) latData.get1DJavaArray(DataType.DOUBLE);
        
        // Write longitude data
        Array lonData = Array.factory(DataType.DOUBLE, new int[]{numRecords});
        double[] lonArray = (double[]) lonData.get1DJavaArray(DataType.DOUBLE);
        
        // Fill coordinate and time arrays
        int index = 0;
        for (Map<String, String> record : pendingRecords) {
          timeArray[index] = Double.parseDouble(record.getOrDefault("step", "0.0"));
          latArray[index] = Double.parseDouble(record.getOrDefault("position.latitude", "0.0"));
          lonArray[index] = Double.parseDouble(record.getOrDefault("position.longitude", "0.0"));
          index++;
        }
        
        writer.write("time", timeData);
        writer.write("latitude", latData);
        writer.write("longitude", lonData);

        // Write data for each variable
        for (String varName : variables) {
          Array data = Array.factory(DataType.DOUBLE, new int[]{numRecords});
          double[] dataArray = (double[]) data.get1DJavaArray(DataType.DOUBLE);
          index = 0;
          for (Map<String, String> record : pendingRecords) {
            String value = record.getOrDefault(varName, "0.0");
            try {
              dataArray[index] = Double.parseDouble(value);
            } catch (NumberFormatException e) {
              dataArray[index] = 0.0;
            }
            index++;
          }
          writer.write(varName, data);
        }
      }

      return tempFile;
    } catch (IOException | InvalidRangeException e) {
      System.err.println("Exception details: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Failed to write NetCDF file", e);
    }
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
  private void redirectFileToStream(File tempFile, OutputStream outputStream) {
    try {
      byte[] buffer = Files.readAllBytes(Paths.get(tempFile.getAbsolutePath()));
      outputStream.write(buffer);
      outputStream.flush();
      tempFile.delete();
    } catch (IOException e) {
      throw new RuntimeException("Failed to write NetCDF data to output stream", e);
    }
  }
}