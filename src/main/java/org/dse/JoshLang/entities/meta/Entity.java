/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.core.entity.meta;

import org.dse.JoshLang.core.value.EngineValue;
import org.dse.JoshLang.events.EventHandlerGroup;
import java.util.Optional;

/**
 * Represents a base entity in the system.
 * Provides attribute management and event handling capabilities.
 */
public interface Entity {
    /**
     * Gets event handlers for a specific attribute and event.
     *
     * @param attribute the attribute name
     * @param event the event name
     * @return an iterable collection of event handler groups
     */
    Iterable<EventHandlerGroup> getEventHandler(String attribute, String event);

    /**
     * Gets the value of an attribute by name.
     *
     * @param name the attribute name
     * @return an Optional containing the attribute value, or empty if not found
     */
    Optional<EngineValue> getAttributeValue(String name);

    /**
     * Sets the value of an attribute by name.
     *
     * @param name the attribute name
     * @param value the value to set
     */
    void setAttributeValue(String name, EngineValue value);
}