package org.joshsim.geo.external;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating appropriate ExternalDataReader instances based on file type.
 */
public class ExternalDataReaderFactory {
  private static final List<ExternalDataReader> readers = new ArrayList<>();

  /**
   * Creates a ExternalDataReader appropriate for the given file path.
   *
   * @param filePath Path to the data file
   * @return A ExternalDataReader instance that can handle the file
   * @throws IOException If no suitable reader is found or there's an error opening the file
   */
  public static ExternalDataReader createReader(String filePath) throws IOException {
    for (ExternalDataReader reader : readers) {
      if (reader.canHandle(filePath)) {
        return reader;
      }
    }

    throw new IOException("No suitable reader found for file: " + filePath);
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
