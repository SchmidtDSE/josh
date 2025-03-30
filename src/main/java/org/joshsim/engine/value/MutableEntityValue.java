
/**
 * Structures describing an individual entity as engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.math.BigDecimal;
import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.entity.MutableEntity;

/**
 * Engine value which only has a single entity or entity reference which is still mutable.
 */
public class MutableEntityValue extends EntityValue {

  private final MutableEntity innerValue;

  /**
   * Constructs an EntityValue with the specified values.
   *
   * @param caster The caster for this engine value.
   * @param innerValue The inner entity value.
   */
  public MutableEntityValue(EngineValueCaster caster, MutableEntity innerValue) {
    super(caster, innerValue);
    this.innerValue = innerValue;
  }

  @Override
  public MutableEntity getAsMutableEntity() {
    return innerValue;
  }

}
