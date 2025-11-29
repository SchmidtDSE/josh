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

    String state = getState();

    return getHandlersForAttribute(attribute, substep.get(), state);
  }

  private Iterable<EventHandlerGroup> getHandlersForAttribute(String attribute, String substep,
      String state) {
    // Build cache key string
    String cacheKey;
    if (state.isEmpty()) {
      cacheKey = attribute + ":" + substep;
    } else {
      cacheKey = attribute + ":" + substep + ":" + state;
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
   * @return entity that is decorated by this object.
   */
  protected MutableEntity getInner() {
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
    boolean stateIndexInvalid = stateIndex < 0 || stateIndex >= resolvedCacheByIndex.length;
    if (stateIndexInvalid) {
      return DEFAULT_STATE_STR;
    }

    // If state is resolved, use it. Otherwise use prior state for handler lookup.
    EngineValue stateValue = resolvedCacheByIndex[stateIndex];
    if (stateValue == null) {
      Optional<EngineValue> priorState = inner.getAttributeValue(stateIndex);
      if (priorState.isPresent()) {
        stateValue = priorState.get();
      }
    }

    if (stateValue != null) {
      return stateValue.getAsString();
    }
    return DEFAULT_STATE_STR;
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
      resolveAttributeFromPrior(name);
      return;
    }

    if (inner.hasNoHandlers(name, substep.get())) {
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
      resolveAttributeFromPriorByIndex(index);
      return;
    }

    if (inner.hasNoHandlers(name, substep.get())) {
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
    for (EventHandler handler : handlers.getEventHandlers()) {
      Optional<CompiledSelector> conditionalMaybe = handler.getConditional();

      boolean matches;
      if (conditionalMaybe.isPresent()) {
        CompiledSelector conditional = conditionalMaybe.get();
        matches = conditional.evaluate(decoratedScope);
      } else {
        matches = true;
      }

      if (matches) {
        EngineValue value = handler.getCallable().evaluate(decoratedScope);
        setAttributeValue(handler.getAttributeName(), value);
        return true;
      }
    }

    return false;
  }

  /**
   * Set the current attribute to the value from the previous substep.
   *
   * @param name the unique identifier of the attribute to resolve from prior.
   */
  private void resolveAttributeFromPrior(String name) {
    Optional<EngineValue> prior = getPriorAttribute(name);
    if (prior.isPresent()) {
      setAttributeValue(name, prior.get());
    }
  }

  /**
   * Set the current attribute to the value from the previous substep using integer index.
   *
   * @param index the attribute index
   */
  private void resolveAttributeFromPriorByIndex(int index) {
    Optional<EngineValue> prior = getPriorAttribute(index);

    if (prior.isPresent()) {
      inner.setAttributeValue(index, prior.get());

      boolean cacheIndexInBounds = index >= 0 && index < resolvedCacheByIndex.length;
      if (cacheIndexInBounds) {
        resolvedCacheByIndex[index] = prior.get();
      }
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

  @Override
  public void startSubstep(String name) {
    InnerEntityGetter.getInnerEntities(this).forEach((x) -> x.startSubstep(name));
    inner.startSubstep(name);

    // Clear array-based cache
    Arrays.fill(resolvedCacheByIndex, null);
  }

  @Override
  public void endSubstep() {
    InnerEntityGetter.getInnerEntities(this).forEach((x) -> x.endSubstep());

    // Clear array-based tracking
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
