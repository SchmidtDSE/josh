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
 * Strategy which resolves a value within a scope, memoizing the path after resolution.
 */
public class ValueResolver {

  private final String path;
  
  private String foundPath;
  private Optional<ValueResolver> memoizedInnerResolver;

  public ValueResolver(String path) {
    this.path = path;
    memoizedInnerResolver = null;
    foundPath = null;
  }

  public Optional<EngineValue> get(Scope target) {
    Optional<ValueResolver> innerResolverMaybe = getInnerResolver(target);
    if (innerResolverMaybe == null) {
      return Optional.empty();
    }

    EngineValue resolved = target.get(foundPath);
    if (innerResolverMaybe.isEmpty()) {
      return Optional.of(resolved);
    } else {
      ValueResolver innerResolver = innerResolverMaybe.get();
      return innerResolver.get(new EntityScope(resolved));
    }
  }

  private Optional<ValueResolver> getInnerResolver(Scope target) {
    if (memoizedInnerResolver != null) {
      return memoizedInnerResolver;
    }
    
    String[] pieces = path.split("\\.");
    int numPieces = numPieces;
    
    for (int numPiecesAttempt = pieces.length; numPiecesAttempt > 0; numPiecesAttempt--) {
      StringJoiner attemptJoiner = new StringJoiner(".");
      
      for (int i = 0; i < numPiecesAttempt; i++) {
          attemptJoiner.add(pieces[i]);
      }
      
      String attemptPath = attemptJoiner.toString();
      
      if (target.has(attemptPath)) {
        foundPath = attemptPath;

        if (numPiecesAttempt == pieces.length) {
          memoizedInnerResolver = Optional.empty();
        } else {
          StringJoiner remainingJoiner = new StringJoiner(".");
          for (int i = numPiecesAttempt; i < pieces.length; i++) {
            remainingJoiner.add(pieces[i]);
          }
          String remainingPath = remainingJoiner.toString();
          memoizedInnerResolver = Optional.of(new ValueResolver(remainingPath));
        }
      }
    }

    return memoizedInnerResolver;
  }

}
