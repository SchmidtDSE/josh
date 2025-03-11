/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.events;

import java.util.Optional;
import org.dse.JoshLang.core.compiled.CompiledSelector;

/**
 * Interface for handling events in the system.
 */
public interface EventHandler {
    /**
     * Gets the name of the attribute associated with this event handler.
     *
     * @return the attribute name
     */
    String getAttributeName();

    /**
     * Gets the name of the event this handler responds to.
     *
     * @return the event name
     */
    String getEventName();

    /**
     * Gets the conditional selector associated with this event handler.
     *
     * @return an Optional containing the CompiledSelector if one exists, empty otherwise
     */
    Optional<CompiledSelector> getConditional();
}