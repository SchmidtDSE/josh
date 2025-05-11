
package org.joshsim.geo.external.readers;

import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.ExternalDataReader;

public class CsvExternalDataReaderFactory {
  private final EngineValueFactory valueFactory;

  public CsvExternalDataReaderFactory(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  public ExternalDataReader createReader() {
    return new CsvExternalDataReader(valueFactory);
  }

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

  public static CsvExternalDataReaderFactory createWithDefaults() {
    return new CsvExternalDataReaderFactory(new EngineValueFactory());
  }

  public EngineValueFactory getValueFactory() {
    return valueFactory;
  }
}
