/**
 * Strategy implementation through widening only conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.engine;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.joshsim.engine.value.type.Distribution;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.RealizedDistribution;


/**
 * Utility to slice a distribution by another distribution.
 *
 * <p>Utility to slice a distribution called a subject by another distribution called a selection
 * where the selection is a set of boolean values of the same size as the subject.</p>
 */
public class Slicer {

  /**
   * Slice a subject by a set of selections.
   *
   * <p>Utility to slice a distribution called a subject by another distribution called a selection
   * where the selection is a set of boolean values of the same size as the subject. The slice
   * operation will only return values which pairwise correspond to true.</p>
   */
  public EngineValue slice(EngineValue subject, EngineValue selections) {
    Distribution subjectDistribution = subject.getAsDistribution();
    int subjectSize = subjectDistribution.getSize().orElseThrow();

    Distribution selectionsDistribution = selections.getAsDistribution();
    int selectionsSize = selectionsDistribution.getSize().orElseThrow();

    if (subjectSize != selectionsSize) {
      String message = String.format(
          "Cannot slice a subject of size %d with %d size selection.",
          subjectSize,
          selectionsSize
      );
      throw new IllegalArgumentException(message);
    }

    Iterable<EngineValue> subjectIterable = subjectDistribution.getContents(
        subjectSize,
        false
    );

    Iterable<EngineValue> selectionsIterable = selectionsDistribution.getContents(
        selectionsSize,
        false
    );

    Iterable<Optional<EngineValue>> sliceIterator = () -> new SliceIterator(
        subjectIterable.iterator(),
        selectionsIterable.iterator()
    );

    List<EngineValue> values = StreamSupport.stream(sliceIterator.spliterator(), false)
        .filter((x) -> x.isPresent())
        .map((x) -> x.get())
        .collect(Collectors.toList());

    return new RealizedDistribution(subject.getCaster(), values, subject.getUnits());
  }

  /**
   * Iterator which filters elements from a subject based on paired elements from selections.
   */
  private class SliceIterator implements Iterator<Optional<EngineValue>> {

    private final Iterator<EngineValue> subjects;
    private final Iterator<EngineValue> selections;

    /**
     * Create a pairwise iterator.
     *
     * @param subjects The iterator over the subjects to filtered by the selections.
     * @param selection The iterator over the selections by which to filter the subjects.
     */
    public SliceIterator(Iterator<EngineValue> subjects, Iterator<EngineValue> selections) {
      this.subjects = subjects;
      this.selections = selections;
    }

    @Override
    public boolean hasNext() {
      return this.subjects.hasNext();
    }

    @Override
    public Optional<EngineValue> next() {
      EngineValue subject = this.subjects.next();
      EngineValue selection = this.selections.next();
      boolean selectionBool = selection.getAsBoolean();
      return selectionBool ? Optional.of(subject) : Optional.empty();
    }

  }

}
