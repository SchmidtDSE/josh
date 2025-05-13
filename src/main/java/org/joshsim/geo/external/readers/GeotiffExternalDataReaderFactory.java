/**
 * Logic to instantiate a geotiff reader.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.geo.external.readers;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.ExternalDataReader;

/**
 * Factory for creating GeotiffExternalDataReader instances.
 */
public class GeotiffExternalDataReaderFactory {
  private final EngineValueFactory valueFactory;

  /**
   * Creates a new factory with the specified engine value factory.
   */
  public GeotiffExternalDataReaderFactory(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  /**
   * Creates a new Geotiff external data reader.
   */
  public ExternalDataReader createReader() {
    return new GeotiffExternalDataReader(valueFactory, Units.EMPTY);
  }

  /**
   * Creates a new Geotiff external data reader and opens the specified file.
   */
  public ExternalDataReader createAndOpen(String filePath) {
    GeotiffExternalDataReader reader = new GeotiffExternalDataReader(
        valueFactory,
        Units.of("mm")
    );
    try {
      reader.open(filePath);
      return reader;
    } catch (Exception e) {
      try {
        reader.close();
      } catch (Exception ignored) {
        // Ignore close exceptions
      }
      throw new RuntimeException("Failed to open Geotiff file: " + filePath, e);
    }
  }

  /**
   * Creates a factory with system default settings for the engine value factory.
   */
  public static GeotiffExternalDataReaderFactory createWithDefaults() {
    return new GeotiffExternalDataReaderFactory(new EngineValueFactory());
  }

  /**
   * Gets the value factory used by this reader factory.
   */
  public EngineValueFactory getValueFactory() {
    return valueFactory;
  }
}
