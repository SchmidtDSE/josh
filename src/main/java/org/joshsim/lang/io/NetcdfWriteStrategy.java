package org.joshsim.lang.io;

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
 * Implementation of the ExportWriteStrategy interface for writing netCDF files.
 *
 * <p>Class responsible for writing records with lat/lon coordinates into a netCDF file format.
 * Each variable is written as a separate variable in the netCDF file, with latitude and longitude
 * dimensions.</p>
 */
public class NetcdfWriteStrategy implements ExportWriteStrategy<Map<String, String>> {

  private final List<String> variableNames;
  private NetcdfFormatWriter.Builder builder;
  private List<BigDecimal> lats;
  private List<BigDecimal> lons;
  private List<Map<String, String>> records;
  private boolean isInitialized;
  private OutputStream currentOutput;

  /**
   * Create a new netCDF write strategy.
   *
   * @param variableNames List of variable names to write to the netCDF file
   */
  public NetcdfWriteStrategy(List<String> variableNames) {
    this.variableNames = variableNames;
    this.lats = new ArrayList<>();
    this.lons = new ArrayList<>();
    this.records = new ArrayList<>();
    this.isInitialized = false;
  }

  @Override
  public void write(Map<String, String> record, OutputStream output) throws IOException {
    if (!isInitialized) {
      this.currentOutput = output;
      this.builder = NetcdfFormatWriter.createNewNetcdf3("test.nc");
      isInitialized = true;
    }

    // Store coordinates
    BigDecimal lat = new BigDecimal(record.get("position.latitude"));
    BigDecimal lon = new BigDecimal(record.get("position.longitude"));

    if (!lats.contains(lat)) {
      lats.add(lat);
    }
    if (!lons.contains(lon)) {
      lons.add(lon);
    }

    records.add(record);
  }

  @Override
  public void flush() {
    // No intermediate flushing needed
  }

  @Override
  public void close() {
    try {
      if (builder != null && !records.isEmpty()) {
        // Create dimensions
        Dimension latDim = builder.addDimension("latitude", lats.size());
        Dimension lonDim = builder.addDimension("longitude", lons.size());
        List<Dimension> dims = List.of(latDim, lonDim);

        // Create coordinate variables
        Variable.Builder<?> latVar = builder.addVariable("latitude", DataType.DOUBLE, List.of(latDim));
        Variable.Builder<?> lonVar = builder.addVariable("longitude", DataType.DOUBLE, List.of(lonDim));

        // Add standard attributes
        latVar.addAttribute(new Attribute("units", "degrees_north"));
        lonVar.addAttribute(new Attribute("units", "degrees_east"));

        // Create variables for each data field
        for (String varName : variableNames) {
          builder.addVariable(varName, DataType.DOUBLE, dims);
        }

        // Build the writer and write to the output stream
        try (NetcdfFormatWriter writer = builder.build()) {
          // Write coordinate data
          double[] latArray = new double[lats.size()];
          double[] lonArray = new double[lons.size()];

          for (int i = 0; i < lats.size(); i++) {
            latArray[i] = lats.get(i).doubleValue();
          }
          for (int i = 0; i < lons.size(); i++) {
            lonArray[i] = lons.get(i).doubleValue();
          }

          writer.write("latitude", Array.factory(DataType.DOUBLE, new int[]{lats.size()}, latArray));
          writer.write("longitude", Array.factory(DataType.DOUBLE, new int[]{lons.size()}, lonArray));

          // Write variable data
          for (String varName : variableNames) {
            double[] data = new double[lats.size() * lons.size()];

            for (Map<String, String> record : records) {
              BigDecimal lat = new BigDecimal(record.get("position.latitude"));
              BigDecimal lon = new BigDecimal(record.get("position.longitude"));
              int latIndex = lats.indexOf(lat);
              int lonIndex = lons.indexOf(lon);

              if (record.containsKey(varName)) {
                data[latIndex * lons.size() + lonIndex] = Double.parseDouble(record.get(varName));
              }
            }

            writer.write(varName, Array.factory(DataType.DOUBLE, new int[]{lats.size(), lons.size()}, data));
          }
          writer.close();
        } catch (IOException | InvalidRangeException e) {
            throw new RuntimeException(e);
        }

        // Copy the temporary file to the output stream
        try {
          Files.copy(Paths.get("test.nc"), currentOutput);
          // Clean up temporary file
          Files.delete(Paths.get("test.nc"));
        } catch (IOException e) {
          throw new RuntimeException("Failed to copy or clean up temporary file", e);
        }
      }
    }  finally {
       lats.clear();
       lons.clear();
       records.clear();
       isInitialized = false;
    }
  }
}