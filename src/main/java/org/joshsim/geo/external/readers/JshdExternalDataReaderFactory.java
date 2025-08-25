package org.joshsim.geo.external.readers;

import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.ExternalDataReader;

/**
 * Factory for creating JshdExternalDataReader instances.
 */
public class JshdExternalDataReaderFactory {
  private final EngineValueFactory valueFactory;

  /**
   * Creates a new factory with the specified engine value factory.
   *
   * @param valueFactory Factory for creating EngineValue objects
   */
  public JshdExternalDataReaderFactory(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  /**
   * Creates a new JSHD external data reader.
   *
   * @return A new JshdExternalDataReader instance
   */
  public ExternalDataReader createReader() {
    return new JshdExternalDataReader(valueFactory);
  }

  /**
   * Creates a new JSHD external data reader and opens the specified file.
   *
   * @param filePath Path to the JSHD file to open
   * @return A new JshdExternalDataReader instance with the file opened
   * @throws RuntimeException if the file cannot be opened
   */
  public ExternalDataReader createAndOpen(String filePath) {
    JshdExternalDataReader reader = new JshdExternalDataReader(valueFactory);
    try {
      reader.open(filePath);
      return reader;
    } catch (Exception e) {
      try {
        reader.close();
      } catch (Exception ignored) {
        // Ignore close exceptions
      }
      throw new RuntimeException("Failed to open JSHD file: " + filePath, e);
    }
  }

  /**
   * Gets the value factory used by this reader factory.
   *
   * @return The EngineValueFactory instance
   */
  public EngineValueFactory getValueFactory() {
    return valueFactory;
  }
}
