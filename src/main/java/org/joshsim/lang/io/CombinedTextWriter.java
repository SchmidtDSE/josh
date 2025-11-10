/**
 * Combined output writer that routes text messages to per-entity-type writers.
 *
 * <p>This writer implements OutputWriter&lt;String&gt; and routes debug messages to different
 * output destinations based on the entity type. This allows separate debug files for different
 * entity types (e.g., one file for "ForeverTree" organisms, another for "patch" entities).</p>
 *
 * <p>Routing is based on a ThreadLocal context that tracks the current entity type. When
 * write() is called, the writer looks up the appropriate per-entity-type writer and delegates
 * to it. If no writer is configured for the current entity type, the write is silently ignored
 * (zero overhead).</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * Map&lt;String, OutputWriter&lt;String&gt;&gt; writers = new HashMap&lt;&gt;();
 * writers.put("patch", patchWriter);
 * writers.put("organism", organismWriter);
 *
 * CombinedTextWriter combined = new CombinedTextWriter(writers);
 * combined.start();
 *
 * combined.setCurrentEntityType("patch");
 * combined.write("Patch debug message", 1);
 *
 * combined.setCurrentEntityType("organism");
 * combined.write("Organism debug message", 1);
 *
 * combined.join();
 * </pre>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Map;

/**
 * OutputWriter that routes text messages to different writers based on entity type.
 *
 * <p>This implementation maintains a map of entity types to OutputWriter instances and uses
 * a ThreadLocal to track the current entity type context. This allows thread-safe routing
 * in multi-threaded simulation environments.</p>
 */
public class CombinedTextWriter implements OutputWriter<String> {

  private final Map<String, OutputWriter<String>> writersByEntityType;
  private final ThreadLocal<String> currentEntityType;

  /**
   * Creates a combined text writer with the specified per-entity-type writers.
   *
   * <p>The writers map should contain entries for each entity type that needs separate
   * debug output. Entity types not in the map will have their debug messages silently
   * ignored.</p>
   *
   * @param writersByEntityType Map from entity type names to their corresponding writers
   */
  public CombinedTextWriter(Map<String, OutputWriter<String>> writersByEntityType) {
    this.writersByEntityType = writersByEntityType;
    this.currentEntityType = new ThreadLocal<>();
  }

  @Override
  public void start() {
    // Start all per-entity-type writers
    writersByEntityType.values().forEach(OutputWriter::start);
  }

  @Override
  public void join() {
    // Wait for all per-entity-type writers to complete
    writersByEntityType.values().forEach(OutputWriter::join);
  }

  @Override
  public void write(String data, long step) {
    String entityType = currentEntityType.get();
    if (entityType == null) {
      // No entity type set - silent no-op (zero overhead)
      return;
    }

    OutputWriter<String> writer = writersByEntityType.get(entityType);
    if (writer != null) {
      // Route to the appropriate writer
      // If the writer is a TextOutputWriter, use the extended write method with entityType
      if (writer instanceof TextOutputWriter) {
        ((TextOutputWriter) writer).write(data, step, entityType);
      } else {
        writer.write(data, step);
      }
    }
    // Silent no-op if no writer configured for this entity type
  }

  @Override
  public String getPath() {
    // Return a descriptive string indicating this is a combined writer
    return "combined://" + writersByEntityType.size() + " entity types";
  }

  /**
   * Sets the current entity type context for this thread.
   *
   * <p>This method should be called before write() to establish which entity type's
   * writer should receive the next message. The entity type context is thread-local,
   * so different threads can have different entity type contexts simultaneously.</p>
   *
   * @param entityType The entity type name (e.g., "ForeverTree", "patch", "agent")
   */
  public void setCurrentEntityType(String entityType) {
    currentEntityType.set(entityType);
  }

  /**
   * Clears the current entity type context for this thread.
   *
   * <p>After calling this method, subsequent write() calls will be silently ignored
   * until setCurrentEntityType() is called again.</p>
   */
  public void clearCurrentEntityType() {
    currentEntityType.remove();
  }

  /**
   * Gets the current entity type context for this thread.
   *
   * @return The current entity type, or null if none is set
   */
  public String getCurrentEntityType() {
    return currentEntityType.get();
  }

  /**
   * Checks if any writers are configured.
   *
   * @return true if at least one entity type writer is configured, false otherwise
   */
  public boolean isConfigured() {
    return !writersByEntityType.isEmpty();
  }

  /**
   * Gets the number of configured entity type writers.
   *
   * @return The number of entity types with configured writers
   */
  public int getWriterCount() {
    return writersByEntityType.size();
  }
}
