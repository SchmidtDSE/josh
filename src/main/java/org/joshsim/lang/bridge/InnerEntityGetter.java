/**
 * Utility class for retrieving inner entities from an entity's attributes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
   * Get inner MutableEntity instances from attribute values.
   *
   * <p>Iterates through all attribute names and collects MutableEntity instances from attributes
   * that contain entities. Uses direct iteration instead of streams for better performance
   * in this hot path (called in every substep for every entity).</p>
   *
   * @param target in which to find the inner entites
   * @return Iterable of MutableEntity instances found in attributes that contain entities
   */
  public static Iterable<MutableEntity> getInnerEntities(MutableEntity target) {
    List<MutableEntity> result = new ArrayList<>();

    // Use integer-based iteration
    Map<String, Integer> indexMap = target.getAttributeNameToIndex();
    int numAttributes = indexMap.size();

    for (int i = 0; i < numAttributes; i++) {
      Optional<EngineValue> valueMaybe = target.getAttributeValue(i);
      boolean valueNotSet = valueMaybe.isEmpty();
      if (valueNotSet) {
        // Continue for profiling reasons
        continue;
      }

      EngineValue value = valueMaybe.get();
      boolean hasInnerAttrs = value.getLanguageType().containsAttributes();
      if (!hasInnerAttrs) {
        // Continue for profiling reasons
        continue;
      }

      Optional<Integer> sizeMaybe = value.getSize();
      if (sizeMaybe.isEmpty()) {
        continue;
      }

      int size = sizeMaybe.get();
      if (size == 1) {
        result.add(value.getAsMutableEntity());
      } else {
        Iterable<EngineValue> innerValues = value.getAsDistribution().getContents(size, false);
        for (EngineValue innerValue : innerValues) {
          result.add(innerValue.getAsMutableEntity());
        }
      }
    }

    return result;
  }

  /**
   * Get inner Entity instances from attribute values.
   *
   * <p>Iterates through all attribute names and collects Entity instances from attributes
   * that contain entities. Uses direct iteration instead of streams for better performance.</p>
   *
   * @param target in which to find the inner entites
   * @return Iterable of Entity instances found in attributes that contain entities
   */
  public static Iterable<Entity> getInnerFrozenEntities(Entity target) {
    List<Entity> result = new ArrayList<>();

    // Use integer-based iteration
    Map<String, Integer> indexMap = target.getAttributeNameToIndex();
    int numAttributes = indexMap.size();

    for (int i = 0; i < numAttributes; i++) {
      Optional<EngineValue> valueMaybe = target.getAttributeValue(i);
      boolean valueNotSet = valueMaybe.isEmpty();
      if (valueNotSet) {
        // Continue for profiling reasons
        continue;
      }

      EngineValue value = valueMaybe.get();
      boolean hasInnerAttrs = value.getLanguageType().containsAttributes();
      if (!hasInnerAttrs) {
        // Continue for profiling reasons
        continue;
      }

      Optional<Integer> sizeMaybe = value.getSize();
      if (sizeMaybe.isEmpty()) {
        continue;
      }

      int size = sizeMaybe.get();
      if (size == 1) {
        result.add(value.getAsEntity());
      } else {
        Iterable<EngineValue> innerValues = value.getAsDistribution().getContents(size, false);
        for (EngineValue innerValue : innerValues) {
          result.add(innerValue.getAsEntity());
        }
      }
    }

    return result;
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
    return StreamSupport.stream(getInnerEntities(target).spliterator(), false)
        .flatMap((x) -> Stream.concat(
            Stream.of(x),
            getInnerEntitiesRecursive(x)
        ));
  }

  /**
   * Get a stream of all descendent Entity instances from attribute values.
   *
   * <p>Get a stream of all descendent Entity instances from attribute values by recursing
   * through all children into their children.</p>
   *
   * @param target in which to find the inner entites
   * @return Stream of Entity instances found in attributes that contain entities after
   *     recursion
   */
  public static Stream<Entity> getInnerFrozenEntitiesRecursive(Entity target) {
    return StreamSupport.stream(getInnerFrozenEntities(target).spliterator(), false)
        .flatMap((x) -> Stream.concat(
            Stream.of(x),
            getInnerFrozenEntitiesRecursive(x)
        ));
  }

}
