/**
 * Simple stack-based implementation of EventHandlerMachine.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import java.util.Optional;
import java.util.Stack;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.func.SingleValueScope;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.interpret.ValueResolver;
import org.joshsim.lang.interpret.action.EventHandlerAction;


/**
 * Push-down automaton which uses stack operations to implement EventHandlerMachine.
 *
 * <p>Push-down automaton which uses stack operations to implement EventHandlerMachine under
 * assumption that it is not shared across threads.</p>
 */
public class SingleThreadEventHandlerMachine implements EventHandlerMachine {

  // TODO

  private final Stack<EngineValue> memory;
  private final Scope scope;

  private boolean inConversionGroup;
  private Optional<Units> conversionTarget;

  /**
   * Create a new pushdown automaton which operates on the given scope.
   *
   * @param scope The scope in which to have this automaton perform its operations.
   */
  public SingleThreadEventHandlerMachine(Scope scope) {
    this.scope = scope;

    memory = new Stack<>();
    inConversionGroup = false;
    conversionTarget = Optional.empty();
  }

  @Override
  public EventHandlerMachine push(ValueResolver valueResolver) {
    Optional<EngineValue> value = valueResolver.get(scope);
    memory.push(value.orElseThrow());
    return this;
  }

  @Override
  public EventHandlerMachine push(EngineValue value) {
    memory.push(value);
    return this;
  }

  @Override
  public EventHandlerMachine applyMap(String strategy) {
    if (!"linear".equals(strategy)) {
      throw new IllegalArgumentException("Unsupported map strategy: " + strategy);
    }

    startConversionGroup();
    EngineValue toHigh = pop();
    EngineValue toLow = pop();
    EngineValue fromHigh = pop();
    EngineValue fromLow = pop();
    EngineValue operand = pop();
    endConversionGroup();

    EngineValue fromSpan = fromHigh.subtract(fromLow);
    EngineValue toSpan = toHigh.subtract(toLow);
    EngineValue operandDiff = operand.subtract(fromLow);
    EngineValue percent = operandDiff.divide(fromSpan);
    EngineValue result = toSpan.multiply(percent).add(toLow);

    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine add() {

    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    memory.push(left.add(right));

    return this;
  }

  @Override
  public EventHandlerMachine subtract() {

    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    memory.push(left.subtract(right));
    return this;
  }

  @Override
  public EventHandlerMachine multiply() {

    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    memory.push(left.multiply(right));

    return this;
  }

  @Override
  public EventHandlerMachine divide() {

    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    memory.push(left.divide(right));

    return this;
  }

  @Override
  public EventHandlerMachine pow() {
    EngineValue exponent = pop();
    EngineValue base = pop();
    memory.push(base.raiseToPower(exponent));
    return this;
  }

  @Override
  public EventHandlerMachine and() {
    return null;
  }

  @Override
  public EventHandlerMachine or() {
    return null;
  }

  @Override
  public EventHandlerMachine xor() {
    return null;
  }

  @Override
  public EventHandlerMachine neq() {
    return null;
  }

  @Override
  public EventHandlerMachine gt() {
    return null;
  }

  @Override
  public EventHandlerMachine lt() {
    return null;
  }

  @Override
  public EventHandlerMachine eq() {
    return null;
  }

  @Override
  public EventHandlerMachine lteq() {
    return null;
  }

  @Override
  public EventHandlerMachine gteq() {
    return null;
  }

  @Override
  public EventHandlerMachine slice() {
    return null;
  }

  @Override
  public EventHandlerMachine condition(EventHandlerAction positive) {
    return null;
  }

  @Override
  public EventHandlerMachine branch(EventHandlerAction posAction, EventHandlerAction negAction) {
    return null;
  }

  @Override
  public EventHandlerMachine sample(boolean withReplacement) {
    return null;
  }

  @Override
  public EventHandlerMachine cast(Units newUnits, boolean force) {
    return null;
  }

  @Override
  public EventHandlerMachine bound(boolean hasLower, boolean hasUpper) {
    return null;
  }

  @Override
  public EventHandlerMachine createEntity(String entityType) {
    return null;
  }

  @Override
  public EventHandlerMachine executeSpatialQuery() {
    return null;
  }

  @Override
  public EventHandlerMachine pushAttribute(String attrName) {
    return null;
  }

  @Override
  public EventHandlerMachine randUniform() {
    return null;
  }

  @Override
  public EventHandlerMachine randNorm() {
    return null;
  }

  @Override
  public EventHandlerMachine abs() {
    return null;
  }

  @Override
  public EventHandlerMachine ceil() {
    return null;
  }

  @Override
  public EventHandlerMachine count() {
    return null;
  }

  @Override
  public EventHandlerMachine floor() {
    return null;
  }

  @Override
  public EventHandlerMachine log10() {
    return null;
  }

  @Override
  public EventHandlerMachine ln() {
    return null;
  }

  @Override
  public EventHandlerMachine max() {
    return null;
  }

  @Override
  public EventHandlerMachine mean() {
    return null;
  }

  @Override
  public EventHandlerMachine min() {
    return null;
  }

  @Override
  public EventHandlerMachine round() {
    return null;
  }

  @Override
  public EventHandlerMachine std() {
    return null;
  }

  @Override
  public EventHandlerMachine sum() {
    return null;
  }

  @Override
  public EventHandlerMachine saveLocalVariable(String identifierName) {
    return null;
  }

  @Override
  public EventHandlerMachine end() {
    return null;
  }

  @Override
  public boolean isEnded() {
    return false;
  }

  @Override
  public EngineValue getResult() {
    return null;
  }

  /**
   * Get a value from the top of the stack, converting it if in a conversion group.
   *
   * <p>Get a value from the top of the memory stack and, if in a coversion group, either use this
   * value as the target units if target units have not yet been found or convert to the active
   * target units if required.</p>
   *
   * @return EngineValue after checking for and applying a conversion if required.
   */
  private EngineValue pop() {
    EngineValue valueUncast = memory.pop();

    if (!inConversionGroup) {
      return valueUncast;
    }

    if (conversionTarget.isEmpty()) {
      conversionTarget = Optional.of(valueUncast.getUnits());
      return valueUncast;
    }

    Units startUnits = valueUncast.getUnits();
    Units endUnits = conversionTarget.get();

    if (startUnits.equals(endUnits)) {
      return valueUncast;
    }

    Converter converter = scope.getConverter();
    Conversion conversion = converter.getConversion(startUnits, endUnits);
    CompiledCallable callable = conversion.getConversionCallable();
    Scope innerScope = new SingleValueScope(valueUncast);
    return callable.evaluate(innerScope);
  }

  /**
   * Start a conversion group.
   *
   * <p>Indicate that a conversion group is starting such that all values popped while in the
   * conversion group will be converted to the same type. This target type is determined by the
   * first popped value while in the conversion group.</p>
   */
  private void startConversionGroup() {
    if (inConversionGroup) {
      throw new IllegalStateException("Already in conversion group.");
    }

    inConversionGroup = true;
    conversionTarget = Optional.empty();
  }

  /**
   * Indicate that the conversion group is over.
   *
   * <p>Stop automatically converting all values to the same units and clear the target units for
   * conversion.</p>
   */
  private void endConversionGroup() {
    if (!inConversionGroup) {
      throw new IllegalStateException("Not in conversion group.");
    }

    inConversionGroup = false;
  }
}
