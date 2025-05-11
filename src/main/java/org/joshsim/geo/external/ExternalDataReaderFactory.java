package org.joshsim.geo.external;

import java.io.IOException;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.readers.GeotiffExternalDataReaderFactory;
import org.joshsim.geo.external.readers.NetcdfExternalDataReaderFactory;

/**
 * Factory for creating appropriate ExternalDataReader instances based on file type.
 */
public class ExternalDataReaderFactory {

  // Specialized reader factories
  private static final NetcdfExternalDataReaderFactory netcdfReaderFactory =
      new NetcdfExternalDataReaderFactory(new EngineValueFactory());
  private static final GeotiffExternalDataReaderFactory geotiffReaderFactory =
      new GeotiffExternalDataReaderFactory(new EngineValueFactory());
  private static final CsvExternalDataReaderFactory csvReaderFactory =
      new CsvExternalDataReaderFactory(new EngineValueFactory());

  /**
   * Creates a ExternalDataReader appropriate for the given file path.
   *
   * @param filePath Path to the data file
   * @return A ExternalDataReader instance that can handle the file
   * @throws IOException If no suitable reader is found or there's an error opening the file
   */
  public static ExternalDataReader createReader(
        String filePath
  ) throws IOException {
    if (isNetCdfFile(filePath)) {
      return netcdfReaderFactory.createReader();
    }

    if (isGeotiffFile(filePath)) {
      return geotiffReaderFactory.createReader();
    }

    if (isZarrFile(filePath)) {
      throw new UnsupportedOperationException("Zarr reader not implemented yet");
    }

    if (isCsvFile(filePath)) {
      return csvReaderFactory.createReader();
    }

    throw new IOException("No suitable reader found for file: " + filePath);
  }

  /**
   * Checks if the file is a CSV format file based on extension.
   */
  private static boolean isCsvFile(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return false;
    }
    return filePath.toLowerCase().endsWith(".csv");
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
  private static boolean isGeotiffFile(String filePath) {
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

  // Private constructor to prevent instantiation
  private ExternalDataReaderFactory() {
    throw new AssertionError(
        "ExternalDataReaderFactory is a utility class and should not be instantiated");
  }
}
