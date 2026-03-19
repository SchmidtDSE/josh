package org.joshsim.geo.external;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.joshsim.engine.value.engine.EngineValueFactory;

/**
 * Manages a pool of thread-local ExternalDataReader instances for parallel processing.
 *
 * <p>Each thread gets its own reader instance, which is reused for all patches processed
 * by that thread. All readers are tracked for cleanup when processing completes.</p>
 */
class ParallelReaderPool implements AutoCloseable {

  private final ThreadLocal<ExternalDataReader> threadLocalReader;
  private final ConcurrentLinkedQueue<ExternalDataReader> createdReaders;

  /**
   * Creates a new parallel reader pool.
   *
   * @param valueFactory Factory for creating engine values
   * @param dataFilePath Path to the data file
   * @param dimensionX X dimension name
   * @param dimensionY Y dimension name
   * @param timeDimension Time dimension name (may be null)
   * @param crsCode CRS code (may be null)
   */
  ParallelReaderPool(
      EngineValueFactory valueFactory,
      String dataFilePath,
      String dimensionX,
      String dimensionY,
      String timeDimension,
      String crsCode) {

    this.createdReaders = new ConcurrentLinkedQueue<>();
    this.threadLocalReader = ThreadLocal.withInitial(() -> {
      try {
        ExternalDataReader reader = ExternalDataReaderFactory.createReader(
            valueFactory, dataFilePath);
        reader.open(dataFilePath);
        reader.setDimensions(dimensionX, dimensionY, Optional.ofNullable(timeDimension));
        if (crsCode != null) {
          reader.setCrsCode(crsCode);
        }
        createdReaders.add(reader);
        return reader;
      } catch (Exception e) {
        throw new RuntimeException("Failed to create thread-local reader", e);
      }
    });
  }

  /**
   * Gets the reader for the current thread.
   *
   * @return The thread-local reader instance
   */
  ExternalDataReader getReader() {
    return threadLocalReader.get();
  }

  /**
   * Closes all readers created by this pool.
   */
  @Override
  public void close() {
    for (ExternalDataReader reader : createdReaders) {
      try {
        reader.close();
      } catch (Exception e) {
        // Ignore cleanup errors
      }
    }
    threadLocalReader.remove();
  }
}
