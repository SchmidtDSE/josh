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
 * Recursive implementation of ValueResolver which resolves dot-separated paths by recursion.
 *
 * <p>Following the dot chaining pattern of the Josh language definition, this implementation
 * resolves a value within a scope, memoizing the path after resolution. It recurses on each
 * dot-separated segment of the path to find the final value.</p>
 */
public class RecursiveValueResolver implements ValueResolver {

  private static final String EVAL_DURATION_ATTR = "evalDuration";

  private final EngineValueFactory valueFactory;
  private final String path;
  private final boolean hasDot;
  private final boolean isEvalDuration;

  private String foundPath;
  private Optional<RecursiveValueResolver> memoizedContinuationResolver;

  private IdentityHashMap<Map<String, Integer>, Integer> indexCache;

  /**
   * Creates a new RecursiveValueResolver for resolving dot-separated paths.
   *
   * @param valueFactory The value factory to use in constructing returned and supporting values.
   * @param path The dot-separated path to resolve (e.g. "entity.attribute").
   */
  public RecursiveValueResolver(EngineValueFactory valueFactory, String path) {
    this.valueFactory = valueFactory;
    this.path = path;
    this.hasDot = path != null && path.indexOf('.') != -1;
    this.isEvalDuration = EVAL_DURATION_ATTR.equals(path);
    memoizedContinuationResolver = null;
    foundPath = null;
    indexCache = null;
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
  @Override
  public Optional<EngineValue> get(Scope target) {
    if (isEvalDuration) {
      return Optional.of(valueFactory.build(0L, Units.of("milliseconds")));
    }

    if (target instanceof EntityScope) {
      EntityScope entityScope = (EntityScope) target;
      Optional<EngineValue> fastResult = tryIntegerLookup(entityScope);
      if (fastResult != null) {
        return fastResult;
      }
    }

    Optional<RecursiveValueResolver> continuationResolverMaybe = getInnerResolver(target);
    if (continuationResolverMaybe == null) {
      return Optional.empty();
    }

    EngineValue resolved = target.get(foundPath);
    if (continuationResolverMaybe.isEmpty()) {
      return Optional.of(resolved);
    } else {
      RecursiveValueResolver continuationResolver = continuationResolverMaybe.get();
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
   * Attempts to resolve the attribute using a cached integer index for fast array access.
   *
   * <p>The cache maps each entity type's shared {@code attributeNameToIndex} map reference
   * to the attribute's integer index. Map object identity serves as a stand-in for the entity
   * type name without the overhead of String hashing, since all instances of a given entity type
   * share the same immutable index map. Only simple (non-dotted) paths use this fast path.</p>
   *
   * @param entityScope The EntityScope to resolve from.
   * @return Optional containing the resolved value if the fast path succeeded,
   *         or null if the fast path cannot be used (caller should fall back to slow path).
   */
  private Optional<EngineValue> tryIntegerLookup(EntityScope entityScope) {
    if (hasDot) {
      return null;
    }

    Map<String, Integer> indexMap = entityScope.getAttributeNameToIndex();

    if (indexMap == null || indexMap.isEmpty()) {
      return null;
    }

    if (indexCache == null) {
      indexCache = new IdentityHashMap<>();
    }

    Integer cachedIndex = indexCache.get(indexMap);

    if (cachedIndex != null) {
      return entityScope.getOptional(cachedIndex);
    }

    Integer index = indexMap.get(path);
    if (index == null) {
      return null;
    }

    indexCache.put(indexMap, index);
    return entityScope.getOptional(index);
  }

  /**
   * Get the path being resolved.
   *
   * @return the dot-separated path string.
   */
  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String toString() {
    return String.format("RecursiveValueResolver(%s)", path);
  }

  /**
   * Gets or creates a resolver for any remaining path segments after the longest matching prefix.
   *
   * <p>This method attempts to find the longest prefix of the path that exists in the target scope.
   * It then creates a resolver for any remaining path segments if needed. This is required because
   * some attributes may appear nested but not actually within an inner scope. This may be because
   * they are saved on the outer scope like for steps.low and steps.high which are within
   * Simulation. The "nesting" is simply syntactic sugar in this case for the Josh language.</p>
   *
   * @param target The scope to search for matching path prefixes.
   * @return Optional containing a resolver for remaining segments if any, empty if full path.
   *     matched on the root, or null if no match found. If null, this should try resolution again
   *     on the next request.
   */
  private Optional<RecursiveValueResolver> getInnerResolver(Scope target) {
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
              new RecursiveValueResolver(valueFactory, remainingPath)
          );
        }

        return memoizedContinuationResolver;
      }
    }

    return null;
  }
}
