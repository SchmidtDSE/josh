
package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.netcdf.NetcdfStore;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * Implementation of the ExportWriteStrategy interface for writing netCDF files.
 *
 * <p>Class responsible for writing records with lat/lon coordinates into a netCDF file format.
 * Each variable is written as a separate variable in the netCDF file, with latitude and longitude
 * dimensions.</p>
 */
public class NetcdfWriteStrategy implements ExportWriteStrategy<Map<String, String>> {

  private final List<String> variableNames;
  private NetcdfFileWriter writer;
  private List<BigDecimal> lats;
  private List<BigDecimal> lons;
  private List<Map<String, String>> records;
  private boolean isInitialized;

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
      writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, output.toString());
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
    try {
      if (writer != null && !records.isEmpty()) {
        // Create dimensions
        Dimension latDim = writer.addDimension(null, "latitude", lats.size());
        Dimension lonDim = writer.addDimension(null, "longitude", lons.size());
        List<Dimension> dims = List.of(latDim, lonDim);

        // Create coordinate variables
        Variable latVar = writer.addVariable(null, "latitude", DataType.DOUBLE, List.of(latDim));
        Variable lonVar = writer.addVariable(null, "longitude", DataType.DOUBLE, List.of(lonDim));
        
        // Add standard attributes
        latVar.addAttribute(new ucar.nc2.Attribute("units", "degrees_north"));
        lonVar.addAttribute(new ucar.nc2.Attribute("units", "degrees_east"));

        // Create variables for each data field
        for (String varName : variableNames) {
          writer.addVariable(null, varName, DataType.DOUBLE, dims);
        }

        writer.create();

        // Write coordinate data
        Array latData = Array.factory(DataType.DOUBLE, new int[]{lats.size()});
        Array lonData = Array.factory(DataType.DOUBLE, new int[]{lons.size()});
        
        for (int i = 0; i < lats.size(); i++) {
          latData.setDouble(i, lats.get(i).doubleValue());
        }
        for (int i = 0; i < lons.size(); i++) {
          lonData.setDouble(i, lons.get(i).doubleValue());
        }

        writer.write(latVar, latData);
        writer.write(lonVar, lonData);

        // Write variable data
        for (String varName : variableNames) {
          Variable var = writer.findVariable(varName);
          Array data = Array.factory(DataType.DOUBLE, new int[]{lats.size(), lons.size()});
          
          for (Map<String, String> record : records) {
            BigDecimal lat = new BigDecimal(record.get("position.latitude"));
            BigDecimal lon = new BigDecimal(record.get("position.longitude"));
            int latIndex = lats.indexOf(lat);
            int lonIndex = lons.indexOf(lon);
            
            if (record.containsKey(varName)) {
              data.setDouble(latIndex * lons.size() + lonIndex, 
                  Double.parseDouble(record.get(varName)));
            }
          }
          
          writer.write(var, data);
        }

        writer.close();
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to write netCDF file", e);
    }
  }
}
