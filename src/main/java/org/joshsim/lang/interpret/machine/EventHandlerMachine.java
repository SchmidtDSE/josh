/**
 * Structures describing a machine for an InterpreterAction.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.interpret.ValueResolver;
import org.joshsim.lang.interpret.action.EventHandlerAction;


/**
 * Structure which describes the virtual machine for event handlers.
 */
public interface EventHandlerMachine {

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

  /**
   * Pop the top two EngineNumbers on the stack and perform a less than or equal test.
   *
   * <p>Pop the top two EngineNumbers on the stack and perform a less than or equal test between
   * them. The first pop will give the right side operand. The second pop will give the left side
   * operand. The result should be compared such that an EngineValue of true will be pushed if left
   * is smaller than right and false otherwise.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine lteq();

  /**
   * Pop the top two EngineNumbers on the stack and perform a greater than or equal test.
   *
   * <p>Pop the top two EngineNumbers on the stack and perform a greater than or equal test between
   * them. The first pop will give the right side operand. The second pop will give the left side
   * operand. The result should be compared such that an EngineValue of true will be pushed if left
   * is smaller than right and false otherwise.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine gteq();

  /**
   * Apply a map command based on the given strategy.
   *
   * <p>Apply a map operation using the given strategy. For example, a linear map will determine
   * what percentage a value is between a "from" low and high. This will then return a value that is
   * the same percentage between a "to" high and low. Expects the following on the top of the stack
   * where each is under the following: operand, from low, from high, to low, to high, and strategy
   * name like linear. After popping these parameters, the result of the map will be pushed to the
   * top of the stack.</p>
   *
   * @param strategy Name of strategy to use in this map operation where, currently, only linear is
   *     valid.
   * @return Reference to this machine for chaining.
   * @throws IllegalArgumentException if unrecognized strategy
   */
  EventHandlerMachine applyMap(String strategy);

  /**
   * Perform a slice operation.
   *
   * <p>Perform a slice operation and put the result on the top of the stack. First pop the
   * selection. Then, pop the subject. Join the elements of the subject pairwise with the elements
   * of the selection. Filter for subject elements that, when paired with the corresponding
   * selection element, is true. Push to the top of the stack the distribution containing only those
   * that make it through the filter.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine slice();

  /**
   * Conditionally execute an action.
   *
   * <p>Pop the top of the stack and execute the given action only if that popped element is true
   * or which is sampled to be true.</p>
   *
   * @param positive The action to execute if the top popped element is true.
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine condition(EventHandlerAction positive);

  /**
   * Conditionally execute one or another action.
   *
   * <p>Pop the top of the stack. Then, execute the positive action only if that popped element is
   * true or which is sampled to be true. If the popped element is false, execute the negative
   * action.</p>
   *
   * @param posAction The action to execute if the top popped element is true.
   * @param negAction The action to execute if the top popped element is false.
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine branch(EventHandlerAction posAction, EventHandlerAction negAction);

  /**
   * Sample values from a distribution or repeatedly sample a single value if not a distribution.
   *
   * <p>Pop the count of values to sample and then pop the target of the sampling. Sample the number
   * of times indicated by the count. Push the result of the sampling as a single EngineValue to
   * the top of the stack. This will be a scalar if the popped count was one or a distribution if
   * the popped count was more than one.</p>
   *
   * @param withReplacement Boolean flag indicating if the sampling should happen with our without
   *     replacement. True if sampling with replacement and false otherwise.
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine sample(boolean withReplacement);

  /**
   * Cast the top of the stack to another value.
   *
   * <p>Pop the value at the top of the stack and convert it to a new value with the given units
   * before pushing the result to the top of the stack.</p>
   *
   * @param newUnits The units to which the top of the stack value should be converted.
   * @param force Boolean flag indicating if the conversion has already happened or not. If false,
   *     the conversion has not yet happened and the machine should use a Converter. If true, the
   *     conversion has already happened and the value of the popped value should be pushed again
   *     numerically unchanged but with the given units.
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine cast(Units newUnits, boolean force);

  /**
   * Ensure that a value is within a given range.
   *
   * <p>Ensure that an operand value is within a certain range before pushing it again to the top
   * of the stack. The original value will be pushed if the operand is in range, the low value if
   * the operand is lower than the range allows, the high value if the operand is higher than the
   * range allows. The following will be popped from the top of the stack before pushing the result
   * where each element is below the following in the stack: operand, lower bound if present, and
   * upper bound if present.</p>
   *
   * @param hasLower Boolean flag indicating if a lower bound is present on the stack. True if on
   *     the stack and a lower bound should be enforced. False if the lower bound is not on the
   *     stack and should not be enforced (effectively the range lower bound is negative infinity).
   * @param hasUpper Boolean flag indicating if an upper bound is present on the stack. True if on
   *     the stack and an upper bound should be enforced. False if the upper bound is not on the
   *     stack and should not be enforced (effectively the range upper bound is positive infinity).
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine bound(boolean hasLower, boolean hasUpper);

  /**
   * Make one or more entities using an EntityPrototype.
   *
   * <p>Make one or more entities from a prototype given a count, pushing the result to the top of
   * the stack where the pushed EngineValue is a Scalar if only one entity was made or a
   * distribution if count was more than one. Count is found by popping the top of the stack before
   * pushing the result.</p>
   *
   * @param entityType The name of the entity type corresponding to the prototype to use.
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine createEntity(String entityType);

  /**
   * Execute a spatial query and push the result to the top of the stack.
   *
   * <p>Execute a spatial query and push the result as an EngineValue to the top of the stack. This
   * query requires a distance which is a distance from center which is at the top of the stack.</p>
   *
   * @param resolver The ValueResolver to use after getting patches at the location.
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine executeSpatialQuery(ValueResolver resolver);

  /**
   * Push an attribute onto the top of the stack.
   *
   * <p>Lookup the value of an attribute on the result of an expression, popping the top of the
   * stack to get the result of the expression before looking up the attribute by the given name
   * and pushing the result to the top of the stack. </p>
   *
   * @param attrName The name of the attribute to be read from the value currently at the top of
   *     the stack.
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine pushAttribute(String attrName);

  /**
   * Draw a single value randomly from a uniform distribution.
   *
   * <p>Draw a single value randomly from a uniform distribution, pushing the value drawn to the top
   * of the stack. Prior to pushing the result, first the high end of the uniform distribution will
   * be popped followed by the low end of the distribution.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine randUniform();

  /**
   * Draw a single value randomly from a normal distribution.
   *
   * <p>Draw a single value randomly from a normal distribution, pushing the value drawn to the top
   * of the stack. Prior to pushing the result, first the standard deviation of the distribution
   * will be popped followed by the mean of the distribution.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine randNorm();

  /**
   * Calculate the absolute value of a number.
   *
   * <p>Pop a value from the top of the stack, calculate its absolute value, and push the result
   * back to the top of the stack. If the popped value is a distribution, it will be sampled once
   * to get a scalar value before the operation.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine abs();

  /**
   * Round a number up to the nearest integer.
   *
   * <p>Pop a value from the top of the stack, round it up to the nearest integer, and push the
   * result back to the top of the stack. If the popped value is a distribution, it will be sampled
   * once to get a scalar value before the operation.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine ceil();

  /**
   * Count the number of values in a distribution.
   *
   * <p>Pop a value from the top of the stack, count its values, and push the result back
   * to the top of the stack. If the popped value is a scalar, it will be treated as a single-value
   * distribution. The pushed result will be a scalar.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine count();

  /**
   * Round a number down to the nearest integer.
   *
   * <p>Pop a value from the top of the stack, round it down to the nearest integer, and push the
   * result back to the top of the stack. If the popped value is a distribution, it will be sampled
   * once to get a scalar value before the operation.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine floor();

  /**
   * Calculate the base-10 logarithm of a number.
   *
   * <p>Pop a value from the top of the stack, calculate its base-10 logarithm, and push the result
   * back to the top of the stack. If the popped value is a distribution, it will be sampled once
   * to get a scalar value before the operation.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine log10();

  /**
   * Calculate the natural logarithm (base e) of a number.
   *
   * <p>Pop a value from the top of the stack, calculate its natural logarithm, and push the result
   * back to the top of the stack. If the popped value is a distribution, it will be sampled once
   * to get a scalar value before the operation.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine ln();

  /**
   * Calculate the maximum value in a distribution.
   *
   * <p>Pop a value from the top of the stack, find its maximum value, and push the result back
   * to the top of the stack. If the popped value is a scalar, it will be treated as a single-value
   * distribution. The pushed result will be a scalar.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine max();

  /**
   * Calculate the mean (average) of a distribution.
   *
   * <p>Pop a value from the top of the stack, calculate its mean, and push the result back
   * to the top of the stack. If the popped value is a scalar, it will be treated as a single-value
   * distribution. The pushed result will be a scalar.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine mean();

  /**
   * Calculate the minimum value in a distribution.
   *
   * <p>Pop a value from the top of the stack, find its minimum value, and push the result back
   * to the top of the stack. If the popped value is a scalar, it will be treated as a single-value
   * distribution. The pushed result will be a scalar.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine min();

  /**
   * Round a number to the nearest integer.
   *
   * <p>Pop a value from the top of the stack, round it to the nearest integer, and push the result
   * back to the top of the stack. If the popped value is a distribution, it will be sampled once
   * to get a scalar value before the operation.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine round();

  /**
   * Calculate the standard deviation of a distribution.
   *
   * <p>Pop a value from the top of the stack, calculate its standard deviation, and push the result
   * back to the top of the stack. If the popped value is a scalar, it will be treated as a single-
   * value distribution. The pushed result will be a scalar.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine std();

  /**
   * Calculate the sum of all values in a distribution.
   *
   * <p>Pop a value from the top of the stack, sum its values, and push the result back
   * to the top of the stack. If the popped value is a scalar, it will be treated as a single-value
   * distribution. The pushed result will be a scalar.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine sum();

  /**
   * Check that a local variable does not yet exist and save it.
   *
   * <p>Pop the top of the stack and save that value as a new local variable in the current scope.
   * This will first check that a local variable of that name is not already present in the scope,
   * throwing an exception if it is already there.</p>
   *
   * @param identifierName The name of the variable to be created if a variable of that name does
   *     not already exist.
   * @return Reference to this machine for chaining.
   * @throws IllegalStateException Thrown if a variable of the given name already exists in the
   *     scope.
   */
  EventHandlerMachine saveLocalVariable(String identifierName);

  /**
   * Indicate that no additional actions should be taken on this machine.
   *
   * <p>End this machine such that any additional actions taken on it will throw an
   * IllegalStateException.</p>
   *
   * @return Reference to this machine for chaining.
   */
  EventHandlerMachine end();

  /**
   * Check if this machine has ended.
   *
   * @return True if this machine has ended such that additional actions taken within this machine
   *     will cause an IllegalStateException or false otherwise.
   */
  boolean isEnded();

  /**
   * Peek at the top of the stack.
   *
   * @return Returns the EngineValue currently at the top of the stack without removing that element
   *     from the top of the stack.
   */
  EngineValue getResult();

}