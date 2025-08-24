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
import java.util.LinkedHashMap;
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
    variablesRequired.add("replicate");
    return variablesRequired;
  }

  @Override
  public void writeAll(List<Map<String, String>> records, OutputStream outputStream) {
    Map<Integer, List<Map<String, String>>> recordsByReplicate = groupRecordsByReplicate(records);
    File tempFile = writeToTempFile(recordsByReplicate);
    redirectFileToStream(tempFile, outputStream);
  }

  /**
   * Groups records by their replicate number.
   *
   * @param records The list of records to group by replicate.
   * @return A map from replicate number to list of records for that replicate.
   */
  private Map<Integer, List<Map<String, String>>> groupRecordsByReplicate(
        List<Map<String, String>> records) {
    Map<Integer, List<Map<String, String>>> recordsByReplicate = new LinkedHashMap<>();
    
    for (Map<String, String> record : records) {
      String replicateStr = record.getOrDefault("replicate", "0");
      int replicate;
      try {
        replicate = Integer.parseInt(replicateStr);
      } catch (NumberFormatException e) {
        replicate = 0;
      }
      
      recordsByReplicate.computeIfAbsent(replicate, k -> new ArrayList<>()).add(record);
    }
    
    return recordsByReplicate;
  }

  /**
   * Write all pending records to a new temporary file.
   *
   * @param recordsByReplicate The records grouped by replicate number.
   * @return The temporary file where all pending records are written before being redirected to
   *     the output stream.
   */
  private File writeToTempFile(Map<Integer, List<Map<String, String>>> recordsByReplicate) {
    try {
      File tempFile = File.createTempFile("netcdf", ".nc");
      tempFile.deleteOnExit();

      // Create NetCDF writer with the temporary file
      NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf4(
          NetcdfFileFormat.NETCDF4,
          tempFile.getAbsolutePath(),
          Nc4ChunkingStrategy.factory(Nc4Chunking.Strategy.standard, 0, true)
      );

      // Determine dimensions
      int numReplicates = recordsByReplicate.size();
      int maxTimeSteps = recordsByReplicate.values().stream()
          .mapToInt(List::size)
          .max()
          .orElse(0);

      // Handle empty records case - ensure at least 1 for dimension
      if (numReplicates == 0) {
        numReplicates = 1;
      }
      if (maxTimeSteps == 0) {
        maxTimeSteps = 1;
      }

      // Add dimensions - replicate first, then time
      Dimension replicateDim = Dimension.builder()
          .setName("replicate")
          .setLength(numReplicates)
          .build();
      builder.addDimension(replicateDim);
      
      Dimension timeDim = Dimension.builder()
          .setName("time")
          .setLength(maxTimeSteps)
          .build();
      builder.addDimension(timeDim);

      // Add variables with 2D dimensions (replicate, time)
      builder.addVariable("time", DataType.DOUBLE, "replicate time");
      builder.addVariable("latitude", DataType.DOUBLE, "replicate time");
      builder.addVariable("longitude", DataType.DOUBLE, "replicate time");

      for (String varName : variables) {
        builder.addVariable(varName, DataType.DOUBLE, "replicate time");
      }

      // Build and get the writer
      try (NetcdfFormatWriter writer = builder.build()) {
        // Create 2D arrays for all variables
        int[] shape = new int[]{numReplicates, maxTimeSteps};
        
        Array timeData = Array.factory(DataType.DOUBLE, shape);
        double[][] timeArray = (double[][]) timeData.copyToNDJavaArray();
        
        Array latData = Array.factory(DataType.DOUBLE, shape);
        double[][] latArray = (double[][]) latData.copyToNDJavaArray();
        
        Array lonData = Array.factory(DataType.DOUBLE, shape);
        double[][] lonArray = (double[][]) lonData.copyToNDJavaArray();

        // Fill coordinate and time arrays by replicate
        int replicateIndex = 0;
        for (Map.Entry<Integer, List<Map<String, String>>> entry 
                : recordsByReplicate.entrySet()) {
          List<Map<String, String>> records = entry.getValue();
          
          for (int timeIndex = 0; timeIndex < records.size(); timeIndex++) {
            Map<String, String> record = records.get(timeIndex);
            timeArray[replicateIndex][timeIndex] = Double.parseDouble(
                record.getOrDefault("step", "0.0"));
            latArray[replicateIndex][timeIndex] = Double.parseDouble(
                record.getOrDefault("position.latitude", "0.0"));
            lonArray[replicateIndex][timeIndex] = Double.parseDouble(
                record.getOrDefault("position.longitude", "0.0"));
          }
          
          // Fill remaining time steps with NaN for shorter replicates
          for (int timeIndex = records.size(); timeIndex < maxTimeSteps; timeIndex++) {
            timeArray[replicateIndex][timeIndex] = Double.NaN;
            latArray[replicateIndex][timeIndex] = Double.NaN;
            lonArray[replicateIndex][timeIndex] = Double.NaN;
          }
          
          replicateIndex++;
        }

        writer.write("time", timeData);
        writer.write("latitude", latData);
        writer.write("longitude", lonData);

        // Write data for each variable
        for (String varName : variables) {
          Array data = Array.factory(DataType.DOUBLE, shape);
          double[][] dataArray = (double[][]) data.copyToNDJavaArray();
          
          replicateIndex = 0;
          for (Map.Entry<Integer, List<Map<String, String>>> entry 
                : recordsByReplicate.entrySet()) {
            List<Map<String, String>> records = entry.getValue();
            
            for (int timeIndex = 0; timeIndex < records.size(); timeIndex++) {
              Map<String, String> record = records.get(timeIndex);
              String value = record.getOrDefault(varName, "0.0");
              try {
                dataArray[replicateIndex][timeIndex] = Double.parseDouble(value);
              } catch (NumberFormatException e) {
                dataArray[replicateIndex][timeIndex] = 0.0;
              }
            }
            
            // Fill remaining time steps with NaN for shorter replicates
            for (int timeIndex = records.size(); timeIndex < maxTimeSteps; timeIndex++) {
              dataArray[replicateIndex][timeIndex] = Double.NaN;
            }
            
            replicateIndex++;
          }
          
          writer.write(varName, data);
        }
      }

      return tempFile;
    } catch (IOException | InvalidRangeException e) {
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
