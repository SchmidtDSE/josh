/**
 * Structures describing a machine for an InterpreterAction.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import org.joshsim.engine.value.EngineValue;
import org.joshsim.lang.interpret.ValueResolver;
import org.joshsim.lang.interpret.action.EventHandlerAction;


/**
 * Structure which describes the virtual machine for event handlers.
 */
public interface EventHandlerMachine {

  // TODO

  /**
   * Resolve a value through a value resolver and push it at the top of the stack.
   *
   * @param valueResolver The resolver to use in finding the value to push.
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine push(ValueResolver valueResolver);

  /**
   * Push an already resolved value onto the top of the stack for this automaton.
   *
   * @param value The value to push onto the top of the stack.
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine push(EngineValue value);

  /**
   * Pop the top two EngineNumbers on the stack and add them together, pushing the result.
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine add();

  /**
   * Pop the top two EngineNumbers on the stack and subtract, pushing the result.
   *
   * <p>Pop the top two EngineNumbers where the first pop is the right side operand and the second
   * pop is the left hand operand. Then, subtract right from left. Finally, push the result.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine subtract();

  /**
   * Pop the top two EngineNumbers on the stack and mulitiply them together, pushing the result.
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine multiply();

  /**
   * Pop the top two EngineNumbers on the stack and divide, pushing the result.
   *
   * <p>Pop the top two EngineNumbers where the first pop is the right side operand and the second
   * pop is the left hand operand. Then, divide right from left. Finally, push the result.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine divide();

  /**
   * Pop the top two EngineNumbers on the stack and raise to power, pushing the result.
   *
   * <p>Pop the top two EngineNumbers where the first pop is the right side operand and the second
   * pop is the left hand operand. Then, raise left to the power of right. Finally, push the result
   * to the top of the stack.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine pow();

  /**
   * Pop the top two EngineNumbers on the stack and perform a logical and.
   *
   * <p>Pop the top two EngineNumbers on the stack and perform a logical and between them, putting
   * the result on the top of the stack.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine and();

  /**
   * Pop the top two EngineNumbers on the stack and perform a logical or.
   *
   * <p>Pop the top two EngineNumbers on the stack and perform a logical or between them, putting
   * the result on the top of the stack.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine or();

  /**
   * Pop the top two EngineNumbers on the stack and perform a logical xor.
   *
   * <p>Pop the top two EngineNumbers on the stack and perform a logical exclusive or between them,
   * putting the result on the top of the stack.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine xor();

  /**
   * Pop the top two EngineNumbers on the stack and perform a not equals test.
   *
   * <p>Pop the top two EngineNumbers on the stack and perform a not equals test between them,
   * putting the result on the top of the stack.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine neq();

  /**
   * Pop the top two EngineNumbers on the stack and perform a greater than test.
   *
   * <p>Pop the top two EngineNumbers on the stack and perform a greater than test between them. The
   * first pop will give the right side operand. The second pop will give the left side operand. The
   * result should be compared such that an EngineValue of true will be pushed if left is larger
   * than right and false otherwise.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine gt();

  /**
   * Pop the top two EngineNumbers on the stack and perform a less than test.
   *
   * <p>Pop the top two EngineNumbers on the stack and perform a less than test between them. The
   * first pop will give the right side operand. The second pop will give the left side operand. The
   * result should be compared such that an EngineValue of true will be pushed if left is smaller
   * than right and false otherwise.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine lt();

  /**
   * Pop the top two EngineNumbers on the stack and perform an equals test.
   *
   * <p>Pop the top two EngineNumbers on the stack and perform an equals test between them, putting
   * the result on the top of the stack.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine eq();

  EventHandlerMachine lteq();

  EventHandlerMachine gteq();

  /**
   * Apply a map command based on the given strategy.
   *
   * <p>Apply a map operation using the given strategy. For example, a linear map will determine
   * what percentage a value is between a "from" low and high. This will then return a value that is
   * the same percentage between a "to" high and low. Expects the following on the top of the stack
   * where each is under the other: operand, from low, from high, to low, and to high. After popping
   * these parameters, the result of the map will be pushed to the top of the stack.</p>
   *
   * @param strategy Name of strategy to use in this map operation where, currently, only linear is
   *     valid.
   * @throws IllegalArgumentException if unrecognized strategy
   * @return
   */
  EventHandlerMachine applyMap(String strategy);

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
