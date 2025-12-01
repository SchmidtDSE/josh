/**
 * Key class for mapping state x attribute x event to an EventHandlerGroup.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;

/**
 * Composite key for mapping state, attribute, and event.
 */
public class EventKey {
  // Interning cache for EventKey instances to reduce allocations
  private static final Map<String, EventKey> INTERN_CACHE = new ConcurrentHashMap<>();

  // Maximum cache size before eviction
  private static final int MAX_CACHE_SIZE = 100000;

  private final String state;
  private final String attribute;
  private final String event;
  private final String stringRepresentation;
  private final int hashRepresentation;

  /**
   * Key for mapping attribute x event to an EventHandlerGroup using empty default state.
   *
   * @param attribute attribute string
   * @param event event string
   */
  public EventKey(String attribute, String event) {
    this.state = "";
    this.attribute = attribute;
    this.event = event;
    this.stringRepresentation = generateStringRepresentation();
    this.hashRepresentation = stringRepresentation.hashCode();
  }

  /**
   * Composite key class for mapping state x attribute x event to an EventHandlerGroup.
   *
   * @param state state string or empty if default state
   * @param attribute attribute string
   * @param event event string
   */
  public EventKey(String state, String attribute, String event) {
    this.state = state;
    this.attribute = attribute;
    this.event = event;
    this.stringRepresentation = generateStringRepresentation();
    this.hashRepresentation = stringRepresentation.hashCode();
  }

  /**
   * Gets the state component of this event key.
   *
   * @return the state string, or empty if default state
   */
  public String getState() {
    return state;
  }

  /**
   * Gets the attribute component of this event key.
   *
   * @return the attribute string
   */
  public String getAttribute() {
    return attribute;
  }

  /**
   * Gets the event component of this event key.
   *
   * @return the event string
   */
  public String getEvent() {
    return event;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    EventKey eventKey = (EventKey) o;
    return toString().equals(eventKey.toString());
  }

  @Override
  public int hashCode() {
    return hashRepresentation;
  }

  @Override
  public String toString() {
    return stringRepresentation;
  }

  private String generateStringRepresentation() {
    CompatibleStringJoiner joiner = CompatibilityLayerKeeper.get().createStringJoiner(",");
    joiner.add(state);
    joiner.add(attribute);
    joiner.add(event);
    return joiner.toString();
  }

  /**
   * Factory method to get or create an EventKey for attribute x event (default state).
   *
   * <p>This method implements an interning pattern where EventKey instances are cached and reused
   * to reduce object allocations and expensive string representation generation. The cache is
   * thread-safe via ConcurrentHashMap to support parallel patch processing.</p>
   *
   * @param attribute attribute string
   * @param event event string
   * @return cached or new EventKey instance
   */
  public static EventKey of(String attribute, String event) {
    // Build cache key directly without creating EventKey object
    String cacheKey = "," + attribute + "," + event;

    // Use computeIfAbsent to avoid creating EventKey unless cache miss
    EventKey result = INTERN_CACHE.computeIfAbsent(cacheKey,
        k -> new EventKey(attribute, event));

    // Check cache size and evict if needed
    if (INTERN_CACHE.size() > MAX_CACHE_SIZE) {
      INTERN_CACHE.clear();
      INTERN_CACHE.put(cacheKey, result);
    }

    return result;
  }

  /**
   * Factory method to get or create an EventKey for state x attribute x event.
   *
   * <p>This method implements an interning pattern where EventKey instances are cached and reused
   * to reduce object allocations and expensive string representation generation. The cache is
   * thread-safe via ConcurrentHashMap to support parallel patch processing.</p>
   *
   * @param state state string or empty if default state
   * @param attribute attribute string
   * @param event event string
   * @return cached or new EventKey instance
   */
  public static EventKey of(String state, String attribute, String event) {
    // Build cache key directly without creating EventKey object
    String cacheKey = state + "," + attribute + "," + event;

    // Use computeIfAbsent to avoid creating EventKey unless cache miss
    EventKey result = INTERN_CACHE.computeIfAbsent(cacheKey,
        k -> new EventKey(state, attribute, event));

    // Check cache size and evict if needed
    if (INTERN_CACHE.size() > MAX_CACHE_SIZE) {
      INTERN_CACHE.clear();
      INTERN_CACHE.put(cacheKey, result);
    }

    return result;
  }
}
