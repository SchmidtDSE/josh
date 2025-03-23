/**
 * Structures for handling query for present or prior state as it resolves across substeps.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.EventHandlerGroup;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.entity.Simulation;
import org.joshsim.engine.entity.SpatialEntity;
import org.joshsim.engine.func.EntityScope;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.EngineValue;


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
public class ShadowingEntity {

  private final SpatialEntity inner;
  private final ShadowingEntity here;
  private final Simulation meta;
  private final Set<String> resolvedAttributes;
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
  public ShadowingEntity(SpatialEntity inner, ShadowingEntity here, Simulation meta) {
    this.inner = inner;
    this.here = here;
    this.meta = meta;

    resolvedAttributes = new HashSet<>();
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
    substep = Optional.empty();
    inner.unlock();
  }

  /**
   * Get the names of all attributes with event handlers registered to this entity.
   *
   * @return Iterable over attribute names as Strings.
   */
  public Iterable<String> getAttributes() {
    return scope.getAttributes();
  }

  /**
   * Get all known event handlers for the given attribute on the current substep.
   *
   * @param attribute name of the attribute for which event handlers are requested.
   * @throws IllegalStateException if not currently in a substep.
   */
  public Iterable<EventHandlerGroup> getHandlers(String attribute) {
    if (substep.isEmpty()) {
      String message = String.format(
          "Cannot get handler for %s while not within a substep.",
          attribute
      );
      throw new IllegalStateException(message);
    }

    return inner.getEventHandlers(attribute, substep.get());
  }

  /**
   * Get the current value of an attribute if it has been resolved in the current substep.
   *
   * @param name unique identifier of the attribute to retrieve.
   * @return Optional containing the current value if resolved, empty Optional otherwise.
   * @throws IllegalArgumentException if the attribute is not known for this entity.
   * @throws IllegalStateException if the attribute exists but has not been initialized.
   */
  public Optional<EngineValue> getCurrentAttribute(String name) {
    if (!resolvedAttributes.contains(name)) {
      return Optional.empty();
    }

    Optional<EngineValue> valueMaybe = inner.getAttributeValue(name);
    if (valueMaybe.isEmpty()) {
      assertAttributePresent(name);
      String message = String.format("A value for %s has not been initialized.", name);
      throw new IllegalStateException(message);
    }

    return valueMaybe;
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
    return allAttributes.contains(name);
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
   * Get the simulation context for this entity.
   *
   * @return the Simulation object that provides context for this entity.
   */
  public Simulation getMeta() {
    return meta;
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

}
