package org.joshsim.geo.external.readers;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.ExternalDataReader;
import ucar.nc2.dataset.NetcdfDatasets;

/**
 * Factory for creating NetcdfExternalDataReader instances with managed cache initialization.
 */
public class NetcdfExternalDataReaderFactory {
  private static volatile boolean cacheInitialized = false;
  private static final Lock CACHE_LOCK = new ReentrantLock();
  private static final int MIN_CACHE_SIZE = 1;
  private static final int MAX_CACHE_SIZE = 20;
  private static final int CACHE_REFRESH_INTERVAL_SECONDS = 300;


  private final EngineValueFactory valueFactory;

  /**
   * Creates a new factory with the specified engine value factory.
   */
  public NetcdfExternalDataReaderFactory(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  /**
   * Creates a new NetCDF external data reader with cache already initialized.
   */
  public ExternalDataReader createReader() {
    ensureCacheInitialized();
    return new NetcdfExternalDataReader(valueFactory);
  }

  /**
   * Creates a new NetCDF external data reader and opens the specified file.
   */
  public ExternalDataReader createAndOpen(String filePath) {
    ensureCacheInitialized();
    NetcdfExternalDataReader reader = new NetcdfExternalDataReader(valueFactory);
    try {
      reader.open(filePath);
      return reader;
    } catch (Exception e) {
      try {
        reader.close();
      } catch (Exception ignored) {
        // Ignore close exceptions
      }
      throw new RuntimeException("Failed to open NetCDF file: " + filePath, e);
    }
  }

  /**
   * Initializes the NetCDF file cache if it hasn't been initialized yet.
   * This method is thread-safe and ensures the cache is initialized only once.
   */
  private void ensureCacheInitialized() {
    if (!cacheInitialized) {
      CACHE_LOCK.lock();
      try {
        if (!cacheInitialized) {
          NetcdfDatasets.initNetcdfFileCache(
              MIN_CACHE_SIZE,
              MAX_CACHE_SIZE,
              CACHE_REFRESH_INTERVAL_SECONDS
          );
          cacheInitialized = true;
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to initialize NetCDF file cache", e);
      }
    }
  }

  /**
   * Gets the value factory used by this reader factory.
   */
  public EngineValueFactory getValueFactory() {
    return valueFactory;
  }
}
