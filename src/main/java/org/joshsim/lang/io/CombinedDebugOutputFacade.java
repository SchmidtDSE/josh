/**
 * Combined facade that routes debug messages to per-entity-type output facades.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes debug messages to different output facades based on entity type.
 *
 * <p>This facade follows the same architectural pattern as {@link CombinedExportFacade},
 * which routes entity exports to different destinations. The key differences are:</p>
 * <ul>
 *   <li>This routes String debug messages, CombinedExportFacade routes Entity data</li>
 *   <li>This uses {@link DebugOutputFacade}, CombinedExportFacade uses format-specific facades</li>
 * </ul>
 *
 * <p>Uses a ThreadLocal to track the current entity type context, allowing thread-safe
 * routing in multi-threaded simulation environments (e.g., parallel replicates).</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * Map&lt;String, DebugOutputFacade&gt; facades = new HashMap&lt;&gt;();
 * facades.put("patch", new DebugOutputFacade(new LocalOutputStreamStrategy("/tmp/patch.log")));
 * facades.put("agent", new DebugOutputFacade(new LocalOutputStreamStrategy("/tmp/agent.log")));
 *
 * CombinedDebugOutputFacade combined = new CombinedDebugOutputFacade(facades);
 * combined.start();
 *
 * // In simulation, entity type is set automatically by the machine
 * combined.write("Debug message", 1, "patch");
 *
 * combined.join();
 * </pre>
 *
 * @see DebugOutputFacade
 * @see CombinedExportFacade
 */
public class CombinedDebugOutputFacade {

  private final Map<String, DebugOutputFacade> facadesByEntityType;
  private final ThreadLocal<String> currentEntityType;

  /**
   * Creates a combined facade with the specified per-entity-type output facades.
   *
   * <p>The facades map should contain entries for each entity type that needs separate
   * debug output. Entity types not in the map will have their debug messages silently
   * ignored (zero overhead).</p>
   *
   * @param facadesByEntityType Map from entity type names to their corresponding facades.
   */
  public CombinedDebugOutputFacade(Map<String, DebugOutputFacade> facadesByEntityType) {
    this.facadesByEntityType = new HashMap<>(facadesByEntityType);
    this.currentEntityType = new ThreadLocal<>();
  }

  /**
   * Creates an empty combined facade (no configured outputs).
   *
   * <p>Use this constructor when debug output is disabled. All write operations
   * will be silently ignored with zero overhead.</p>
   */
  public CombinedDebugOutputFacade() {
    this(new HashMap<>());
  }

  /**
   * Starts all per-entity-type output facades.
   */
  public void start() {
    facadesByEntityType.values().forEach(DebugOutputFacade::start);
  }

  /**
   * Waits for all per-entity-type output facades to complete.
   */
  public void join() {
    facadesByEntityType.values().forEach(DebugOutputFacade::join);
  }

  /**
   * Writes a debug message with entity context information.
   *
   * <p>The message is routed based on entity type. If no facade is configured
   * for the entity type, the message is silently ignored.</p>
   *
   * @param message The debug message to write.
   * @param step The current simulation step number.
   * @param entityType The entity type for routing.
   * @param identifier Unique identifier for the entity (hex hash).
   * @param x X coordinate of the entity's location.
   * @param y Y coordinate of the entity's location.
   */
  public void write(String message, long step, String entityType, String identifier,
      double x, double y) {
    DebugOutputFacade facade = facadesByEntityType.get(entityType);
    if (facade != null) {
      facade.write(message, step, entityType, identifier, x, y);
    }
  }

  /**
   * Sets the current entity type context for this thread.
   *
   * <p>This method should be called before write() to establish which entity type's
   * facade should receive the next message. The entity type context is thread-local,
   * so different threads can have different entity type contexts simultaneously.</p>
   *
   * @param entityType The entity type name (e.g., "patch", "agent", "disturbance").
   */
  public void setCurrentEntityType(String entityType) {
    currentEntityType.set(entityType);
  }

  /**
   * Clears the current entity type context for this thread.
   */
  public void clearCurrentEntityType() {
    currentEntityType.remove();
  }

  /**
   * Gets the current entity type context for this thread.
   *
   * @return The current entity type, or null if none is set.
   */
  public String getCurrentEntityType() {
    return currentEntityType.get();
  }

  /**
   * Checks if any output facades are configured.
   *
   * @return true if at least one entity type facade is configured, false otherwise.
   */
  public boolean isConfigured() {
    return !facadesByEntityType.isEmpty();
  }

  /**
   * Gets the number of configured entity type facades.
   *
   * @return The number of entity types with configured facades.
   */
  public int getFacadeCount() {
    return facadesByEntityType.size();
  }

}
