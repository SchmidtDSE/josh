/**
 * Constructs for simulation event callabacks.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.entity;

import java.util.Optional;
import org.joshsim.engine.func.CompiledSelector;

/**
 * Interface for an event handler which offers a callback for an event.
 */
public interface EventHandler {
  /**
   * Get the name of the attribute associated with this event handler.
   *
   * @return the attribute name
   */
  String getAttributeName();

  /**
   * Get the name of the event this handler responds to.
   *
   * @return the event name
   */
  String getEventName();

  /**
   * Get the conditional selector associated with this event handler.
   *
   * @return an Optional containing the CompiledSelector if one exists, empty otherwise
   */
  Optional<CompiledSelector> getConditional();
}
