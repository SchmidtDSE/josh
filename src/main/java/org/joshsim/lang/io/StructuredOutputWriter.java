/**
 * Structured data output writer for CSV export.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.QueueService;
import org.joshsim.compat.QueueServiceCallback;
import org.joshsim.lang.io.strategy.CsvWriteStrategy;
import org.joshsim.lang.io.strategy.StringMapWriteStrategy;

/**
 * Writes structured data rows (Map&lt;String, String&gt;) to CSV files.
 *
 * <p>This writer implements the OutputWriter interface for structured data export,
 * handling CSV formatting with comma-separated values, proper quoting, and header
 * rows. It supports both file:// and minio:// destinations using queue-based
 * async writing for performance.</p>
 *
 * <p><strong>CSV Format:</strong></p>
 * <ul>
 *   <li>Comma-separated values with proper quoting (via Apache Commons CSV)</li>
 *   <li>Header row with column names</li>
 *   <li>Automatic addition of 'step' and 'replicate' columns</li>
 *   <li>Empty string for missing values</li>
 * </ul>
 *
 * <p><strong>Threading Model:</strong></p>
 * <ul>
 *   <li>Queue-based async writing via QueueService</li>
 *   <li>Producer thread calls write() - non-blocking queue add</li>
 *   <li>Consumer thread processes queue and writes to CSV</li>
 *   <li>Call start() to begin consumer thread</li>
 *   <li>Call join() to wait for queue to drain</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * OutputTarget target = new OutputTarget("file", "/tmp/output.csv");
 * OutputStreamStrategy strategy = new LocalOutputStreamStrategy("/tmp/output.csv", true);
 * StructuredOutputWriter writer = new StructuredOutputWriter(target, strategy);
 *
 * writer.start();
 *
 * Map<String, String> row = new LinkedHashMap<>();
 * row.put("age", "5.0");
 * row.put("height", "10.2");
 * writer.write(row, 100); // step = 100
 *
 * writer.join();
 * }</pre>
 *
 * @see OutputWriter
 * @see CsvWriteStrategy
 */
public class StructuredOutputWriter implements OutputWriter<Map<String, String>> {

  private final OutputTarget target;
  private final Optional<Iterable<String>> header;
  private final InnerWriter innerWriter;
  private final QueueService queueService;
  private final int replicateNumber;

  /**
   * Constructs a StructuredOutputWriter with the specified output target and strategy.
   *
   * @param target The output target describing destination and path
   * @param outputStrategy The strategy to provide an output stream for writing
   * @param replicateNumber The replicate number to include in output rows
   */
  public StructuredOutputWriter(OutputTarget target, OutputStreamStrategy outputStrategy,
                                 int replicateNumber) {
    this.target = target;
    this.header = Optional.empty();
    this.replicateNumber = replicateNumber;
    this.innerWriter = new InnerWriter(header, outputStrategy, replicateNumber);
    this.queueService = CompatibilityLayerKeeper.get().createQueueService(innerWriter);
  }

  /**
   * Constructs a StructuredOutputWriter with specified headers.
   *
   * @param target The output target describing destination and path
   * @param outputStrategy The strategy to provide an output stream for writing
   * @param replicateNumber The replicate number to include in output rows
   * @param header Iterable over the header columns to use for the CSV output
   */
  public StructuredOutputWriter(OutputTarget target, OutputStreamStrategy outputStrategy,
                                 int replicateNumber, Iterable<String> header) {
    this.target = target;
    this.header = Optional.of(header);
    this.replicateNumber = replicateNumber;
    this.innerWriter = new InnerWriter(Optional.of(header), outputStrategy, replicateNumber);
    this.queueService = CompatibilityLayerKeeper.get().createQueueService(innerWriter);
  }

  @Override
  public void start() {
    queueService.start();
  }

  @Override
  public void join() {
    queueService.join();
  }

  @Override
  public void write(Map<String, String> data, long step) {
    WriteTask<Map<String, String>> task = new WriteTask<>(data, step);
    queueService.add(task);
  }

  @Override
  public String getPath() {
    return target.getPath();
  }

  /**
   * Callback to write structured data to a CSV file.
   *
   * <p>This inner class handles the actual CSV writing in the consumer thread,
   * processing WriteTask objects from the queue and formatting them as CSV rows.</p>
   */
  private static class InnerWriter implements QueueServiceCallback {

    private final Optional<Iterable<String>> header;
    private final OutputStream outputStream;
    private final StringMapWriteStrategy writeStrategy;
    private final int replicateNumber;

    /**
     * Create a writer which writes to the CSV file as data become available.
     *
     * @param header an optional iterable containing the header values for the CSV output.
     *     If not present, the header will be inferred.
     * @param outputStrategy the strategy to provide an OutputStream instance. This determines
     *     where the data will be written.
     * @param replicateNumber The replicate number to add to each row
     * @throws RuntimeException if an exception occurs while attempting to open the OutputStream.
     */
    public InnerWriter(Optional<Iterable<String>> header, OutputStreamStrategy outputStrategy,
                       int replicateNumber) {
      this.header = header;
      this.replicateNumber = replicateNumber;

      try {
        outputStream = outputStrategy.open();
      } catch (IOException e) {
        throw new RuntimeException("Error opening output stream", e);
      }

      if (header.isPresent()) {
        writeStrategy = new CsvWriteStrategy(header.get());
      } else {
        writeStrategy = new CsvWriteStrategy();
      }
    }

    @Override
    public void onStart() {
      // No initialization needed
    }

    @Override
    public void onTask(Optional<Object> taskMaybe) {
      if (taskMaybe.isEmpty()) {
        writeStrategy.flush();
        return;
      }

      @SuppressWarnings("unchecked")
      WriteTask<Map<String, String>> task = (WriteTask<Map<String, String>>) taskMaybe.get();
      long step = task.getStep();

      try {
        Map<String, String> original = task.getData();

        // Create a LinkedHashMap to preserve ordering and ensure step/replicate are last
        Map<String, String> serialized = new LinkedHashMap<>();

        // Add all original data first
        original.entrySet().forEach(entry -> serialized.put(entry.getKey(), entry.getValue()));

        // Add step column (before replicate to match web editor behavior)
        serialized.put("step", Long.toString(step));

        // Add replicate column as the last column (matches web editor)
        serialized.put("replicate", Integer.toString(replicateNumber));

        writeStrategy.write(serialized, outputStream);
      } catch (IOException e) {
        throw new RuntimeException("Error writing to output stream", e);
      }
    }

    @Override
    public void onEnd() {
      writeStrategy.close();

      try {
        outputStream.close();
      } catch (IOException e) {
        throw new RuntimeException("Error closing output stream", e);
      }
    }
  }

}
