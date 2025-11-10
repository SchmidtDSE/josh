/**
 * Generic interface for writing data to various output destinations.
 *
 * <p>This interface provides a unified abstraction for writing data of type T to different
 * output targets (file, MinIO, stdout). It supports both debug output (String) and structured
 * export output (DataRow) through type parameterization.</p>
 *
 * <p>OutputWriter implementations handle asynchronous writing through internal queues,
 * allowing producer threads to continue execution while a dedicated writer thread processes
 * and persists data in the background.</p>
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>Create OutputWriter instance via OutputWriterFactory</li>
 *   <li>Call {@link #start()} to begin background writing</li>
 *   <li>Call {@link #write(Object, long)} to queue data items</li>
 *   <li>Call {@link #join()} to wait for all queued items to be written</li>
 * </ol>
 *
 * <p>Example usage for debug output:</p>
 * <pre>
 * OutputWriter&lt;String&gt; debugWriter = factory.createTextWriter("file:///tmp/debug.txt");
 * debugWriter.start();
 * debugWriter.write("Debug message at step 1", 1);
 * debugWriter.write("Debug message at step 2", 2);
 * debugWriter.join();
 * </pre>
 *
 * <p>Example usage for export output:</p>
 * <pre>
 * OutputWriter&lt;DataRow&gt; exportWriter =
 *     factory.createStructuredWriter("minio://bucket/export.csv");
 * exportWriter.start();
 * exportWriter.write(dataRow1, 1);
 * exportWriter.write(dataRow2, 2);
 * exportWriter.join();
 * </pre>
 *
 * @param <T> The type of data being written (String for debug, DataRow for export)
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

/**
 * Generic output writer that handles writing data of type T to various destinations.
 *
 * @param <T> The type of data being written (String for debug, DataRow for export)
 */
public interface OutputWriter<T> {

  /**
   * Starts the writer's background processing.
   *
   * <p>Begins processing data items to be written to the output target in a dedicated writer
   * thread which operates in the background, writing items to the output location as they are
   * added to the queue. If the writer is already active, calling this method has no effect.</p>
   *
   * <p>For some implementations (like stdout or memory), this may be a no-op.</p>
   */
  void start();

  /**
   * Waits for all pending writes to complete.
   *
   * <p>Ensures that all pending data items in the queue are processed and written to the output
   * before returning. This method blocks until the writer thread is fully terminated, which
   * occurs when its queue is empty and all data has been flushed to the output destination.</p>
   *
   * <p>For some implementations (like stdout or memory), this may be a no-op.</p>
   */
  void join();

  /**
   * Writes a single data item at the given step.
   *
   * <p>Queues the data item for asynchronous writing by the background writer thread. The data
   * will be written to the output destination along with its associated step number. The actual
   * write operation happens asynchronously, so this method returns immediately.</p>
   *
   * @param data The data item to write (String for debug, DataRow for export)
   * @param step The simulation step number associated with this data
   */
  void write(T data, long step);

  /**
   * Gets the resolved output path.
   *
   * <p>Returns the fully resolved path where output is being written. For file and MinIO
   * destinations, this is the complete path after template variable resolution. For stdout
   * destinations, this may return a descriptive string like "stdout".</p>
   *
   * <p>Template variables like {replicate}, {user}, {editor}, and other custom tags are
   * resolved before this value is returned.</p>
   *
   * @return The resolved output path or destination identifier
   */
  String getPath();
}
