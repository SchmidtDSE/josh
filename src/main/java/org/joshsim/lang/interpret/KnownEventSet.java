/**
 * Compile-time record of the event names a Josh program declares.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The set of event names known to be valid for a Josh program at compile time.
 *
 * <p>Josh has two kinds of events. <em>Init</em> events run once when an entity is born: the base
 * {@code "init"} plus, for each {@code create ... through "<origin>"} dispatch target, a per-origin
 * variant event named {@link #initEventFor(String)} (e.g. {@code "init:founding"}).
 * <em>Substep</em> events ({@code "start"}, {@code "step"}, {@code "end"}) run every timestep. Two
 * further names, {@code "constant"} and {@code "remove"}, are structural and always recognized.</p>
 *
 * <p>This is produced by a pre-pass ({@code JoshLangEventSetVisitor}) before the main interpret
 * walk and threaded into the {@code DelegateToolbox} so the visitors can (a) split an attribute
 * name from its trailing event (see {@code JoshFunctionVisitor.isEventName}), (b) decide whether a
 * {@code through "<origin>"} clause has a matching {@code start init through} block, and (c) tell
 * {@code EntityBuilder} which init events to build handler caches and default {@code state}
 * handlers for. Instances are immutable once built; {@link #combine(KnownEventSet)} merges two
 * into a new
 * one.</p>
 */
public class KnownEventSet {

  /** The base init event, run for creates without an origin (or with an unknown origin). */
  public static final String BASE_INIT_EVENT = "init";

  /** Prefix marking a per-origin init variant event; see {@link #initEventFor(String)}. */
  private static final String INIT_THROUGH_PREFIX = "init:";

  /** Structural events that are always valid regardless of what a program declares. */
  private static final Set<String> STRUCTURAL_EVENTS = Set.of("constant", "remove");

  /** The standard per-timestep substep events. */
  private static final Set<String> STANDARD_SUBSTEP_EVENTS = Set.of("start", "step", "end");

  private final Set<String> initEvents;
  private final Set<String> substepEvents;

  /**
   * Create a set seeded with the base init event and the standard substeps.
   */
  public KnownEventSet() {
    initEvents = new HashSet<>();
    initEvents.add(BASE_INIT_EVENT);
    substepEvents = new HashSet<>(STANDARD_SUBSTEP_EVENTS);
  }

  /**
   * Create a set from explicit event collections (used by {@link #combine(KnownEventSet)}).
   *
   * @param initEvents The init events to include.
   * @param substepEvents The substep events to include.
   */
  private KnownEventSet(Set<String> initEvents, Set<String> substepEvents) {
    this.initEvents = new HashSet<>(initEvents);
    this.substepEvents = new HashSet<>(substepEvents);
  }

  /**
   * Get the variant init event name for a creation origin.
   *
   * @param origin The origin from a {@code create ... through "<origin>"} or a
   *     {@code start init through} header.
   * @return The internal init event name (e.g. {@code "init:founding"}). Never parsed back into its
   *     origin; only produced and set-membership-tested.
   */
  public static String initEventFor(String origin) {
    return INIT_THROUGH_PREFIX + origin;
  }

  /**
   * Register a per-origin init variant event.
   *
   * @param origin The origin declared by a {@code start init through "<origin>"} block.
   */
  public void addInitOrigin(String origin) {
    initEvents.add(initEventFor(origin));
  }

  /**
   * Determine whether a name is an init event (base or a declared per-origin variant).
   *
   * @param candidate The event name to test.
   * @return True if {@code candidate} is an init event.
   */
  public boolean isInitEvent(String candidate) {
    return initEvents.contains(candidate);
  }

  /**
   * Determine whether a name is a substep event.
   *
   * @param candidate The event name to test.
   * @return True if {@code candidate} is a substep event.
   */
  public boolean isSubstepEvent(String candidate) {
    return substepEvents.contains(candidate);
  }

  /**
   * Determine whether a name is a recognized event name.
   *
   * <p>True for init events, substep events, and the structural {@code "constant"} and
   * {@code "remove"} events. This is the predicate used to split an attribute name from its
   * trailing event.</p>
   *
   * @param candidate The final dot-separated segment of a handler name.
   * @return True if {@code candidate} names an event rather than part of the attribute name.
   */
  public boolean isEventName(String candidate) {
    return isInitEvent(candidate)
        || isSubstepEvent(candidate)
        || STRUCTURAL_EVENTS.contains(candidate);
  }

  /**
   * Get the init events (base plus declared per-origin variants).
   *
   * @return An unmodifiable view of the init event names.
   */
  public Iterable<String> getInitEvents() {
    return Collections.unmodifiableSet(initEvents);
  }

  /**
   * Get the substep events.
   *
   * @return An unmodifiable view of the substep event names.
   */
  public Iterable<String> getSubstepEvents() {
    return Collections.unmodifiableSet(substepEvents);
  }

  /**
   * Merge this set with another, producing a new combined set.
   *
   * @param other The set to merge in.
   * @return A new KnownEventSet containing the union of both sets' events.
   */
  public KnownEventSet combine(KnownEventSet other) {
    Set<String> combinedInit = new HashSet<>(initEvents);
    Set<String> combinedSubstep = new HashSet<>(substepEvents);
    combinedInit.addAll(other.initEvents);
    combinedSubstep.addAll(other.substepEvents);
    return new KnownEventSet(combinedInit, combinedSubstep);
  }

}
