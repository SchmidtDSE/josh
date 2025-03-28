/**
 * Structures describing an interpreter runtime while be built.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment;


import org.joshsim.engine.entity.EventHandlerGroupBuilder;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.engine.value.Conversion;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.engine.entity.prototype.EntityPrototype;

import java.util.List;
import java.util.Optional;

/**
 * Structure which helps build an interpreter runtime.
 */
public abstract class Fragment {

  public EventHandlerAction getCurrentAction() {
    throw new RuntimeException("This fragment does not have an event handler action;");
  }

  public CompiledCallable getCompiledCallable() {
    throw new RuntimeException("This fragment does not have an compiled callable.");
  }

  public Optional<CompiledSelector> getCompiledSelector() {
    throw new RuntimeException("This fragment does not have an compiled selector.");
  }

  public EventHandlerGroupBuilder getEventHandlerGroup() {
    throw new RuntimeException("This fragment does not have an event handler group.");
  }

  public Iterable<EventHandlerGroupBuilder> getEventHandlerGroups() {
    return List.of(getEventHandlerGroup());
  }

  public EntityPrototype getEntity() {
    throw new RuntimeException("This fragment does not have an entity.");
  }

  public Conversion getConversion() {
    throw new RuntimeException("This fragment does not have a conversion.");
  }

  public Iterable<Conversion> getConversions() {
    return List.of(getConversion());
  }

  public abstract FragmentType getFragmentType();
  
}
