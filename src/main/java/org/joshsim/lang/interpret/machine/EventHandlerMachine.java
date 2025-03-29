/**
 * Structures describing a machine for an InterpreterAction.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import org.joshsim.engine.value.EngineValue;
import org.joshsim.lang.interpret.action.EventHandlerAction;


/**
 * Structure which describes the virtual machine for event handlers.
 */
public interface EventHandlerMachine {

  // TODO

  EventHandlerMachine pushIdentifier(String name);

  EventHandlerMachine push(EngineValue value);

  EventHandlerMachine applyMap(String strategy);

  EventHandlerMachine add();

  EventHandlerMachine subtract();

  EventHandlerMachine multiply();

  EventHandlerMachine divide();

  EventHandlerMachine pow();

  EventHandlerMachine and();

  EventHandlerMachine or();

  EventHandlerMachine xor();

  EventHandlerMachine neq();

  EventHandlerMachine gt();

  EventHandlerMachine lt();

  EventHandlerMachine eq();

  EventHandlerMachine lteq();

  EventHandlerMachine gteq();

  EventHandlerMachine slice();

  EventHandlerMachine condition(EventHandlerAction positive);

  EventHandlerMachine branch(EventHandlerAction posAction, EventHandlerAction negAction);

  EventHandlerMachine sample(boolean withReplacement);

  EventHandlerMachine cast(String newUnits, boolean force);

  EventHandlerMachine bound(boolean hasLower, boolean hasUpper);

  EventHandlerMachine makeEntity(String entityType);

  EventHandlerMachine executeSpatialQuery();

  EventHandlerMachine pushAttribute(String attrName);

  EventHandlerMachine randUniform();

  EventHandlerMachine randNorm();

  EventHandlerMachine abs();

  EventHandlerMachine ceil();

  EventHandlerMachine count();

  EventHandlerMachine floor();

  EventHandlerMachine log10();

  EventHandlerMachine ln();

  EventHandlerMachine max();

  EventHandlerMachine mean();

  EventHandlerMachine min();

  EventHandlerMachine round();

  EventHandlerMachine std();

  EventHandlerMachine sum();

  EventHandlerMachine create(String entityName);

  EventHandlerMachine saveLocalVariable(String identifierName);

  EventHandlerMachine end();

  boolean isEnded();

  EngineValue getResult();

}
