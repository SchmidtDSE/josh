/**
 * Logic to write to a netCDF file.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;


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
    try {
      // Create a temporary file.
      File tempFile = File.createTempFile("netcdf-temp", ".nc");
      NetcdfFileWriter writer = NetcdfFileWriter.createNew(tempFile.getAbsolutePath(), false);

      // Define dimensions
      Dimension timeDim = writer.addDimension("time", records.size());
      Dimension latDim = writer.addDimension("latitude", 1); // Assuming single latitude/longitude per record
      Dimension lonDim = writer.addDimension("longitude", 1); // Assuming single latitude/longitude per record

      // Define variables
      Variable latitude = writer.addVariable("latitude", DataType.DOUBLE, Arrays.asList(timeDim, latDim));
      latitude.addAttribute(new Attribute("units", "degrees_north"));

      Variable longitude = writer.addVariable("longitude", DataType.DOUBLE, Arrays.asList(timeDim, lonDim));
      longitude.addAttribute(new Attribute("units", "degrees_east"));

      Variable step = writer.addVariable("step", DataType.LONG, Collections.singletonList(timeDim));

      List<Variable> dataVariables = new ArrayList<>();
      Map<String, Variable> variableMap = new HashMap<>();

      for (String variableName : variables) {
        Variable variable = writer.addVariable(variableName, DataType.DOUBLE, Collections.singletonList(timeDim));
        dataVariables.add(variable);
        variableMap.put(variableName, variable);
      }


      writer.addGlobalAttribute("Conventions", "CF-1.6");
      writer.create();


      // Write data
      Array latitudeData = Array.factory(DataType.DOUBLE, new int[]{records.size(), 1});
      Array longitudeData = Array.factory(DataType.DOUBLE, new int[]{records.size(), 1});
      Array stepData = Array.factory(DataType.LONG, new int[]{records.size()});
      Map<String, Array> dataArrays = new HashMap<>();

      for (String variableName : variables) {
        dataArrays.put(variableName, Array.factory(DataType.DOUBLE, new int[]{records.size()}));
      }


      Index latIndex = latitudeData.getIndex();
      Index lonIndex = longitudeData.getIndex();
      Index stepIndex = stepData.getIndex();

      Map<String, Index> dataIndices = new HashMap<>();
      for (String variableName : variables) {
        dataIndices.put(variableName, dataArrays.get(variableName).getIndex());
      }


      for (int i = 0; i < records.size(); i++) {
        Map<String, String> record = records.get(i);

        double lat = Double.parseDouble(record.get("position.latitude"));
        double lon = Double.parseDouble(record.get("position.longitude"));
        long stepValue = Long.parseLong(record.get("step"));

        latitudeData.setDouble(latIndex.set(i, 0), lat);
        longitudeData.setDouble(lonIndex.set(i, 0), lon);
        stepData.setLong(stepIndex.set(i), stepValue);


        for (String variableName : variables) {
          Array dataArray = dataArrays.get(variableName);
          Index dataIndex = dataIndices.get(variableName);

          double value = Double.parseDouble(record.get(variableName));
          dataArray.setDouble(dataIndex.set(i), value);
        }

      }

      writer.write(latitude, latitudeData);
      writer.write(longitude, longitudeData);
      writer.write(step, stepData);

      for (String variableName : variables) {
        writer.write(variableMap.get(variableName), dataArrays.get(variableName));
      }

      // Close the writer
      writer.close();

      // Write the temporary file to the output stream
      Files.copy(tempFile.toPath(), outputStream);

      // Delete the temporary file
      tempFile.delete();

    } catch (IOException e) {
      throw new RuntimeException("Error writing to netCDF file", e);
    }

  }
}