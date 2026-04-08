/**
 * Structures to help find values within a scope or nested scopes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.ArrayList;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;
import org.joshsim.engine.func.DistributionScope;
import org.joshsim.engine.func.EntityScope;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
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

  private final ValueSupportFactory valueFactory;
  private final String path;
  private final boolean hasDot;
  private final boolean isEvalDuration;
  private final EngineValue zeroMilliseconds;

  private String foundPath;
  private Optional<ValueResolver> memoizedContinuationResolver;

  /**
   * Creates a new RecursiveValueResolver for resolving dot-separated paths.
   *
   * @param valueFactory The value factory to use in constructing returned and supporting values.
   * @param path The dot-separated path to resolve (e.g. "entity.attribute").
   */
  public RecursiveValueResolver(ValueSupportFactory valueFactory, String path) {
    this.valueFactory = valueFactory;
    this.path = path;
    this.hasDot = getHasDot(path);
    this.isEvalDuration = EVAL_DURATION_ATTR.equals(path);
    this.zeroMilliseconds = valueFactory.build(0L, Units.MILLISECONDS);
    memoizedContinuationResolver = null;
    foundPath = null;
  }

  /**
   * Determines whether the given path contains a dot separator.
   *
   * <p>Used to decide whether the fast integer-indexed lookup can be applied. If the path
   * contains a dot, the path refers to a nested attribute and must be resolved recursively
   * rather than via the direct index cache.</p>
   *
   * @param path The dot-separated attribute path to examine, may be null.
   * @return True if the path is non-null and contains at least one dot character, false otherwise.
   */
  private static boolean getHasDot(String path) {
    return path != null && path.indexOf('.') != -1;
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
      return Optional.of(zeroMilliseconds);
    }

    if (!hasDot) {
      Optional<EngineValue> fastResult = target.tryIndexedGet(path);
      if (fastResult.isPresent()) {
        return fastResult;
      }
    }

    Optional<ValueResolver> continuationResolverMaybe = getInnerResolver(target);
    if (continuationResolverMaybe == null) {
      return Optional.empty();
    }

    EngineValue resolved = target.get(foundPath);
    if (continuationResolverMaybe.isEmpty()) {
      return Optional.of(resolved);
    } else {
      ValueResolver continuationResolver = continuationResolverMaybe.get();

      checkUndefinedSize(resolved);

      return resolveBasedOnSize(resolved, continuationResolver, target);
    }
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
   * Checks that the resolved value has a defined size, throwing if it does not.
   *
   * <p>A value with an undefined size is a distribution or type whose cardinality cannot be
   * determined at resolution time. Attempting to resolve attributes within such a value is
   * not supported because no appropriate scope can be constructed for the continuation
   * resolver.</p>
   *
   * @param resolved the EngineValue whose size to check.
   * @throws IllegalArgumentException if the size of {@code resolved} is undefined.
   */
  private void checkUndefinedSize(EngineValue resolved) {
    if (resolved.getSize().isEmpty()) {
      String message = String.format(
          "Cannot resolve attributes in %s as it is a distribution or type of undefined size.",
          resolved.getLanguageType()
      );
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Resolves the continuation path based on the size of the already-resolved value.
   *
   * <p>Three cases are handled based on the size of {@code resolved}:</p>
   * <ul>
   *   <li>Size&nbsp;==&nbsp;1: the value represents a single entity. If the continuation path is
   *       {@code evalDuration}, the continuation resolver is invoked on the original outer scope
   *       to return the 0&nbsp;ms sentinel value, since {@code evalDuration} is a reserved
   *       attribute that always returns 0&nbsp;ms regardless of the entity type. Otherwise, an
   *       {@link EntityScope} is constructed around the entity and the continuation resolver is
   *       invoked on it.</li>
   *   <li>Size&nbsp;==&nbsp;0: the value represents an empty distribution. An empty
   *       {@link RealizedDistribution} is returned immediately without further resolution.</li>
   *   <li>Size&nbsp;&gt;&nbsp;1: the value represents a non-empty distribution. A
   *       {@link DistributionScope} is constructed and the continuation resolver is invoked
   *       on it.</li>
   * </ul>
   *
   * @param resolved the resolved EngineValue whose size determines the scope type.
   * @param continuationResolver the resolver for remaining path segments.
   * @param target the original outer scope, used for the {@code evalDuration} fast-path.
   * @return Optional containing the final resolved value.
   */
  private Optional<EngineValue> resolveBasedOnSize(
      EngineValue resolved,
      ValueResolver continuationResolver,
      Scope target) {
    int size = resolved.getSize().get();
    Scope newScope;
    if (size == 1) {
      if (EVAL_DURATION_ATTR.equals(continuationResolver.getPath())) {
        return continuationResolver.get(target);
      }
      newScope = new EntityScope(resolved.getAsEntity());
    } else if (size == 0) {
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
              valueFactory.buildValueResolver(remainingPath)
          );
        }

        return memoizedContinuationResolver;
      }
    }

    return null;
  }
}
