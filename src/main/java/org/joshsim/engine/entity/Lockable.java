/**
 * Interface for entities that can be locked for thread safety.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

/**
 * Interface for entities that can be locked for thread safety.
 *
 * <p>Entities that implement this interface can be locked to prevent concurrent access to their
 * data. This is useful when multiple threads are accessing the same entity and need to ensure that
 * their operations are atomic.</p>
 */
public interface Lockable {

  /**
   * Acquire a global lock on this entity for thread safety.
   *
   * <p>This is a convenience method for client code and is not automatically  enforced by getters
   * and setters. The method will block until the lock is acquired.</p>
   */
  void lock();

  /**
   * Release the global lock on this entity.
   *
   * <p>This is a convenience method for client code and should be called after thread-safe
   * operations are complete.</p>
   */
  void unlock();

}