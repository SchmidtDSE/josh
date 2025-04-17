/**
 * Utility class for retrieving inner entities from an entity's attributes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Helper class to get inner entities from attribute values.
 *
 * <p>Provides utility methods to extract inner MutableEntity instances from an entity's
 * attributes that contain other entities. This is useful for traversing entity hierarchies
 * and accessing nested entity structures.</p>
 */
public class InnerEntityGetter {

  /**
   * Get a stream of inner MutableEntity instances from attribute values.
   *
   * @param target in which to find the inner entites
   * @return Stream of MutableEntity instances found in attributes that contain entities
   */
  public static Stream<MutableEntity> getInnerEntities(MutableEntity target) {
    return target.getAttributeNames().stream()
      .map(target::getAttributeValue)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter((x) -> x.getLanguageType().containsAttributes())
      .flatMap((x) -> {
        Optional<Integer> sizeMaybe = x.getSize();
        if (sizeMaybe.isEmpty()) {
          return Stream.empty();
        }

        int size = sizeMaybe.get();
        if (size == 1) {
          return Stream.of(x.getAsMutableEntity());
        } else {
          Iterable<EngineValue> innerValues = x.getAsDistribution().getContents(size, false);
          Stream<EngineValue> innerStream = StreamSupport.stream(innerValues.spliterator(), false);
          return innerStream.map(EngineValue::getAsMutableEntity);
        }
      });
  }

  /**
   * Get a stream of inner MutableEntity instances from attribute values.
   *
   * @param target in which to find the inner entites
   * @return Stream of MutableEntity instances found in attributes that contain entities
   */
  public static Stream<Entity> getInnerFrozenEntities(Entity target) {
    return target.getAttributeNames().stream()
      .map(target::getAttributeValue)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter((x) -> x.getLanguageType().containsAttributes())
      .flatMap((x) -> {
        Optional<Integer> sizeMaybe = x.getSize();
        if (sizeMaybe.isEmpty()) {
          return Stream.empty();
        }

        int size = sizeMaybe.get();
        if (size == 1) {
          return Stream.of(x.getAsEntity());
        } else {
          Iterable<EngineValue> innerValues = x.getAsDistribution().getContents(size, false);
          Stream<EngineValue> innerStream = StreamSupport.stream(innerValues.spliterator(), false);
          return innerStream.map(EngineValue::getAsEntity);
        }
      });
  }

  /**
   * Get a stream of all descendent MutableEntity instances from attribute values.
   *
   * <p>Get a stream of all descendent MutableEntity instances from attribute values by recursing
   * through all children into their children.</p>
   *
   * @param target in which to find the inner entites
   * @return Stream of MutableEntity instances found in attributes that contain entities after
   *     recursion
   */
  public static Stream<MutableEntity> getInnerEntitiesRecursive(MutableEntity target) {
    return getInnerEntities(target).flatMap((x) -> Stream.concat(
        Stream.of(x),
        getInnerEntities(x)
    ));
  }

  /**
   * Get a stream of all descendent MutableEntity instances from attribute values.
   *
   * <p>Get a stream of all descendent MutableEntity instances from attribute values by recursing
   * through all children into their children.</p>
   *
   * @param target in which to find the inner entites
   * @return Stream of MutableEntity instances found in attributes that contain entities after
   *     recursion
   */
  public static Stream<Entity> getInnerFrozenEntitiesRecursive(Entity target) {
    return getInnerFrozenEntities(target).flatMap((x) -> Stream.concat(
        Stream.of(x),
        getInnerFrozenEntities(x)
    ));
  }

}
