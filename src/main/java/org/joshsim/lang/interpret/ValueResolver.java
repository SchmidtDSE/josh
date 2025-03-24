
/**
 * Structures to help find values within a scope or nested scopes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Optional;
import java.util.StringJoiner;
import org.joshsim.engine.func.EntityScope;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.EngineValue;

/**
 * Helper which resolves a value within a scope, memoizing the path after resolution.
 *
 * <p>Following the dot chaining pattern of the Josh language definition, helper which resolves a
 * value within a scope, memoizing the path after resolution.</p>
 */
public class ValueResolver {

  private final String path;

  private String foundPath;
  private Optional<ValueResolver> memoizedContinuationResolver;

  /**
   * Creates a new ValueResolver for resolving dot-separated paths.
   *
   * @param path The dot-separated path to resolve (e.g. "entity.attribute")
   */
  public ValueResolver(String path) {
    this.path = path;
    memoizedContinuationResolver = null;
    foundPath = null;
  }

  /**
   * Attempts to get a value from the target scope using the configured path.
   *
   * <p>This method will attempt to resolve as much of the path as possible within the given scope,
   * then recursively resolve any remaining path segments in nested entity scopes.</p>
   *
   * @param target The scope to search for the value
   * @return Optional containing the resolved value if found, empty otherwise
   */
  public Optional<EngineValue> get(Scope target) {
    Optional<ValueResolver> continuationResolverMaybe = getInnerResolver(target);
    if (continuationResolverMaybe == null) {
      return Optional.empty();
    }

    EngineValue resolved = target.get(foundPath);
    if (continuationResolverMaybe.isEmpty()) {
      return Optional.of(resolved);
    } else {
      ValueResolver continuationResolver = continuationResolverMaybe.get();
      return continuationResolver.get(new EntityScope(resolved.getAsEntity()));
    }
  }

  /**
   * Gets or creates a resolver for any remaining path segments after the longest matching prefix.
   *
   * <p>This method attempts to find the longest prefix of the path that exists in the target scope.
   * It then creates a resolver for any remaining path segments if needed.</p>
   *
   * @param target The scope to search for matching path prefixes
   * @return Optional containing a resolver for remaining segments if any, empty if full path matched,
   *     or null if no match found
   */
  private Optional<ValueResolver> getInnerResolver(Scope target) {
    if (memoizedContinuationResolver != null) {
      return memoizedContinuationResolver;
    }

    String[] pieces = path.split("\\.");
    int numPieces = pieces.length;

    for (int numPiecesAttempt = numPieces; numPiecesAttempt > 0; numPiecesAttempt--) {
      StringJoiner attemptJoiner = new StringJoiner(".");

      for (int i = 0; i < numPiecesAttempt; i++) {
          attemptJoiner.add(pieces[i]);
      }

      String attemptPath = attemptJoiner.toString();

      if (target.has(attemptPath)) {
        foundPath = attemptPath;

        if (numPiecesAttempt == numPieces) {
          memoizedContinuationResolver = Optional.empty();
        } else {
          StringJoiner remainingJoiner = new StringJoiner(".");
          for (int i = numPiecesAttempt; i < numPieces; i++) {
            remainingJoiner.add(pieces[i]);
          }
          String remainingPath = remainingJoiner.toString();
          memoizedContinuationResolver = Optional.of(new ValueResolver(remainingPath));
        }
      }
    }

    return memoizedContinuationResolver;
  }
}
