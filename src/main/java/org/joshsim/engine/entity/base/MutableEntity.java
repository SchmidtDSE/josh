/**
 * Base entity which is a mutable entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.value.type.EngineValue;


/**
 * An entity whose state can be mutated in-place.
 */
public interface MutableEntity extends Entity, Lockable {

  /**
   * Get event handlers for all attributes and events.
   *
   * @returns Hashmap of (state x attribute x event) to EventHandlerGroups.
   */
  Iterable<EventHandlerGroup> getEventHandlers();

  /**
   * Get event handlers for a specific attribute and event.
   *
   * @param eventKey The event for which handler groups should be returned.
   * @return the event handler group, or empty if it does not exist
   */
  public Optional<EventHandlerGroup> getEventHandlers(EventKey eventKey);

  @Override
  public Optional<EngineValue> getAttributeValue(String name);

  /**
   * Set the value of an attribute by name.
   *
   * @param name the attribute name
   * @param value the value to set
   */
  public void setAttributeValue(String name, EngineValue value);

}
