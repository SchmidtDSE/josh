/**
 * Structures for handling query for present or prior state as it resolves across substeps.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.engine.func.EntityScope;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Structure which allows for querying prior or current state of a SpatialEntity as it resolves.
 *
 * <p>Structure which allows for querying prior or current state of a SpatialEntity and allows for
 * determining if an attribute value has been resolved over time. This manages reference to
 * current, prior, and here. Current can be used for values set in the current timestep and substep
 * or to determine if just in time evaluation is required if it not yet been resolved. Prior can be
 * used to query for previously resolved values. Here can be used to access the Patch or patch-like
 * entity which houses this entity.</p>
 */
public class ShadowingEntity implements MutableEntity {

  private static final String DEFAULT_STATE_STR = "";
  private static final boolean ASSERT_VALUE_PRESENT_DEBUG = false;

  private static final boolean DEBUG_ORGANISM =
      Boolean.getBoolean("josh.debug.organism");

  private void debugLog(String message) {
    if (DEBUG_ORGANISM) {
      int timestep = SimulationStepper.getCurrentTimestep();
      System.err.println("[ORGANISM-DEBUG] timestep=" + timestep + " ShadowingEntity " + message);
    }
  }

  private final EngineValueFactory valueFactory;
  private final MutableEntity inner;
  private final Entity here;
  private final Entity meta;
  private final Scope scope;
  private final Map<String, List<EventHandlerGroup>> commonHandlerCache;

  // Array-based caching for resolved values and circular dependency tracking
  private final EngineValue[] resolvedCacheByIndex;
  private final boolean[] resolvingByIndex;

  private boolean checkAssertions;

  /**
   * Create a new ShadowingEntity for a Patch or Simulation.
   *
   * @param valueFactory Factory for creating EngineValue objects drived from this entity.
   * @param inner Entity to decorate.
   * @param meta Reference to simulation or simulation-like entity. May be self.
   */
  public ShadowingEntity(EngineValueFactory valueFactory, MutableEntity inner, Entity meta) {
    this.valueFactory = valueFactory;
    this.inner = inner;
    this.here = this;
    this.meta = meta;

    Optional<EngineValue> checkAssertionsMaybe = meta.getAttributeValue("checkAssertions");
    if (checkAssertionsMaybe.isPresent()) {
      checkAssertions = checkAssertionsMaybe.get().getAsBoolean();
    } else {
      checkAssertions = true;
    }

    scope = new EntityScope(inner);

    // Initialize array-based caches
    int numAttributes = inner.getAttributeNameToIndex().size();
    resolvedCacheByIndex = new EngineValue[numAttributes];
    resolvingByIndex = new boolean[numAttributes];

    this.commonHandlerCache = inner.getResolvedHandlers();
  }

  /**
   * Create a new ShadowingEntity for a SpatialEntity which is a member of a Patch.
   *
   * @param inner entity to decorate.
   * @param here reference to Path that contains this entity.
   * @param meta reference to simulation or simulation-like entity.
   */
  public ShadowingEntity(EngineValueFactory valueFactory, MutableEntity inner, Entity here,
        Entity meta) {
    this.valueFactory = valueFactory;
    this.inner = inner;
    this.here = here;
    this.meta = meta;

    scope = new EntityScope(inner);

    // Initialize array-based caches
    int numAttributes = inner.getAttributeNameToIndex().size();
    resolvedCacheByIndex = new EngineValue[numAttributes];
    resolvingByIndex = new boolean[numAttributes];

    this.commonHandlerCache = inner.getResolvedHandlers();
  }

  /**
   * Get the value factory to use in building derivative values from this entity.
   *
   * @return EngineValueFactory available for use in building values from this entity.
   */
  public EngineValueFactory getValueFactory() {
    return valueFactory;
  }

  /**
   * Get the names of all attributes with event handlers registered to this entity.
   *
   * @return Iterable over attribute names as Strings.
   */
  public Set<String> getAttributeNames() {
    return scope.getAttributes();
  }

  /**
   * Get all known event handlers for the given attribute on the current substep.
   *
   * @param attribute name of the attribute for which event handlers are requested.
   * @throws IllegalStateException if not currently in a substep.
   */
  public Iterable<EventHandlerGroup> getHandlersForAttribute(String attribute) {
    Optional<String> substep = getSubstep();
    if (substep.isEmpty()) {
      String message = String.format(
          "Cannot get handler for %s while not within a substep.",
          attribute
      );
      throw new IllegalStateException(message);
    }

    String state;
    if ("state".equals(attribute)) {
      // For "state" attribute, try to use already-resolved state value from current substep.
      // This handles the case where state was set in "init" substep and we're now in
      // "step" substep. If state hasn't been resolved yet in current substep, fall back
      // to prior state. This avoids circular dependency while ensuring we have the
      // correct state for cache lookup.
      Optional<Integer> stateIndexMaybe = inner.getAttributeIndex("state");
      if (stateIndexMaybe.isPresent()) {
        int stateIndex = stateIndexMaybe.get();
        boolean indexInBounds = stateIndex >= 0 && stateIndex < resolvedCacheByIndex.length;
        if (indexInBounds && resolvedCacheByIndex[stateIndex] != null) {
          // State was already resolved in current substep - use it for cache lookup
          state = resolvedCacheByIndex[stateIndex].getAsString();
        } else {
          // State not yet resolved - use prior state (avoids circular dependency)
          state = getPriorState();
        }
      } else {
        // No state attribute exists
        state = "";
      }
    } else {
      // For non-state attributes, use current state (may trigger resolution)
      state = getState();
    }

    return getHandlersForAttribute(attribute, substep.get(), state);
  }

  private Iterable<EventHandlerGroup> getHandlersForAttribute(String attribute, String substep,
      String state) {
    // Build cache key string
    // Note: State values in the handler cache are stored with quotes (e.g., "seedling")
    // but the state value we get from the entity doesn't have quotes, so we need to add them
    String cacheKey;
    if (state.isEmpty()) {
      cacheKey = attribute + ":" + substep;
    } else {
      cacheKey = attribute + ":" + substep + ":\"" + state + "\"";
    }

    // Look up in shared cache, return empty list if not found
    List<EventHandlerGroup> handlers = commonHandlerCache.get(cacheKey);


    if (handlers != null) {
      return handlers;
    }

    return Collections.emptyList();
  }

  /**
   * Get the current value of an attribute if it has been resolved in the current substep.
   *
   * @param name unique identifier of the attribute to retrieve.
   * @return Optional containing the current value if resolved, empty Optional otherwise.
   * @throws IllegalArgumentException if the attribute is not known for this entity.
   * @throws IllegalStateException if the attribute exists but has not been initialized.
   */
  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    Optional<Integer> indexMaybe = inner.getAttributeIndex(name);

    if (indexMaybe.isPresent()) {
      int index = indexMaybe.get();
      boolean cacheIndexInBounds = index >= 0 && index < resolvedCacheByIndex.length;
      if (cacheIndexInBounds) {
        EngineValue cached = resolvedCacheByIndex[index];
        if (cached != null) {
          return Optional.of(cached);
        }
      }
    }

    if (hasAttribute(name)) {
      resolveAttribute(name);
      if (indexMaybe.isPresent()) {
        int index = indexMaybe.get();
        boolean postResolveIndexInBounds = index >= 0 && index < resolvedCacheByIndex.length;
        if (postResolveIndexInBounds) {
          EngineValue cached = resolvedCacheByIndex[index];
          if (cached != null) {
            return Optional.of(cached);
          }
        }
      }
    }

    return inner.getAttributeValue(name);
  }

  @Override
  public Optional<EngineValue> getAttributeValue(int index) {
    // Integer-based access with resolution support

    // Bounds check - if index is negative, return empty
    if (index < 0) {
      return Optional.empty();
    }

    // Check array-based cache first
    // Note: If index >= array length (can happen in tests with mocks), we skip cache and continue
    EngineValue cached = null;
    if (index < resolvedCacheByIndex.length) {
      cached = resolvedCacheByIndex[index];
      if (cached != null) {
        return Optional.of(cached);
      }
    }

    String[] indexArray = inner.getIndexToAttributeName();
    String attributeName = null;

    boolean indexInRange = index >= 0 && indexArray != null && index < indexArray.length;
    if (indexInRange) {
      attributeName = indexArray[index];
    }

    if (attributeName == null) {
      return Optional.empty();
    }

    // Trigger resolution using integer-based path for efficiency
    // IMPORTANT: Must trigger resolution, not bypass it, to ensure handlers execute
    if (hasAttribute(attributeName)) {
      resolveAttributeByIndex(index, attributeName);
      // Check array cache again after resolution (only if index is in bounds)
      if (index < resolvedCacheByIndex.length) {
        cached = resolvedCacheByIndex[index];
        if (cached != null) {
          return Optional.of(cached);
        }
      }
    }

    // Fallback: retrieve from inner entity using integer access
    return inner.getAttributeValue(index);
  }

  /**
   * Set the current value of an attribute in the current substep.
   *
   * @param name unique identifier of the attribute to set.
   * @param value new value to assign to the attribute.
   * @throws IllegalArgumentException if the attribute is not known for this entity.
   */
  @Override
  public void setAttributeValue(String name, EngineValue value) {
    assertAttributePresent(name);

    Optional<Integer> indexMaybe = inner.getAttributeIndex(name);
    if (indexMaybe.isPresent()) {
      int index = indexMaybe.get();
      boolean cacheIndexInBounds = index >= 0 && index < resolvedCacheByIndex.length;
      if (cacheIndexInBounds) {
        resolvedCacheByIndex[index] = value;
      }
    }

    inner.setAttributeValue(name, value);
  }

  @Override
  public void setAttributeValue(int index, EngineValue value) {
    String[] indexArray = inner.getIndexToAttributeName();
    String attributeName = null;

    boolean indexInRange = index >= 0 && indexArray != null && index < indexArray.length;
    if (indexInRange) {
      attributeName = indexArray[index];
    }

    if (attributeName == null) {
      String message = String.format(
          "Attribute index %d not found for entity %s",
          index, inner.getName());
      throw new IndexOutOfBoundsException(message);
    }

    boolean cacheIndexInBounds = index >= 0 && index < resolvedCacheByIndex.length;
    if (cacheIndexInBounds) {
      resolvedCacheByIndex[index] = value;
    }

    setAttributeValue(attributeName, value);
  }

  /**
   * Get the value of an attribute from the previous substep or, if first substep, prior timestep.
   *
   * @param name unique identifier of the attribute to retrieve.
   * @return the value of the attribute from the previous step.
   * @throws IllegalStateException if the attribute exists but has not been initalized.
   * @throws IllegalArgumentException if the attribute does not exist on this entity.
   */
  public Optional<EngineValue> getPriorAttribute(String name) {
    Optional<EngineValue> valueMaybe = inner.getAttributeValue(name);
    if (valueMaybe.isEmpty()) {
      assertAttributePresent(name);
    }

    return valueMaybe;
  }

  /**
   * Get the value of an attribute from the previous substep by index.
   *
   * @param index the attribute index
   * @return the value of the attribute from the previous step
   * @throws IllegalStateException if the attribute exists but has not been initialized
   * @throws IndexOutOfBoundsException if index is invalid
   */
  public Optional<EngineValue> getPriorAttribute(int index) {
    return inner.getAttributeValue(index);
  }

  /**
   * Determine if this entity has an attribute.
   *
   * @param name unique identifier of the attribute.
   * @return true if found on this entity within any event handler registration or false otherwise.
   */
  public boolean hasAttribute(String name) {
    return scope.has(name);
  }

  @Override
  public Optional<Integer> getAttributeIndex(String name) {
    return inner.getAttributeIndex(name);
  }

  @Override
  public Map<String, Integer> getAttributeNameToIndex() {
    return inner.getAttributeNameToIndex();
  }

  @Override
  public String[] getIndexToAttributeName() {
    return inner.getIndexToAttributeName();
  }

  @Override
  public Map<String, List<EventHandlerGroup>> getResolvedHandlers() {
    return commonHandlerCache;
  }

  /**
   * Get the patch that contains this entity.
   *
   * @return the ShadowingEntity representing the containing patch.
   */
  public Entity getHere() {
    return here;
  }

  /**
   * Get the simulation context for this entity.
   *
   * @return the Simulation object that provides context for this entity.
   */
  public Entity getMeta() {
    return meta;
  }

  public Entity getPrior() {
    return new PriorShadowingEntityDecorator(this);
  }

  /**
   * Get the underlying entity.
   *
   * <p>Package-private to allow SimulationStepper to access prior state without
   * triggering attribute resolution.</p>
   *
   * @return entity that is decorated by this object.
   */
  MutableEntity getInner() {
    return inner;
  }

  /**
   * Verify that an attribute exists on this entity if enabled.
   *
   * @param name unique identifier of the attribute to verify.
   * @throws IllegalArgumentException if the attribute is not found.
   */
  private void assertAttributePresent(String name) {
    if (ASSERT_VALUE_PRESENT_DEBUG || hasAttribute(name)) {
      return;
    }

    String message = String.format("%s is not a known attribute of %s", name, inner.getName());
    throw new IllegalArgumentException(message);
  }

  /**
   * Get the current state of this entity.
   *
   * @return State of this entity after current resolution.
   */
  private String getState() {
    // Check if "state" attribute exists and is cached in array
    Optional<Integer> stateIndexMaybe = inner.getAttributeIndex("state");
    if (stateIndexMaybe.isEmpty()) {
      return DEFAULT_STATE_STR;
    }

    int stateIndex = stateIndexMaybe.get();
    boolean doesNotUseState = stateIndex < 0
        || stateIndex >= resolvedCacheByIndex.length
        || resolvedCacheByIndex[stateIndex] == null;
    if (doesNotUseState) {
      return DEFAULT_STATE_STR;
    }

    Optional<EngineValue> stateValueMaybe = getAttributeValue("state");
    if (stateValueMaybe.isPresent()) {
      return stateValueMaybe.get().getAsString();
    } else {
      return DEFAULT_STATE_STR;
    }
  }

  /**
   * Get the state of this entity from the prior timestep or substep.
   *
   * <p>This method is used for handler lookup to avoid circular dependencies. When resolving
   * the "state" attribute itself, we cannot call {@link #getState()} because it would trigger
   * attribute resolution while we're already in the middle of resolving "state".</p>
   *
   * <p><b>State Machine Semantics</b>: When an organism is in a particular state, the handlers
   * for that state should execute to determine what the NEXT state will be. Therefore, handler
   * lookup must use the PRIOR state value, not the state being computed.</p>
   *
   * <p>Example: An organism in state "seedling" with age 2 years should have its "seedling"
   * state handlers execute, which may transition it to "juvenile". If we used the NEW state
   * value for handler lookup, we'd look for "juvenile" handlers instead, creating incorrect
   * semantics.</p>
   *
   * <p><b>Implementation</b>: This method directly accesses the prior state from the inner
   * entity without triggering resolution, avoiding circular dependencies.</p>
   *
   * @return The state string from the prior step, or empty string if state attribute doesn't
   *     exist or has no value. Never null.
   */
  private String getPriorState() {
    // Check if "state" attribute exists
    Optional<Integer> stateIndexMaybe = inner.getAttributeIndex("state");
    if (stateIndexMaybe.isEmpty()) {
      debugLog("getPriorState entity=" + inner.getName() + " noStateAttribute");
      return DEFAULT_STATE_STR;
    }

    // Get prior state value directly from inner entity (no resolution)
    Optional<EngineValue> priorStateMaybe = inner.getAttributeValue("state");
    if (priorStateMaybe.isPresent()) {
      String stateValue = priorStateMaybe.get().getAsString();
      debugLog("getPriorState entity=" + inner.getName() + " state=" + stateValue);
      return stateValue;
    } else {
      debugLog("getPriorState entity=" + inner.getName() + " emptyState");
      return DEFAULT_STATE_STR;
    }
  }

  /**
   * Get an immutable copy of the decorated entity for record keeping.
   *
   * @return Entity that is an immutable snapshot such that further edits to this entity are not
   *     reflected on past frozen entities.
   */
  public Entity freeze() {
    return inner.freeze();
  }

  @Override
  public Optional<EngineGeometry> getGeometry() {
    return inner.getGeometry();
  }

  @Override
  public String getName() {
    return inner.getName();
  }

  @Override
  public EntityType getEntityType() {
    return inner.getEntityType();
  }

  @Override
  public Optional<GeoKey> getKey() {
    return inner.getKey();
  }

  @Override
  public long getSequenceId() {
    return inner.getSequenceId();
  }

  /**
   * Attempt to resolve this attribute.
   *
   * <p>Resolve the given attribute to its current value, whether by current substep resolution,
   * previous state, or through handlers. If no conditions for handlers match, previous state is
   * used.</p>
   *
   * @param name unique identifier of the attribute to resolve.
   */
  private void resolveAttribute(String name) {
    Optional<Integer> indexMaybe = inner.getAttributeIndex(name);
    if (indexMaybe.isPresent()) {
      int index = indexMaybe.get();

      boolean indexInBounds = index >= 0 && index < resolvingByIndex.length;
      if (indexInBounds && resolvingByIndex[index]) {
        System.err.println("Encountered a loop when resolving " + name);
        System.err.println("Currently resolving attributes:");
        Map<String, Integer> nameToIndex = inner.getAttributeNameToIndex();
        for (Map.Entry<String, Integer> entry : nameToIndex.entrySet()) {
          int attrIndex = entry.getValue();
          boolean attrIndexInBounds = attrIndex >= 0 && attrIndex < resolvingByIndex.length;
          if (attrIndexInBounds && resolvingByIndex[attrIndex]) {
            System.err.println("\t" + entry.getKey());
          }
        }
        throw new RuntimeException("Encountered a loop when resolving " + name);
      }

      if (indexInBounds) {
        resolvingByIndex[index] = true;
      }

      resolveAttributeUnsafe(name);

      if (indexInBounds) {
        resolvingByIndex[index] = false;
      }
    } else {
      resolveAttributeUnsafe(name);
    }
  }

  /**
   * Attempt to resolve this attribute without checking for circular dependency.
   *
   * @param name unique identifier of the attribute to resolve.
   */
  private void resolveAttributeUnsafe(String name) {
    Optional<String> substep = getSubstep();

    // If outside substep, use prior
    if (substep.isEmpty()) {
      debugLog("resolveAttribute entity=" + inner.getName()
          + " attribute=" + name + " reason=noSubstep");
      resolveAttributeFromPrior(name);
      return;
    }

    boolean hasNoHandlers = inner.hasNoHandlers(name, substep.get());
    debugLog("hasNoHandlers entity=" + inner.getName()
        + " attribute=" + name
        + " substep=" + substep.get()
        + " result=" + hasNoHandlers);

    if (hasNoHandlers) {
      debugLog("fastPath entity=" + inner.getName()
          + " attribute=" + name + " reason=noHandlers");
      resolveAttributeFromPrior(name);
      return;
    }

    // Check for handlers
    Iterator<EventHandlerGroup> handlersMaybe = getHandlersForAttribute(name).iterator();
    if (!handlersMaybe.hasNext()) {
      resolveAttributeFromPrior(name);
      return;
    }

    boolean executed = false;
    boolean isAssertion = false;
    while (handlersMaybe.hasNext()) {
      EventHandlerGroup handlers = handlersMaybe.next();
      boolean localExecuted = executeHandlers(handlers);
      if (localExecuted) {
        executed = true;
        isAssertion = handlers.isAssertion();
        break;
      }
    }

    if (executed) {
      if (checkAssertions && isAssertion) {
        Optional<EngineValue> result = getAttributeValue(name);
        if (result.isPresent()) {
          boolean value = result.get().getAsBoolean();
          if (!value) {
            throw new RuntimeException("Assertion failed for " + name);
          }
        }
      }
    } else {
      resolveAttributeFromPrior(name);
    }
  }

  /**
   * Attempt to resolve an attribute using integer-based access.
   *
   * <p>Integer-based variant of resolveAttribute that uses integer indexing when accessing
   * the inner entity. The attribute name is still required for cache operations and handler
   * execution.</p>
   *
   * @param index the integer index of the attribute
   * @param name the attribute name (needed for cache and handlers)
   */
  private void resolveAttributeByIndex(int index, String name) {
    boolean indexInBounds = index >= 0 && index < resolvingByIndex.length;
    if (indexInBounds && resolvingByIndex[index]) {
      System.err.println("Encountered a loop when resolving " + name);
      System.err.println("Currently resolving attributes:");
      Map<String, Integer> nameToIndex = inner.getAttributeNameToIndex();
      for (Map.Entry<String, Integer> entry : nameToIndex.entrySet()) {
        int attrIndex = entry.getValue();
        boolean attrIndexInBounds = attrIndex >= 0 && attrIndex < resolvingByIndex.length;
        if (attrIndexInBounds && resolvingByIndex[attrIndex]) {
          System.err.println("\t" + entry.getKey());
        }
      }
      throw new RuntimeException("Encountered a loop when resolving " + name);
    }

    if (indexInBounds) {
      resolvingByIndex[index] = true;
    }

    resolveAttributeUnsafeByIndex(index, name);

    if (indexInBounds) {
      resolvingByIndex[index] = false;
    }
  }

  /**
   * Attempt to resolve an attribute by index without checking for circular dependency.
   *
   * <p>Uses integer-based access to inner entity. Critical for hot paths like
   * EntityFastForwarder that iterate over all attributes.</p>
   *
   * @param index the integer index of the attribute
   * @param name the attribute name (needed for cache and handlers)
   */
  private void resolveAttributeUnsafeByIndex(int index, String name) {
    Optional<String> substep = getSubstep();

    // If outside substep, use prior with integer access
    if (substep.isEmpty()) {
      debugLog("resolveAttributeByIndex entity=" + inner.getName()
          + " index=" + index + " attribute=" + name + " reason=noSubstep");
      resolveAttributeFromPriorByIndex(index);
      return;
    }

    boolean hasNoHandlers = inner.hasNoHandlers(name, substep.get());
    debugLog("hasNoHandlers entity=" + inner.getName()
        + " index=" + index
        + " attribute=" + name
        + " substep=" + substep.get()
        + " result=" + hasNoHandlers);

    if (hasNoHandlers) {
      debugLog("fastPath entity=" + inner.getName()
          + " index=" + index
          + " attribute=" + name + " reason=noHandlers");
      resolveAttributeFromPriorByIndex(index);
      return;
    }

    // Check for handlers
    Iterator<EventHandlerGroup> handlersMaybe = getHandlersForAttribute(name).iterator();
    if (!handlersMaybe.hasNext()) {
      resolveAttributeFromPriorByIndex(index);
      return;
    }

    boolean executed = false;
    boolean isAssertion = false;
    while (handlersMaybe.hasNext()) {
      EventHandlerGroup handlers = handlersMaybe.next();
      boolean localExecuted = executeHandlers(handlers);
      if (localExecuted) {
        executed = true;
        isAssertion = handlers.isAssertion();
        break;
      }
    }

    if (executed) {
      if (checkAssertions && isAssertion) {
        Optional<EngineValue> result = getAttributeValue(name);
        if (result.isPresent()) {
          boolean value = result.get().getAsBoolean();
          if (!value) {
            throw new RuntimeException("Assertion failed for " + name);
          }
        }
      }
    } else {
      resolveAttributeFromPriorByIndex(index);
    }
  }


  /**
   * Execute an event handler group.
   *
   * @param handlers The set of handlers to attempt to execute, trying each conditional in order and
   *     stopping execution upon encountering one that matches.
   * @return True if a handler was found to match and was executed. False if no handlers executed.
   */
  private boolean executeHandlers(EventHandlerGroup handlers) {
    Scope decoratedScope = new SyntheticScope(this);
    String entityName = inner.getName();

    for (EventHandler handler : handlers.getEventHandlers()) {
      String attrName = handler.getAttributeName();
      Optional<CompiledSelector> conditionalMaybe = handler.getConditional();

      boolean hasCondition = conditionalMaybe.isPresent();
      debugLog("checkHandler entity=" + entityName
          + " attribute=" + attrName
          + " hasCondition=" + hasCondition);

      boolean matches;
      if (conditionalMaybe.isPresent()) {
        CompiledSelector conditional = conditionalMaybe.get();
        matches = conditional.evaluate(decoratedScope);
        debugLog("evalCondition entity=" + entityName
            + " attribute=" + attrName
            + " result=" + matches);
      } else {
        matches = true;
        debugLog("unconditional entity=" + entityName
            + " attribute=" + attrName);
      }

      if (matches) {
        debugLog("executeHandler entity=" + entityName
            + " attribute=" + attrName);
        EngineValue value = handler.getCallable().evaluate(decoratedScope);

        // Log value type and size if it's a collection
        String valueInfo = value.getLanguageType().toString();
        Optional<Integer> sizeMaybe = value.getSize();
        if (sizeMaybe.isPresent()) {
          valueInfo += " size=" + sizeMaybe.get();
        }

        debugLog("handlerResult entity=" + entityName
            + " attribute=" + attrName
            + " value=" + valueInfo);

        setAttributeValue(handler.getAttributeName(), value);
        return true;
      } else {
        debugLog("handlerSkipped entity=" + entityName
            + " attribute=" + attrName
            + " reason=conditionFalse");
      }
    }

    debugLog("noMatchingHandler entity=" + entityName);
    return false;
  }

  /**
   * Set the current attribute to the value from the previous substep.
   *
   * @param name the unique identifier of the attribute to resolve from prior.
   */
  private void resolveAttributeFromPrior(String name) {
    debugLog("resolveFromPrior entity=" + inner.getName()
        + " attribute=" + name);
    Optional<EngineValue> prior = getPriorAttribute(name);
    if (prior.isPresent()) {
      setAttributeValue(name, prior.get());
    } else {
      debugLog("noPriorValue entity=" + inner.getName()
          + " attribute=" + name);
    }
  }

  /**
   * Set the current attribute to the value from the previous substep using integer index.
   *
   * @param index the attribute index
   */
  private void resolveAttributeFromPriorByIndex(int index) {
    String[] indexArray = inner.getIndexToAttributeName();
    String name = "unknown";
    if (index >= 0 && indexArray != null && index < indexArray.length) {
      name = indexArray[index];
    }

    debugLog("resolveFromPriorByIndex entity=" + inner.getName()
        + " index=" + index + " attribute=" + name);

    Optional<EngineValue> prior = getPriorAttribute(index);

    if (prior.isPresent()) {
      inner.setAttributeValue(index, prior.get());

      boolean cacheIndexInBounds = index >= 0 && index < resolvedCacheByIndex.length;
      if (cacheIndexInBounds) {
        resolvedCacheByIndex[index] = prior.get();
      }
    } else {
      debugLog("noPriorValue entity=" + inner.getName()
          + " index=" + index + " attribute=" + name);
    }
  }

  @Override
  public void lock() {
    inner.lock();
  }

  @Override
  public void unlock() {
    inner.unlock();
  }

  @Override
  public Iterable<EventHandlerGroup> getEventHandlers() {
    return inner.getEventHandlers();
  }

  @Override
  public Optional<EventHandlerGroup> getEventHandlers(EventKey eventKey) {
    return inner.getEventHandlers(eventKey);
  }

  /**
   * Starts a new substep for attribute resolution.
   *
   * <p><b>ARCHITECTURAL CHANGE</b>: This method NO LONGER discovers or manages organism lifecycle.
   * Organisms are now discovered and initialized by {@code SimulationStepper} AFTER all patch
   * attributes have been resolved. This prevents cache coherency violations where organisms
   * discovered from cached values differ from those in final resolved values.</p>
   *
   * <p><b>Why this change?</b> Previously, organism discovery happened here using values from
   * {@code resolvedCacheByIndex[]} before the cache was cleared. This meant organisms were
   * discovered based on stale cached values from the previous phase. Then, during attribute
   * resolution, handlers could create NEW organism instances (e.g., via union operations like
   * {@code Trees.end = prior.Trees | Trees}). These new instances would differ from the ones
   * that had {@code startSubstep()} called, leading to lock/unlock mismatches and organisms
   * only executing once.</p>
   *
   * <p><b>The Solution</b>: By moving organism discovery to {@code SimulationStepper} AFTER
   * attribute resolution completes, we ensure that organisms are discovered from final,
   * fully-resolved attribute values. This guarantees consistent organism instance identity
   * throughout the lifecycle (startSubstep → updateEntity → endSubstep).</p>
   *
   * <p><b>Separation of Concerns</b>:</p>
   * <ul>
   *   <li>{@code ShadowingEntity}: Manages attribute resolution and caching</li>
   *   <li>{@code SimulationStepper}: Manages organism lifecycle</li>
   *   <li>Clear ownership boundaries prevent cache coherency issues</li>
   * </ul>
   *
   * <p>See {@code ORGANISM_LIFECYCLE_ARCHITECTURE_FIX.md} for complete design rationale
   * and architecture diagrams.</p>
   *
   * @param name the name of the substep to start
   */
  @Override
  public void startSubstep(String name) {
    // ARCHITECTURAL CHANGE: Organisms are now discovered and initialized by SimulationStepper
    // after all patch attributes are resolved. This prevents cache coherency violations
    // where organisms discovered from cached values differ from those in final resolved values.
    // See ORGANISM_LIFECYCLE_ARCHITECTURE_FIX.md for full design rationale.
    inner.startSubstep(name);

    // Clear cache immediately - no organisms discovered yet
    Arrays.fill(resolvedCacheByIndex, null);
  }

  /**
   * Ends the current substep after attribute resolution completes.
   *
   * <p><b>ARCHITECTURAL CHANGE</b>: This method NO LONGER discovers or manages organism lifecycle.
   * Organisms are now managed by {@code SimulationStepper}, which handles their complete
   * lifecycle (startSubstep → updateEntity → endSubstep) AFTER patch attribute resolution
   * completes. This ensures consistent organism instance identity throughout the lifecycle.</p>
   *
   * <p><b>Why this change?</b> Previously, this method would discover organisms again and call
   * {@code endSubstep()} on them. However, these organisms might be different instances from
   * those that had {@code startSubstep()} called (due to handlers creating new instances during
   * resolution). This caused {@code IllegalMonitorStateException} errors because we were trying
   * to unlock organisms that were never locked.</p>
   *
   * <p><b>The Solution</b>: {@code SimulationStepper} now manages the complete organism lifecycle
   * using organisms discovered from final attribute values. It maintains references to the same
   * organism instances throughout startSubstep/update/endSubstep, preventing identity
   * mismatches.</p>
   *
   * <p><b>What this method does now</b>:</p>
   * <ul>
   *   <li>Resets circular dependency tracking ({@code resolvingByIndex})</li>
   *   <li>Ends the substep on the inner entity</li>
   *   <li>NO organism discovery or lifecycle management</li>
   * </ul>
   *
   * <p>See {@code ORGANISM_LIFECYCLE_ARCHITECTURE_FIX.md} for complete design rationale
   * and architecture diagrams.</p>
   */
  @Override
  public void endSubstep() {
    // ARCHITECTURAL CHANGE: Organisms are now managed by SimulationStepper, which handles
    // their complete lifecycle (start/update/end) after patch attribute resolution completes.
    // This ensures consistent organism instance identity throughout the lifecycle.

    // Clear circular dependency tracking
    Arrays.fill(resolvingByIndex, false);

    inner.endSubstep();
  }

  @Override
  public Optional<String> getSubstep() {
    return inner.getSubstep();
  }

  @Override
  public boolean hasNoHandlers(String attributeName, String substep) {
    return inner.hasNoHandlers(attributeName, substep);
  }

}
