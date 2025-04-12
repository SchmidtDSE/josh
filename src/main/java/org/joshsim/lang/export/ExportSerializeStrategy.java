/**
 * Definition of structures for exporting results from a simulation to a serialization format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import java.util.stream.Stream;
import org.joshsim.engine.entity.base.Entity;


/**
 * Strategy interface for exporting to a specific form before writing to persistence.
 *
 * @param <T> The serialization type to which entities should be converted.
 */
public interface ExportSerializeStrategy<T> {

  /**
   * Get the serialization-ready records for
   *
   * @param target The entities to be converted to the serialization form.
   * @return Stream (for performance) over the serialized records ready to be written.
   */
  Stream<T> getRecords(Stream<Entity> target);

}
