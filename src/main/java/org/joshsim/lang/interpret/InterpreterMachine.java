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

  InterpreterMachine pushNumber(double name);

  InterpreterMachine pushValue(EngineValue value);

}
