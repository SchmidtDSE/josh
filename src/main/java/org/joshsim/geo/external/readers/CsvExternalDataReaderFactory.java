/**
 * Logic to instantiate utilities to read external data from CSV.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.geo.external.readers;

import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.ExternalDataReader;


/**
 * A factory for building utilities to read external data from CSV files.
 *
 * <p>Factory class for creating instances of CsvExternalDataReader. CsvExternalDataReader is a
 * specific implementation of the ExternalDataReader interface, designed to handle CSV data
 * files.</p>
 */
public class CsvExternalDataReaderFactory {
  private final EngineValueFactory valueFactory;

  /**
   * Constructs a new CsvExternalDataReaderFactory instance.
   *
   * @param valueFactory the EngineValueFactory used to create EngineValue instances
   *     for data manipulation within the CsvExternalDataReader.
   */
  public CsvExternalDataReaderFactory(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  /**
   * Creates a new instance of a CSV-based external data reader.
   *
   * @return an ExternalDataReader instance, specifically a CsvExternalDataReader, for reading
   *     grid-based geospatial data from CSV files.
   */
  public ExternalDataReader createReader() {
    return new CsvExternalDataReader(valueFactory);
  }

  /**
   * Create a new data reader for CSV.
   *
   * <p>Creates and opens a new instance of a CSV-based external data reader. If the file cannot be
   * opened, an exception is thrown, and the reader is closed to release resources.</p>
   *
   * @param filePath the path to the CSV file to be opened
   * @return an ExternalDataReader instance, specifically a CsvExternalDataReader, for reading data
   *     from the specified CSV file
   * @throws RuntimeException if the file cannot be opened or an unexpected error occurs
   */
  public ExternalDataReader createAndOpen(String filePath) {
    CsvExternalDataReader reader = new CsvExternalDataReader(valueFactory);
    try {
      reader.open(filePath);
      return reader;
    } catch (Exception e) {
      try {
        reader.close();
      } catch (Exception ignored) {
        // Ignore close exceptions
      }
      throw new RuntimeException("Failed to open CSV file: " + filePath, e);
    }
  }

  /**
   * Retrieves the current instance of the EngineValueFactory used by this factory.
   *
   * @return the EngineValueFactory instance used for creating EngineValue objects for data
   *     manipulation.
   */
  public EngineValueFactory getValueFactory() {
    return valueFactory;
  }
}
