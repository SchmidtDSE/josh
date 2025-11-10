/**
 * Combined structured output writer that routes to per-entity-type writers.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes structured data rows to appropriate writers based on entity type.
 *
 * <p>This writer manages multiple OutputWriter&lt;Map&lt;String, String&gt;&gt; instances,
 * one per entity type (e.g., "patch", "organism", "meta"). It uses ThreadLocal to track
 * the current entity type and routes write() calls to the appropriate underlying writer.</p>
 *
 * <p><strong>Threading Model:</strong></p>
 * <ul>
 *   <li>Uses ThreadLocal to track current entity type per thread</li>
 *   <li>Thread-safe - each thread maintains its own entity type context</li>
 *   <li>Calls setCurrentEntityType() before write() to route correctly</li>
 *   <li>Calls clearCurrentEntityType() after batch of writes</li>
 * </ul>
 *
 * <p><strong>Unconfigured Entity Types:</strong></p>
 * <ul>
 *   <li>If entity type is not configured, write() is a silent no-op</li>
 *   <li>Zero overhead for debug output when not configured</li>
 *   <li>Check isConfigured() to determine if an entity type has a writer</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * Map<String, OutputWriter<Map<String, String>>> writersByType = new HashMap<>();
 * writersByType.put("patch", patchWriter);
 * writersByType.put("organism", organismWriter);
 *
 * CombinedStructuredWriter combined = new CombinedStructuredWriter(writersByType);
 * combined.start();
 *
 * combined.setCurrentEntityType("patch");
 * Map<String, String> patchRow = new LinkedHashMap<>();
 * patchRow.put("biomass", "100.5");
 * combined.write(patchRow, 100);
 *
 * combined.setCurrentEntityType("organism");
 * Map<String, String> organismRow = new LinkedHashMap<>();
 * organismRow.put("age", "5.0");
 * combined.write(organismRow, 100);
 *
 * combined.join();
 * }</pre>
 *
 * @see OutputWriter
 * @see StructuredOutputWriter
 */
public class CombinedStructuredWriter implements OutputWriter<Map<String, String>> {

  private final Map<String, OutputWriter<Map<String, String>>> writersByEntityType;
  private final ThreadLocal<String> currentEntityType;

  /**
   * Constructs a CombinedStructuredWriter with the specified writers by entity type.
   *
   * @param writersByEntityType Map from entity type names to OutputWriter instances
   */
  public CombinedStructuredWriter(
      Map<String, OutputWriter<Map<String, String>>> writersByEntityType) {
    this.writersByEntityType = new HashMap<>(writersByEntityType);
    this.currentEntityType = new ThreadLocal<>();
  }

  /**
   * Sets the current entity type for this thread.
   *
   * <p>This method must be called before write() to route data to the correct writer.
   * The entity type context is thread-local, allowing concurrent writes from
   * multiple threads with different entity types.</p>
   *
   * @param entityType The entity type to use for subsequent write() calls on this thread
   */
  public void setCurrentEntityType(String entityType) {
    currentEntityType.set(entityType);
  }

  /**
   * Gets the current entity type for this thread.
   *
   * @return The current entity type, or null if not set
   */
  public String getCurrentEntityType() {
    return currentEntityType.get();
  }

  /**
   * Clears the current entity type for this thread.
   *
   * <p>Call this method after a batch of writes to clean up thread-local state.</p>
   */
  public void clearCurrentEntityType() {
    currentEntityType.remove();
  }

  /**
   * Checks if a writer is configured for the specified entity type.
   *
   * @param entityType The entity type to check
   * @return true if a writer exists for this entity type, false otherwise
   */
  public boolean isConfigured(String entityType) {
    return writersByEntityType.containsKey(entityType);
  }

  /**
   * Gets the number of configured writers.
   *
   * @return The number of entity types with writers
   */
  public int getWriterCount() {
    return writersByEntityType.size();
  }

  @Override
  public void start() {
    writersByEntityType.values().forEach(OutputWriter::start);
  }

  @Override
  public void join() {
    writersByEntityType.values().forEach(OutputWriter::join);
  }

  @Override
  public void write(Map<String, String> data, long step) {
    String entityType = currentEntityType.get();
    OutputWriter<Map<String, String>> writer = writersByEntityType.get(entityType);
    if (writer != null) {
      writer.write(data, step);
    }
    // Silent no-op if entity type not configured
  }

  @Override
  public String getPath() {
    String entityType = currentEntityType.get();
    if (entityType == null) {
      return "";
    }
    OutputWriter<Map<String, String>> writer = writersByEntityType.get(entityType);
    if (writer == null) {
      return "";
    }
    return writer.getPath();
  }

}
