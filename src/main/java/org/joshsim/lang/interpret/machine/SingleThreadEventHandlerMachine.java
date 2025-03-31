/**
 * Simple stack-based implementation of EventHandlerMachine.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.prototype.EmbeddedParentEntityPrototype;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.func.EntityScope;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.geometry.GeometryFactory;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.Distribution;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.bridge.EngineBridge;
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

  private static final Units EMPTY_UNITS = new Units("");
  private static final Units COUNT_UNITS = new Units("count");
  private static final Units METER_UNITS = new Units("meters");
  private static final ValueResolver CURRENT_VALUE_RESOLVER = new ValueResolver("current");

  private final EngineBridge bridge;
  private final Stack<EngineValue> memory;
  private final Scope scope;
  private final EngineValueFactory valueFactory;
  private final Random random;

  private boolean inConversionGroup;
  private Optional<Units> conversionTarget;

  /**
   * Create a new push-down automaton which operates on the given scope.
   *
   * @param bridge The EngineBridge through which to interact with the engine.
   * @param scope The scope in which to have this automaton perform its operations.
   */
  public SingleThreadEventHandlerMachine(EngineBridge bridge, Scope scope) {
    this.bridge = bridge;
    this.scope = scope;

    memory = new Stack<>();
    inConversionGroup = false;
    conversionTarget = Optional.empty();
    valueFactory = new EngineValueFactory();
    random = new Random();
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
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    boolean result = right.getAsBoolean() && left.getAsBoolean();
    EngineValue resultDecorated = valueFactory.build(result, EMPTY_UNITS);
    memory.push(resultDecorated);
    
    return this;
  }

  @Override
  public EventHandlerMachine or() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    boolean result = right.getAsBoolean() || left.getAsBoolean();
    EngineValue resultDecorated = valueFactory.build(result, EMPTY_UNITS);
    memory.push(resultDecorated);

    return this;
  }

  @Override
  public EventHandlerMachine xor() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    boolean result = right.getAsBoolean() ^ left.getAsBoolean();
    EngineValue resultDecorated = valueFactory.build(result, EMPTY_UNITS);
    memory.push(resultDecorated);

    return this;
  }

  @Override
  public EventHandlerMachine eq() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    EngineValue resultDecorated = valueFactory.build(left.equals(right), EMPTY_UNITS);
    memory.push(resultDecorated);

    return this;
  }

  @Override
  public EventHandlerMachine neq() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    EngineValue resultDecorated = valueFactory.build(!left.equals(right), EMPTY_UNITS);
    memory.push(resultDecorated);

    return this;
  }

  @Override
  public EventHandlerMachine gt() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    EngineValue result = left.greaterThan(right);
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine gteq() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    EngineValue result = left.greaterThanOrEqualTo(right);
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine lt() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    EngineValue result = left.lessThan(right);
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine lteq() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    EngineValue result = left.lessThanOrEqualTo(right);
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine slice() {
    // TODO: requires pairwise operations on distribution
    return null;
  }

  @Override
  public EventHandlerMachine condition(EventHandlerAction positive) {
    EngineValue conditionValue = pop();
    boolean conditionResult = conditionValue.getAsBoolean();

    if (conditionResult) {
      positive.apply(this);
    }

    return this;
  }

  @Override
  public EventHandlerMachine branch(EventHandlerAction posAction, EventHandlerAction negAction) {
    EngineValue conditionValue = pop();
    boolean conditionResult = conditionValue.getAsBoolean();

    if (conditionResult) {
      posAction.apply(this);
    } else {
      negAction.apply(this);
    }

    return this;
  }

  @Override
  public EventHandlerMachine sample(boolean withReplacement) {
    EngineValue countValue = convert(pop(), COUNT_UNITS);
    long count = countValue.getAsInt();

    EngineValue subject = pop();
    Distribution subjectDistribution = subject.getAsDistribution();

    EngineValue sampled;
    if (count == 1) {
      sampled = subjectDistribution.sample();
    } else {
      sampled = subjectDistribution.sampleMultiple(count, withReplacement);
    }

    memory.push(sampled);

    return this;
  }

  @Override
  public EventHandlerMachine cast(Units newUnits, boolean force) {
    if (force) {
      forceCast(newUnits);
    } else {
      convertCast(newUnits);
    }

    return this;
  }

  @Override
  public EventHandlerMachine bound(boolean hasLower, boolean hasUpper) {
    startConversionGroup();
    EngineValue upperBound = hasUpper ? pop() : null;
    EngineValue lowerBound = hasLower ? pop() : null;
    EngineValue target = pop();
    endConversionGroup();

    if (hasLower && target.lessThan(lowerBound).getAsBoolean()) {
      memory.push(lowerBound);
    } else if (hasUpper && target.greaterThan(upperBound).getAsBoolean()) {
      memory.push(upperBound);
    } else {
      memory.push(target);
    }

    return this;
  }

  @Override
  public EventHandlerMachine createEntity(String entityType) {
    EntityPrototype prototype = bridge.getPrototype(entityType);
    EmbeddedParentEntityPrototype decoratedPrototype = new EmbeddedParentEntityPrototype(
        prototype,
        scope.get("current").getAsEntity()
    );

    EngineValue countValue = convert(pop(), COUNT_UNITS);
    long count = countValue.getAsInt();

    EngineValue result;
    if (count == 1) {
      result = valueFactory.build(decoratedPrototype.build());
    } else {
      List<EngineValue> values = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        values.add(valueFactory.build(decoratedPrototype.build()));
      }
      result = valueFactory.buildRealizedDistribution(values, new Units(entityType));
    }

    memory.push(result);
    return this;
  }

  @Override
  public EventHandlerMachine executeSpatialQuery(ValueResolver resolver) {
    EngineValue distance = convert(pop(), METER_UNITS);

    Entity executingEntity = CURRENT_VALUE_RESOLVER.get(scope).orElseThrow().getAsEntity();
    Geometry centerGeometry = executingEntity.getGeometry().orElseThrow();
    Geometry queryGeometry = GeometryFactory.createCircle(
        distance.getAsDecimal(),
        centerGeometry.getCenterX(),
        centerGeometry.getCenterY(),
        centerGeometry.getSpatialContext()
    );

    Iterable<Entity> patches = bridge.getPriorPatches(queryGeometry);
    List<EngineValue> resolved = StreamSupport.stream(patches.spliterator(), false)
        .map(EntityScope::new)
        .map(scope -> resolver.get(scope).orElseThrow())
        .toList();

    EngineValue resolvedDistribution = valueFactory.buildRealizedDistribution(
        resolved,
        !resolved.isEmpty() ? EMPTY_UNITS : resolved.get(0).getUnits()
    );
    memory.push(resolvedDistribution);

    return null;
  }

  @Override
  public EventHandlerMachine pushAttribute(ValueResolver resolver) {
    Entity entity = pop().getAsEntity();
    Scope scope = new EntityScope(entity);
    EngineValue attributeValue = resolver.get(scope).orElseThrow();
    memory.push(attributeValue);
    return this;
  }

  @Override
  public EventHandlerMachine randUniform() {
    startConversionGroup();
    EngineValue max = pop();
    EngineValue min = pop();
    endConversionGroup();

    double minDouble = min.getAsDecimal().doubleValue();
    double maxDouble = max.getAsDecimal().doubleValue();
    double doubleResult = random.nextDouble(minDouble, maxDouble);
    BigDecimal result = BigDecimal.valueOf(doubleResult);

    EngineValue decoratedResult = valueFactory.build(result, min.getUnits());
    memory.push(decoratedResult);

    return this;
  }

  @Override
  public EventHandlerMachine randNorm() {
    startConversionGroup();
    EngineValue std = pop();
    EngineValue mean = pop();
    endConversionGroup();

    double meanDouble = mean.getAsDecimal().doubleValue();
    double stdDouble = std.getAsDecimal().doubleValue();
    double randGauss = random.nextGaussian(meanDouble, stdDouble);
    BigDecimal randomValue = BigDecimal.valueOf(randGauss);

    EngineValue result = valueFactory.build(randomValue, mean.getUnits());
    memory.push(result);

    return this;
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
  public EventHandlerMachine count() {
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

    return convert(valueUncast, conversionTarget.get());
  }

  private EngineValue convert(EngineValue valueUncast, Units endUnits) {
    Units startUnits = valueUncast.getUnits();

    if (startUnits.equals(endUnits)) {
      return valueUncast;
    }

    return bridge.convert(valueUncast, endUnits);
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

  /**
   * Forces the unit conversion of the value at the top of the memory stack to the specified units.
   *
   * <p>Retrieve the top value from the memory stack, performs a unit replacement operation based on
   * the provided units, and then pushes the resulting value back onto the memory stack.</p>
   *
   * @param newUnits The new unit specification to which the value should be forcibly cast.
   */
  private void forceCast(Units newUnits) {
    EngineValue subject = pop();
    memory.push(subject.replaceUnits(newUnits));
  }

  /**
   * Convert a value at the top of th ememory stack and push the result.
   *
   * <p>Convert the units of the current subject to the specified new units and  push the result
   * onto the memory stack. If the subject's current units are  the same as the specified new units,
   * the subject is directly pushed onto the memory stack without conversion. Otherwise, the subject
   * is converted to the new units before being pushed.</p>
   *
   * @param newUnits The target units to which the subject should be converted.
   */
  private void convertCast(Units newUnits) {
    EngineValue subject = pop();

    if (subject.getUnits().equals(newUnits)) {
      memory.push(subject);
    } else {
      memory.push(convert(subject, newUnits));
    }
  }
}
