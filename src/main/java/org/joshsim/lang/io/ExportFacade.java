/**
 * Structures describing high level strategies to write entities to persistence.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.wire.NamedMap;


/**
 * Strategy to serialize and write entities to persistence in a separate writer thread.
 */
public interface ExportFacade {

  /**
   * Starts the export process in a dedicated writer thread.
   *
   * <p>Begin processing entities to be written to the export target in a writer thread which
   * operates in the background, serializing and writing entities to the output location as they are
   * added to the queue. If the export process is already active, calling this method has no effect.
   * </p>
   */
  void start();

  /**
   * Stops the export process and waits for the completion of the writer thread.
   *
   * <p>Ensure that all pending entities in the queue are processed and written to the output
   * before shutting down the writer thread, blocking until the writer thread is fully terminated
   * which occurs when its queue is empty.</p>
   */
  void join();

  /**
   * Adds the specified entity to the queue for processing and writing to the export target.
   *
   * @param target The entity to be serialized and written to the output.
   * @param step The step number from the simulation from which this entity snapshot was created.
   */
  void write(Entity target, long step);

  /**
   * Adds the specified NamedMap to the queue for processing and writing to the export target.
   *
   * <p>This method allows wire format deserializers to bypass Entity serialization by providing
   * pre-serialized data directly. The NamedMap contains both the name identifier and the
   * key-value pairs that would normally be produced by serializing an Entity.</p>
   *
   * @param namedMap The NamedMap containing name and serialized data to be written to output.
   * @param step The step number from the simulation from which this data was created.
   */
  void write(NamedMap namedMap, long step);

}
