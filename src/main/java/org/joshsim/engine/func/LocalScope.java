/**
 * Structures to describe a temporary local scope.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Decorator creating a temporary local local variable scope within an enclosing scope.
 *
 * <p>Structure which allows for accessing an enclosing scope which is immutable while also being
 * able to create local variables which are lost after the decorator is over. This only supports
 * creation of local constants so values cannot be written again after creation.</p>
 */
public class LocalScope implements Scope {

  private final Map<String, EngineValue> localValues;
  private final Scope containingScope;

  /**
   * Creates a new local scope with an enclosing scope.
   *
   * @param containingScope The enclosing scope that this local scope will delegate to when a value
   *     is not found locally.
   */
  public LocalScope(Scope containingScope) {
    this.containingScope = containingScope;
    localValues = new HashMap<>();
  }

  /**
   * Get a value within this scope.
   *
   * @param name of the attribute which must be accessible on this scope's root.
   */
  @Override
  public EngineValue get(String name) {
    if (localValues.containsKey(name)) {
      return localValues.get(name);
    } else {
      return containingScope.get(name);
    }
  }

  /**
   * Check if a value is within this scope.
   *
   * @param name of the attribute to look for.
   * @return true if present and false otherwise.
   */
  @Override
  public boolean has(String name) {
    if (localValues.containsKey(name)) {
      return true;
    } else {
      return containingScope.has(name);
    }
  }

  /**
   * Defines a constant value in the local scope.
   *
   * @param name The name of the constant to define within this LocalScope.
   * @param value The value to associate with the constant within this LocalScope which will be lost
   *     after the conclusion of this LocalScope.
   * @throws RuntimeException if a variable with the given name already exists in this scope, either
   *     locally or in the containing scope.
   */
  public void defineConstant(String name, EngineValue value) {
    if (has(name)) {
      String message = String.format("The variable %s already exists in this scope.", name);
      throw new RuntimeException(message);
    }

    localValues.put(name, value);
  }

  /**
   * Determine what values are on this scope.
   *
   * @return all attributes within this scope.
   */
  @Override
  public Iterable<String> getAttributes() {
    Iterable<String> outerAttributes = containingScope.getAttributes();
    Iterable<String> innerAttributes = localValues.keySet();
    return new CombinedAttributeNameIterable(outerAttributes, innerAttributes);
  }

  /**
   * An iterable that combines two iterables of attribute names.
   */
  private class CombinedAttributeNameIterable implements Iterable<String> {
    private final Iterable<String> firstIterable;
    private final Iterable<String> secondIterable;

    /**
     * Creates a new iterable that combines two existing iterables of attribute names.
     *
     * @param firstIterable The first iterable to traverse in return iterators.
     * @param secondIterable The second iterable to traverse after the first one is complete.
     */
    public CombinedAttributeNameIterable(Iterable<String> firstIterable,
        Iterable<String> secondIterable) {
      this.firstIterable = firstIterable;
      this.secondIterable = secondIterable;
    }

    @Override
    public Iterator<String> iterator() {
      return new Iterator<String>() {
        private final Iterator<String> firstIterator = firstIterable.iterator();
        private final Iterator<String> secondIterator = secondIterable.iterator();
        
        @Override
        public boolean hasNext() {
          return firstIterator.hasNext() || secondIterator.hasNext();
        }

        @Override
        public String next() {
          if (firstIterator.hasNext()) {
            return firstIterator.next();
          } else if (secondIterator.hasNext()) {
            return secondIterator.next();
          }
          throw new NoSuchElementException();
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

}
