/**
 * Structures describing an interpreter runtime while be built.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.josh;

import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.FragmentType;


/**
 * Structure which helps build an interpreter runtime.
 */
public abstract class JoshFragment {

  /**
   * Gets the current event handler action from this fragment.
   *
   * @return The current event handler action
   * @throws RuntimeException if this fragment type does not contain an event handler action
   */
  public EventHandlerAction getCurrentAction() {
    throw new RuntimeException("This fragment does not have an event handler action;");
  }

  /**
   * Gets the compiled callable from this fragment.
   *
   * @return The compiled callable
   * @throws RuntimeException if this fragment type does not contain a compiled callable
   */
  public CompiledCallable getCompiledCallable() {
    throw new RuntimeException("This fragment does not have an compiled callable.");
  }

  /**
   * Gets the compiled selector from this fragment.
   *
   * @return The compiled selector, if present
   * @throws RuntimeException if this fragment type does not contain a compiled selector
   */
  public Optional<CompiledSelector> getCompiledSelector() {
    return Optional.empty();
  }

  /**
   * Gets the event handler group builder from this fragment.
   *
   * @return The event handler group builder
   * @throws RuntimeException if this fragment type does not contain an event handler group
   */
  public EventHandlerGroupBuilder getEventHandlerGroup() {
    throw new RuntimeException("This fragment does not have an event handler group.");
  }

  /**
   * Gets all event handler groups from this fragment.
   *
   * @return An iterable of event handler group builders
   */
  public Iterable<EventHandlerGroupBuilder> getEventHandlerGroups() {
    return List.of(getEventHandlerGroup());
  }

  /**
   * Gets the entity prototype from this fragment.
   *
   * @return The entity prototype
   * @throws RuntimeException if this fragment type does not contain an entity
   */
  public EntityPrototype getEntity() {
    throw new RuntimeException("This fragment does not have an entity.");
  }

  /**
   * Gets the conversion from this fragment.
   *
   * @return The conversion
   * @throws RuntimeException if this fragment type does not contain a conversion
   */
  public Conversion getConversion() {
    throw new RuntimeException("This fragment does not have a conversion.");
  }

  /**
   * Gets all conversions from this fragment.
   *
   * @return An iterable of conversions
   */
  public Iterable<Conversion> getConversions() {
    return List.of(getConversion());
  }

  /**
   * Gets the Josh program from this fragment.
   *
   * @return The Josh program
   * @throws RuntimeException if this fragment type does not contain a program
   */
  public JoshProgram getProgram() {
    throw new RuntimeException("This fragment does not have a program.");
  }

  /**
   * Gets the type of this fragment.
   *
   * @return The fragment type
   */
  public abstract FragmentType getFragmentType();

}