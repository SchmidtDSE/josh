/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.events;

import java.util.Optional;

/**
 * Interface representing a group of event handlers.
 */
public interface EventHandlerGroup {
    /**
     * Gets all event handlers in this group.
     *
     * @return an Iterable of EventHandler objects
     */
    Iterable<EventHandler> getEventHandlers();

    /**
     * Gets the state associated with this event handler group.
     *
     * @return an Optional containing the state String if one exists, empty otherwise
     */
    Optional<String> getState();
}