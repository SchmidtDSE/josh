/**
 * Structures for handling query for present or prior state as it resolves across substeps.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.engine.func.EntityScope;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Structure which allows for querying prior or current state of a SpatialEntity as it resolves.
 *
 * <p>Structure which allows for querying prior or current state of a SpatialEntity and allows for
 * determining if an attribute value has been resolved over time. This manages reference to
 * current, prior, and here. Current can be used for values set in the current timestep and substep
 * or to determine if just in time evaluation is required if it not yet been resolved. Prior can be
 * used to query for previously resolved values. Here can be used to access the Path or patch-like
 * entity which hosues this entity.</p>
 */
public class ShadowingEntity implements Entity {

  private static final String DEFAULT_STATE_STR = "";

  private final MutableEntity inner;
  private final ShadowingEntity here;
  private final Simulation meta;
  private final Set<String> resolvedAttributes;
  private final Set<String> resolvingAttributes;
  private final Scope scope;
  private Optional<String> substep;

  /**
   * Create a new ShadowingEntity for a Patch.
   *
   * @param inner entity to decorate.
   * @param meta reference to simulation or simulation-like entity.
   */
  public ShadowingEntity(Patch inner, Simulation meta) {
    this.inner = inner;
    this.here = this;
    this.meta = meta;

    resolvedAttributes = new HashSet<>();
    resolvingAttributes = new HashSet<>();
    substep = Optional.empty();
    scope = new EntityScope(inner);
  }

  /**
   * Create a new ShadowingEntity for a SpatialEntity which is a member of a Patch.
   *
   * @param inner entity to decorate.
   * @param here reference to Path that contains this entity.
   * @param meta reference to simulation or simulation-like entity.
   */
  public ShadowingEntity(MutableEntity inner, ShadowingEntity here, Simulation meta) {
    this.inner = inner;
    this.here = here;
    this.meta = meta;

    resolvedAttributes = new HashSet<>();
    resolvingAttributes = new HashSet<>();
    substep = Optional.empty();
    scope = new EntityScope(inner);
  }

  /**
   * Indicate that this entity is starting a substep or step phase like step.
   *
   * <p>Indicate that this entity is starting a substep or step phase in which it may be mutated,
   * acquiring a global lock on this entity for thread safety.</p>
   *
   * @param name name of the substep or phase like start which is beginning.
   */
  public void startSubstep(String name) {
    if (substep.isPresent()) {
      String message = String.format(
          "Cannot start %s before %s is completed.",
          substep.get(),
          name
      );
      throw new IllegalStateException(message);
    }

    inner.lock();
    substep = Optional.of(name);
  }

  /**
   * Indicate that this entity is finishing with a substep or step phase like start.
   *
   * <p>Indicate that this entity is ending a substep or step phase in which it may be mutated,
   * releasing a global lock on this entity for thread safety.</p>
   */
  public void endSubstep() {
    resolvedAttributes.clear();
    resolvingAttributes.clear();
    substep = Optional.empty();
    inner.unlock();
  }

  /**
   * Get the names of all attributes with event handlers registered to this entity.
   *
   * @return Iterable over attribute names as Strings.
   */
  public Iterable<String> getAttributeNames() {
    return scope.getAttributes();
  }

  /**
   * Get all known event handlers for the given attribute on the current substep.
   *
   * @param attribute name of the attribute for which event handlers are requested.
   * @throws IllegalStateException if not currently in a substep.
   */
  public Optional<EventHandlerGroup> getHandlers(String attribute) {
    if (substep.isEmpty()) {
      String message = String.format(
          "Cannot get handler for %s while not within a substep.",
          attribute
      );
      throw new IllegalStateException(message);
    }

    String state = getState();
    EventKey eventKey = new EventKey(state, attribute, substep.get());
    return inner.getEventHandlers(eventKey);
  }

  

  /**
   * Resolve all attributes by executing their associated event handlers.
   *
   * <p>This method fetches the attribute names, retrieves the corresponding event handlers,
   * and executes them if present. Execution is done in the context of the current substep, and
   * each handler resolves an attribute to its current value.</p>
   */
  public void resolveAllAttributes() {
    StreamSupport.stream(getAttributeNames().spliterator(), false)
      .map(this::getHandlers)
      .filter((x) -> x.isPresent())
      .map((x) -> x.get())
      .forEach(this::executeHandlers);
  }

  /**
   * Get the current value of an attribute if it has been resolved in the current substep.
   *
   * @param name unique identifier of the attribute to retrieve.
   * @return Optional containing the current value if resolved, empty Optional otherwise.
   * @throws IllegalArgumentException if the attribute is not known for this entity.
   * @throws IllegalStateException if the attribute exists but has not been initialized.
   */
  public Optional<EngineValue> getAttributeValue(String name) {
    if (!resolvedAttributes.contains(name)) {
      resolveAttribute(name);
    }

    return inner.getAttributeValue(name);
  }

  /**
   * Set the current value of an attribute in the current substep.
   *
   * @param name unique identifier of the attribute to set.
   * @param value new value to assign to the attribute.
   * @throws IllegalArgumentException if the attribute is not known for this entity.
   */
  public void setCurrentAttribute(String name, EngineValue value) {
    assertAttributePresent(name);
    resolvedAttributes.add(name);
    inner.setAttributeValue(name, value);
  }

  /**
   * Get the value of an attribute from the previous substep or, if first substep, prior timestep.
   *
   * @param name unique identifier of the attribute to retrieve.
   * @return the value of the attribute from the previous step.
   * @throws IllegalStateException if the attribute exists but has not been initalized.
   * @throws IllegalArgumentException if the attribute does not exist on this entity.
   */
  public EngineValue getPriorAttribute(String name) {
    Optional<EngineValue> valueMaybe = inner.getAttributeValue(name);
    if (valueMaybe.isEmpty()) {
      assertAttributePresent(name);
      String message = String.format("A value for %s is not available.", name);
      throw new IllegalStateException(message);
    }

    return valueMaybe.get();
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

  /**
   * Get the patch that contains this entity.
   *
   * @return the ShadowingEntity representing the containing patch.
   */
  public ShadowingEntity getHere() {
    return here;
  }

  /**
   * Get the key of the patch that contains this entity.
   *
   * @return the PatchKey of the patch that contains this entity.
   */
  public GeoKey getGeoKey() {
    Patch patch = (Patch) getHere().getInner();
    return patch.getKey().orElseThrow();
  }

  /**
   * Get the simulation context for this entity.
   *
   * @return the Simulation object that provides context for this entity.
   */
  public Simulation getMeta() {
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
   * Verify that an attribute exists on this entity.
   *
   * @param name unique identifier of the attribute to verify.
   * @throws IllegalArgumentException if the attribute is not found.
   */
  private void assertAttributePresent(String name) {
    if (hasAttribute(name)) {
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
    boolean doesNotUseState = !resolvedAttributes.contains("state");
    if (doesNotUseState) {
      return DEFAULT_STATE_STR;
    }

    Optional<EngineValue> stateValueMaybe = getAttributeValue("state");
    if (stateValueMaybe.isPresent()) {
      return stateValueMaybe.get().getAsString();
    } else {
      return DEFAULT_STATE_STR;  // TODO: Need to do just in time resolution later merge request.
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
  public Optional<Geometry> getGeometry() {
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
    if (resolvingAttributes.contains(name)) {
      throw new RuntimeException("Encountered a loop when resolving " + name);
    }

    resolvingAttributes.add(name);
    resolveAttributeUnsafe(name);
    resolvingAttributes.remove(name);
  }

  /**
   * Attempt to resolve this attribute without checking for circular dependency.
   *
   * @param name unique identifier of the attribute to resolve.
   */
  private void resolveAttributeUnsafe(String name) {
    // If outside substep, use prior
    if (substep.isEmpty()) {
      resolveAttributeFromPrior(name);
      return;
    }

    // If no handlers, use prior
    Optional<EventHandlerGroup> handlersMaybe = getHandlers(name);
    if (handlersMaybe.isEmpty()) {
      resolveAttributeFromPrior(name);
      return;
    }

    // Attempt to match a handler for updated value
    boolean executed = executeHandlers(handlersMaybe.get());

    // If failed to match, use prior
    if (!executed) {
      resolveAttributeFromPrior(name);
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
        setCurrentAttribute(handler.getAttributeName(), value);
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
    setCurrentAttribute(name, getPriorAttribute(name));
  }
}
