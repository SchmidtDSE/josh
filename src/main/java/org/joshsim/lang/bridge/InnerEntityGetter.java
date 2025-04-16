
/**
 * Utility class for retrieving inner entities from an entity's attributes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.stream.Stream;
import org.joshsim.engine.entity.base.MutableEntity;

/**
 * Helper class to get inner entities from attribute values.
 *
 * <p>Provides utility methods to extract inner MutableEntity instances from an entity's
 * attributes that contain other entities. This is useful for traversing entity hierarchies
 * and accessing nested entity structures.</p>
 */
public class InnerEntityGetter {

  /**
   * Gets a stream of inner MutableEntity instances from attribute values.
   *
   * @return Stream of MutableEntity instances found in attributes that contain entities
   */
  public static Stream<MutableEntity> getInnerEntities() {
    return getAttributeNames().stream()
      .map(this::getAttributeValue)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter((x) -> x.getLanguageType().containsAttributes())
      .map((x) -> x.getAsMutableEntity());
  }

}
