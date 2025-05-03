package org.joshsim.geo.external;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.readers.NetcdfExternalDataReader;
import org.joshsim.geo.external.readers.NetcdfExternalDataReaderFactory;

/**
 * Factory for creating appropriate ExternalDataReader instances based on file type.
 */
public class ExternalDataReaderFactory {
  private static final List<ExternalDataReader> readers = new ArrayList<>();
  
  // Specialized reader factories
  private static final NetcdfExternalDataReaderFactory netcdfReaderFactory = 
      new NetcdfExternalDataReaderFactory(new EngineValueFactory());
  
  // Future factories will be added here
  // private static final CogReaderFactory cogReaderFactory = new CogReaderFactory();
  // private static final ZarrReaderFactory zarrReaderFactory = new ZarrReaderFactory();

  /**
   * Creates a ExternalDataReader appropriate for the given file path.
   *
   * @param filePath Path to the data file
   * @return A ExternalDataReader instance that can handle the file
   * @throws IOException If no suitable reader is found or there's an error opening the file
   */
  public static ExternalDataReader createReader(String filePath) throws IOException {
    if (isNetCdfFile(filePath)) {
      return netcdfReaderFactory.createReader();
    }
    
    if (isCogFile(filePath)) {
      throw new UnsupportedOperationException("COG reader not implemented yet");
    }

    if (isZarrFile(filePath)) {
      throw new UnsupportedOperationException("Zarr reader not implemented yet");
    }
    
    throw new IOException("No suitable reader found for file: " + filePath);
  }

  /**
   * Checks if the file is a NetCDF format file based on extension.
   */
  private static boolean isNetCdfFile(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return false;
    }

    String lowerPath = filePath.toLowerCase();
    return lowerPath.endsWith(".nc")
           || lowerPath.endsWith(".ncf")
           || lowerPath.endsWith(".netcdf")
           || lowerPath.endsWith(".nc4");
  }

  /**
   * Checks if the file is a Cloud Optimized GeoTIFF (COG) format file based on extension. Note
   * that this is a simple check that checks only the file extension. A more robust check would
   * validate that the file is indeed a COG (and not a naive tiff) by reading the file.
   */
  private static boolean isCogFile(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return false;
    }
    String lowerPath = filePath.toLowerCase();
    return lowerPath.endsWith(".tif")
           || lowerPath.endsWith(".tiff");
  }

  /**
   * Checks if the file is a Zarr format file based on extension.
   */
  private static boolean isZarrFile(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return false;
    }
    String lowerPath = filePath.toLowerCase();
    return lowerPath.endsWith(".zarr");
  }


  /**
   * Registers a custom reader implementation.
   *
   * @param reader The reader to register
   */
  public static void registerReader(ExternalDataReader reader) {
    readers.add(0, reader); // Add at beginning for higher priority
  }

  // Private constructor to prevent instantiation
  private ExternalDataReaderFactory() {
    throw new AssertionError(
        "ExternalDataReaderFactory is a utility class and should not be instantiated");
  }
}