/**
 * Compile-time record of the event names a Josh program declares.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The set of event names known to be valid for a Josh program at compile time.
 *
 * <p>Josh has two kinds of events. <em>Init</em> events run once when an entity is born: the base
 * {@code "init"} plus, for each {@code create ... through "<origin>"} dispatch target, a per-origin
 * variant event named {@link #initEventFor(String)} (e.g. {@code "init:founding"}). A per-origin
 * variant is declared by a {@code start init through} block on a specific entity stanza, so it is
 * only ever valid for the entity type that declared it — {@code Shrub} does not gain an
 * {@code init:founding} event just because {@code Tree} declared one. <em>Substep</em> events run
 * every timestep and default to {@code "start"}, {@code "step"}, {@code "end"}, but a simulation
 * may replace them with its own ordered, named phases via a {@code start phases ... end phases}
 * block (see {@link #declarePhases(List)}); this is program-wide, not per-entity.</p>
 *
 * <p>This is produced by a pre-pass ({@code JoshLangEventSetVisitor}) before the main interpret
 * walk and threaded into the {@code DelegateToolbox} so the visitors can (a) split an attribute
 * name from its trailing event (see {@code JoshFunctionVisitor.isEventName}) — a check that only
 * ever sees naked identifiers and so never needs per-origin variants, since a colon can never
 * appear in one — (b) decide, for the specific entity type being created, whether a
 * {@code through "<origin>"} clause has a matching {@code start init through} block on that same
 * entity, and (c) tell {@code EntityBuilder} which of its own declared init events to build handler
 * caches and default {@code state} handlers for. Instances are immutable once built;
 * {@link #combine(KnownEventSet)} merges two into a new one.</p>
 */
public class KnownEventSet {

  /** The base init event, run for creates without an origin (or with an unknown origin). */
  public static final String BASE_INIT_EVENT = "init";

  /** Prefix marking a per-origin init variant event; see {@link #initEventFor(String)}. */
  private static final String INIT_THROUGH_PREFIX = "init:";

  /** Structural events that are always valid regardless of what a program declares. */
  private static final Set<String> STRUCTURAL_EVENTS = Set.of("constant", "remove");

  /** The default per-timestep substep order, used unless a simulation declares its own phases. */
  private static final List<String> DEFAULT_SUBSTEP_ORDER = List.of("start", "step", "end");

  /** Per-entity-type declared init variant events (e.g. {@code "Tree" -> {"init:founding"}}). */
  private final Map<String, Set<String>> initEventsByEntity;

  private Optional<List<String>> customSubstepOrder;

  /**
   * Create a set seeded with the standard substeps and no declared init origins.
   */
  public KnownEventSet() {
    initEventsByEntity = new HashMap<>();
    customSubstepOrder = Optional.empty();
  }

  /**
   * Create a set from explicit event collections (used by {@link #combine(KnownEventSet)}).
   *
   * @param initEventsByEntity The per-entity declared init variant events to include.
   * @param customSubstepOrder The declared phase order to include, if any.
   */
  private KnownEventSet(Map<String, Set<String>> initEventsByEntity,
        Optional<List<String>> customSubstepOrder) {
    this.initEventsByEntity = new HashMap<>();
    for (Map.Entry<String, Set<String>> entry : initEventsByEntity.entrySet()) {
      this.initEventsByEntity.put(entry.getKey(), new HashSet<>(entry.getValue()));
    }
    this.customSubstepOrder = customSubstepOrder;
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
   * Register a per-origin init variant event declared by a specific entity type.
   *
   * @param entityType The entity type (e.g. {@code "Tree"}) whose stanza declared the
   *     {@code start init through "<origin>"} block.
   * @param origin The origin declared by that block.
   */
  public void addInitOrigin(String entityType, String origin) {
    initEventsByEntity.computeIfAbsent(entityType, key -> new HashSet<>())
        .add(initEventFor(origin));
  }

  /**
   * Declare the ordered phase sequence a {@code start phases ... end phases} block names, replacing
   * the default {@code start}/{@code step}/{@code end} substeps for the whole program.
   *
   * @param order The phase names in declaration order.
   * @throws IllegalStateException if phases have already been declared elsewhere in the program.
   * @throws IllegalArgumentException if a phase name is reserved or repeated.
   */
  public void declarePhases(List<String> order) {
    if (customSubstepOrder.isPresent()) {
      throw new IllegalStateException(
          "Only one simulation may declare a `phases` block per program.");
    }

    Set<String> seen = new HashSet<>();
    for (String phase : order) {
      if (BASE_INIT_EVENT.equals(phase) || STRUCTURAL_EVENTS.contains(phase)) {
        throw new IllegalArgumentException(
            String.format("Cannot use \"%s\" as a phase name; it is reserved.", phase));
      }
      if (!seen.add(phase)) {
        throw new IllegalArgumentException(
            String.format("Phase \"%s\" declared more than once.", phase));
      }
    }

    customSubstepOrder = Optional.of(List.copyOf(order));
  }

  /**
   * Determine whether a name is a recognized init event name (base only).
   *
   * <p>Used only for the naked-identifier discovery check in
   * {@code JoshFunctionVisitor.isEventName}: a candidate there is always a bare identifier segment,
   * which can never contain the {@code ":"} that marks a per-origin variant, so only the base event
   * is ever relevant here. Use {@link #isInitEvent(String, String)} to check whether a specific
   * entity type supports a given (possibly variant) init event.</p>
   *
   * @param candidate The event name to test.
   * @return True if {@code candidate} is the base init event.
   */
  public boolean isInitEvent(String candidate) {
    return BASE_INIT_EVENT.equals(candidate);
  }

  /**
   * Determine whether a specific entity type recognizes an init event.
   *
   * <p>True for the base init event (always available) or a per-origin variant that entity type's
   * own {@code start init through} block declared. False for a variant declared by a
   * <em>different</em> entity type — origin dispatch must not resolve across entity types.</p>
   *
   * @param entityType The entity type being created (e.g. {@code "Tree"}).
   * @param candidate The init event to test (base or a variant from {@link #initEventFor}).
   * @return True if {@code candidate} is valid for {@code entityType}.
   */
  public boolean isInitEvent(String entityType, String candidate) {
    if (BASE_INIT_EVENT.equals(candidate)) {
      return true;
    }
    Set<String> declared = initEventsByEntity.get(entityType);
    return declared != null && declared.contains(candidate);
  }

  /**
   * Determine whether a name is a substep event.
   *
   * @param candidate The event name to test.
   * @return True if {@code candidate} is a substep event.
   */
  public boolean isSubstepEvent(String candidate) {
    return getSubstepOrder().contains(candidate);
  }

  /**
   * Determine whether a name is a recognized event name.
   *
   * <p>True for the base init event, substep events, and the structural {@code "constant"} and
   * {@code "remove"} events. This is the predicate used to split an attribute name from its
   * trailing event; per-origin init variants never appear here since they can only be produced
   * from a quoted string literal, never from a naked identifier segment.</p>
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
   * Get the init events a specific entity type may be born through.
   *
   * @param entityType The entity type (e.g. {@code "Tree"}).
   * @return The base init event plus any per-origin variants that entity type declared. Never
   *     includes another entity type's variants.
   */
  public Iterable<String> getInitEvents(String entityType) {
    Set<String> result = new HashSet<>();
    result.add(BASE_INIT_EVENT);
    Set<String> declared = initEventsByEntity.get(entityType);
    if (declared != null) {
      result.addAll(declared);
    }
    return Collections.unmodifiableSet(result);
  }

  /**
   * Get the ordered substep sequence: the program's declared phases, or the default
   * {@code start}/{@code step}/{@code end} if none were declared.
   *
   * @return The ordered substep names.
   */
  public List<String> getSubstepOrder() {
    return customSubstepOrder.orElse(DEFAULT_SUBSTEP_ORDER);
  }

  /**
   * Determine whether a candidate is a default substep name left over from before this program
   * declared its own phases.
   *
   * <p>Once a {@code start phases ... end phases} block replaces the default substep order,
   * {@code start}/{@code step}/{@code end} are no longer valid trailing event segments. Without
   * this check, a handler name like {@code value.step} written before the migration would
   * silently fold {@code "step"} into the attribute name instead of failing, since
   * {@link #isEventName(String)} simply returns false for it like any other non-event segment.
   * This lets callers tell that specific, likely-unintentional case apart from a genuine
   * multi-part attribute name.</p>
   *
   * @param candidate The final dot-separated segment of a handler name.
   * @return True if {@code candidate} is one of {@code start}/{@code step}/{@code end} and this
   *     program declared custom phases that do not include it.
   */
  public boolean isStaleDefaultSubstep(String candidate) {
    return customSubstepOrder.isPresent() && DEFAULT_SUBSTEP_ORDER.contains(candidate);
  }

  /**
   * Merge this set with another, producing a new combined set.
   *
   * @param other The set to merge in.
   * @return A new KnownEventSet containing the union of both sets' events, keeping each entity
   *     type's declared init variants separate from every other entity type's.
   * @throws IllegalStateException if both sides declare a (necessarily different) phase order.
   */
  public KnownEventSet combine(KnownEventSet other) {
    Map<String, Set<String>> combinedInit = new HashMap<>();
    for (Map.Entry<String, Set<String>> entry : initEventsByEntity.entrySet()) {
      combinedInit.computeIfAbsent(entry.getKey(), key -> new HashSet<>()).addAll(entry.getValue());
    }
    for (Map.Entry<String, Set<String>> entry : other.initEventsByEntity.entrySet()) {
      combinedInit.computeIfAbsent(entry.getKey(), key -> new HashSet<>()).addAll(entry.getValue());
    }

    if (customSubstepOrder.isPresent() && other.customSubstepOrder.isPresent()) {
      throw new IllegalStateException(
          "Only one simulation may declare a `phases` block per program.");
    }
    Optional<List<String>> combinedOrder = customSubstepOrder.isPresent()
        ? customSubstepOrder
        : other.customSubstepOrder;

    return new KnownEventSet(combinedInit, combinedOrder);
  }

}
