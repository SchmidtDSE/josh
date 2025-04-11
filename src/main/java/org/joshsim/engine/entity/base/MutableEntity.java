/**
 * Base entity which is a mutable entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.Optional;
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
  Optional<EventHandlerGroup> getEventHandlers(EventKey eventKey);

  @Override
  Optional<EngineValue> getAttributeValue(String name);

  /**
   * Set the value of an attribute by name.
   *
   * @param name the attribute name
   * @param value the value to set
   */
  void setAttributeValue(String name, EngineValue value);

  /**
   * Indicate that this entity is starting a substep or step phase like step.
   *
   * <p>Indicate that this entity is starting a substep or step phase in which it may be mutated,
   * acquiring a global lock on this entity for thread safety.</p>
   *
   * @param name name of the substep or phase like start which is beginning.
   */
  void startSubstep(String name);

  /**
   * Indicate that this entity is finishing with a substep or step phase like start.
   *
   * <p>Indicate that this entity is ending a substep or step phase in which it may be mutated,
   * releasing a global lock on this entity for thread safety.</p>
   */
  void endSubstep();


  /**
   * Get the name of the current substep or phase.
   *
   * <p>Return the name of the substep or phase that is currently in progress, if any, as an
   * Optional string. This can be useful for debugging or logging purposes to know which stage the
   * entity is in during its lifecycle. The returned value can be empty if no substep is currently
   * active.</p>
   *
   * @return the name of the current substep, or an empty Optional if no substep currently active.
   */
  Optional<String> getSubstep();

}
