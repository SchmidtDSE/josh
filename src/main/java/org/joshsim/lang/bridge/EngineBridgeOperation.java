/**
 * Structures for saving operations which may be repeated on an EngineBridge.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.Optional;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Command pattern representing a repeatable operation which can be executed on an EngineBridge.
 */
public interface EngineBridgeOperation {

  /**
   * Operation which can manipulate an EngineBridge in a repeatable way.
   *
   * @param target EngineBridge in which to perform this operation.
   * @return Optional which is empty if the operation does not yield a value (like an assertion)
   *     or an Optional containing the EngineValue which resulted from the operation if it does
   *     yield a value.
   */
  Optional<EngineValue> perform(EngineBridge target);

}
