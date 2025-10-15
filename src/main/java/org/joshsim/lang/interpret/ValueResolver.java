/**
 * Structures to help find values within a scope or nested scopes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;
import org.joshsim.engine.func.DistributionScope;
import org.joshsim.engine.func.EntityScope;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.RealizedDistribution;

/**
 * Helper which resolves a value within a scope, memoizing the path after resolution.
 *
 * <p>Following the dot chaining pattern of the Josh language definition, helper which resolves a
 * value within a scope, memoizing the path after resolution.</p>
 */
public class ValueResolver {

  private final EngineValueFactory valueFactory;
  private final String path;
  private final boolean hasDot;

  private String foundPath;
  private Optional<ValueResolver> memoizedContinuationResolver;

  // Cache maps from the shared attributeNameToIndex Map reference to the attribute's index.
  // The Map object identity serves as a stand-in for entity type name (e.g., "JoshuaTree")
  // without the overhead of String hashing. All entities of the same type share the same
  // immutable attributeNameToIndex Map instance, making it perfect for identity-based caching.
  private IdentityHashMap<Map<String, Integer>, Integer> indexCache;

  /**
   * Creates a new ValueResolver for resolving dot-separated paths.
   *
   * @param valueFactory The value factory to use in constructing returned and supporting values.
   * @param path The dot-separated path to resolve (e.g. "entity.attribute").
   */
  public ValueResolver(EngineValueFactory valueFactory, String path) {
    this.valueFactory = valueFactory;
    this.path = path;
    this.hasDot = path != null && path.indexOf('.') != -1;
    memoizedContinuationResolver = null;
    foundPath = null;
    indexCache = null; // Initialized lazily
  }

  /**
   * Attempts to get a value from the target scope using the configured path.
   *
   * <p>This method will attempt to resolve as much of the path as possible within the given scope,
   * then recursively resolve any remaining path segments in nested entity scopes.</p>
   *
   * @param target The scope to search for the value.
   * @return Optional containing the resolved value if found, empty otherwise.
   */
  public Optional<EngineValue> get(Scope target) {
    // Try integer-based access for EntityScope
    if (target instanceof EntityScope) {
      EntityScope entityScope = (EntityScope) target;
      Optional<EngineValue> fastResult = tryIntegerLookup(entityScope);
      if (fastResult != null) {
        return fastResult;
      }
    }

    // Original string-based resolution
    Optional<ValueResolver> continuationResolverMaybe = getInnerResolver(target);
    if (continuationResolverMaybe == null) {
      return Optional.empty();
    }

    EngineValue resolved = target.get(foundPath);
    if (continuationResolverMaybe.isEmpty()) {
      return Optional.of(resolved);
    } else {
      ValueResolver continuationResolver = continuationResolverMaybe.get();
      Optional<Integer> innerSize = resolved.getSize();

      if (innerSize.isEmpty()) {
        String message = String.format(
            "Cannot resolve attributes in %s as it is a distribution or type of undefined size.",
            resolved.getLanguageType()
        );
        throw new IllegalArgumentException(message);
      }

      Scope newScope;
      if (innerSize.get() == 1) {
        newScope = new EntityScope(resolved.getAsEntity());
      } else if (innerSize.get() == 0) {
        return Optional.of(new RealizedDistribution(
            resolved.getCaster(),
            new ArrayList<>(),
            Units.EMPTY
        ));
      } else {
        newScope = new DistributionScope(valueFactory, resolved.getAsDistribution());
      }

      return continuationResolver.get(newScope);
    }
  }

  /**
   * Attempts to resolve the attribute using cached integer index.
   *
   * <p>This method implements the fast path for attribute resolution by caching
   * the integer index for each entity type (identified by its attributeNameToIndex map).
   * The cache uses IdentityHashMap to distinguish between different entity types,
   * allowing the same ValueResolver to efficiently handle multiple entity types.</p>
   *
   * <p>Fast path only works for simple attribute names (no dots). Dotted paths like
   * "entity.attribute" fall back to the slow path.</p>
   *
   * @param entityScope The EntityScope to resolve from
   * @return Optional containing the resolved value if fast path succeeded,
   *         null if fast path cannot be used (caller should use slow path)
   */
  private Optional<EngineValue> tryIntegerLookup(EntityScope entityScope) {
    // Fast path only works for simple attribute names (no nested paths)
    if (hasDot) {
      return null; // Use slow path
    }

    // Get the entity type's index map (shared across all instances of this type)
    // The Map object identity serves as the cache key for the entity type
    Map<String, Integer> indexMap = entityScope.getAttributeNameToIndex();

    // Handle null or empty distributions (no attributes)
    if (indexMap == null || indexMap.isEmpty()) {
      return null; // Use slow path for safety
    }

    // Initialize cache on first use
    if (indexCache == null) {
      indexCache = new IdentityHashMap<>();
    }

    // Check cache (identity-based, so different entity types are separate)
    Integer cachedIndex = indexCache.get(indexMap);

    if (cachedIndex != null) {
      // Use cached index for fast array access
      return entityScope.getOptional(cachedIndex);
    }

    // Look up the index and cache it
    Integer index = indexMap.get(path);
    if (index == null) {
      // Attribute doesn't exist on this entity type
      return null; // Use slow path (will properly handle error)
    }

    // Cache the index for future lookups
    indexCache.put(indexMap, index);

    // Use the newly cached index
    return entityScope.getOptional(index);
  }

  @Override
  public String toString() {
    return String.format("ValueResolver(%s)", path);
  }

  /**
   * Gets or creates a resolver for any remaining path segments after the longest matching prefix.
   *
   * <p>This method attempts to find the longest prefix of the path that exists in the target scope.
   * It then creates a resolver for any remaining path segments if needed. This is required because
   * some attributes may appear nested but not actually within an inner scope. This may be beacuse
   * they are saved on the outer scope like for steps.low and steps.high which are within
   * Simulation. The "nesting" is simply syntatic sugar in this case for the Josh language.</p>
   *
   * @param target The scope to search for matching path prefixes.
   * @return Optional containing a resolver for remaining segments if any, empty if full path.
   *     matched on the root, or null if no match found. If null, this should try resolution again
   *     on the next request.
   */
  private Optional<ValueResolver> getInnerResolver(Scope target) {
    if (memoizedContinuationResolver != null) {
      return memoizedContinuationResolver;
    }

    String[] pieces = path.split("\\.");
    int numPieces = pieces.length;

    for (int numPiecesAttempt = numPieces; numPiecesAttempt > 0; numPiecesAttempt--) {
      CompatibleStringJoiner attemptJoiner = CompatibilityLayerKeeper.get().createStringJoiner(".");

      for (int i = 0; i < numPiecesAttempt; i++) {
        attemptJoiner.add(pieces[i]);
      }

      String attemptPath = attemptJoiner.toString();

      if (target.has(attemptPath)) {
        foundPath = attemptPath;

        if (numPiecesAttempt == numPieces) {
          memoizedContinuationResolver = Optional.empty();
        } else {
          CompatibleStringJoiner remainingJoiner = CompatibilityLayerKeeper
              .get()
              .createStringJoiner(".");

          for (int i = numPiecesAttempt; i < numPieces; i++) {
            remainingJoiner.add(pieces[i]);
          }
          String remainingPath = remainingJoiner.toString();
          memoizedContinuationResolver = Optional.of(
              new ValueResolver(valueFactory, remainingPath)
          );
        }

        return memoizedContinuationResolver;
      }
    }

    return null;
  }
}
