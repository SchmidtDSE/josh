/**
 * Structures describing a machine for an InterpreterAction.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;
import org.joshsim.engine.value.EngineValue;


/**
 * Structure which describes the virutal machine in which an interpreter action is taken.
 */
public interface InterpreterMachine {

  InterpreterMachine pushIdentifier(String name);

  InterpreterMachine push(EngineValue value);

  InterpreterMachine applyMap(String strategy);

  InterpreterMachine add();

  InterpreterMachine subtract();

  InterpreterMachine multiply();

  InterpreterMachine divide();

  InterpreterAction pow();

  InterpreterMachine and();

  InterpreterMachine or();

  InterpreterMachine xor();

  InterpreterMachine neq();

  InterpreterMachine gt();

  InterpreterMachine lt();

  InterpreterMachine eq();

  InterpreterMachine lteq();

  InterpreterMachine gteq();

  InterpreterMachine slice();

  InterpreterMachine branch(InterpreterAction posAction, InterpreterAction negAction);

  InterpreterMachine sample(boolean withReplacement);

  InterpreterMachine cast(String newUnits, boolean force);

  InterpreterMachine bound(boolean hasLower, boolean hasUpper);

  InterpreterMachine makeEntity(String entityType);

  InterpreterMachine executeSpatialQuery();

  InterpreterMachine pushAttribute(String attrName);

  InterpreterMachine abs();

  InterpreterMachine ceil();

  InterpreterMachine count();

  InterpreterMachine floor();

  InterpreterMachine log10();

  InterpreterMachine ln();

  InterpreterMachine max();

  InterpreterMachine mean();

  InterpreterMachine min();

  InterpreterMachine round();

  InterpreterMachine std();

  InterpreterMachine sum();

  InterpreterMachine create();

  InterpreterMachine saveLocalVariable(String identifierName);

}
