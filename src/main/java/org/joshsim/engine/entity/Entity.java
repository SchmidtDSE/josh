/**
 * @license BSD-3-Clause
 */
package org.joshsim.engine.entity;

import java.util.Optional;

import org.joshsim.engine.value.EngineValue;


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

    /**
     * Acquires a global lock on this entity for thread safety.
     * This is a convenience method for client code and is not automatically 
     * enforced by getters and setters. The method will block until the lock 
     * is acquired.
     */
    void lock();

    /**
     * Releases the global lock on this entity.
     * This is a convenience method for client code and should be called after 
     * thread-safe operations are complete.
     */
    void unlock();
}