/**
 * Structures to help find values within a scope or nested scopes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Optional;
import org.joshsim.engine.func.Scope;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;



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
    Optional<ValueResolver> innerResolver = getInnerResolver(target);
    if (innerResolver == null) {
      return Optional.empty();
    }

    
  }

  private Optional<ValueResolver> getInnerResolver(Scope target) {
    if (memoizedInnerResolver != null) {
      return memoizedInnerResolver;
    }
    
    String[] pieces = target.split(".");
    int numPieces = numPieces
    
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
