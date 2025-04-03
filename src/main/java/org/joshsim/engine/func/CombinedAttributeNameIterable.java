package org.joshsim.engine.func;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An iterable that combines two iterables of attribute names.
 */
public class CombinedAttributeNameIterable implements Iterable<String> {
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