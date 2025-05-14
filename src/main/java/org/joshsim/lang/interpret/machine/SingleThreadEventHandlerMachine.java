/**
 * Simple stack-based implementation of EventHandlerMachine.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Stack;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EmbeddedParentEntityPrototype;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.func.EntityScope;
import org.joshsim.engine.func.LocalScope;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.engine.Slicer;
import org.joshsim.engine.value.type.Distribution;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.Scalar;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.bridge.ShadowingEntityPrototype;
import org.joshsim.lang.interpret.ValueResolver;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.mapping.LinearMapStrategy;
import org.joshsim.lang.interpret.mapping.MapBounds;
import org.joshsim.lang.interpret.mapping.MapStrategy;
import org.joshsim.lang.interpret.mapping.QuadraticMapStrategy;
import org.joshsim.lang.interpret.mapping.SigmoidMapStrategy;


/**
 * Push-down automaton which uses stack operations to implement EventHandlerMachine.
 *
 * <p>Push-down automaton which uses stack operations to implement EventHandlerMachine under
 * assumption that it is not shared across threads.</p>
 */
public class SingleThreadEventHandlerMachine implements EventHandlerMachine {

  private static final Units EMPTY_UNITS = Units.of("");
  private static final Units COUNT_UNITS = Units.of("count");
  private static final Units METER_UNITS = Units.of("meters");
  private static final ValueResolver CURRENT_VALUE_RESOLVER = new ValueResolver("current");

  private final EngineBridge bridge;
  private final Stack<EngineValue> memory;
  private final LocalScope scope;
  private final EngineValueFactory valueFactory;
  private final Random random;

  private boolean inConversionGroup;
  private Optional<Units> conversionTarget;
  private boolean isEnded;

  /**
   * Create a new push-down automaton which operates on the given scope.
   *
   * @param bridge The EngineBridge through which to interact with the engine.
   * @param scope The scope in which to have this automaton perform its operations.
   */
  public SingleThreadEventHandlerMachine(EngineBridge bridge, Scope scope) {
    this.bridge = bridge;
    this.scope = new LocalScope(scope);

    memory = new Stack<>();
    inConversionGroup = false;
    conversionTarget = Optional.empty();
    valueFactory = EngineValueFactory.getDefault();
    random = new Random();
    isEnded = false;
  }

  @Override
  public EventHandlerMachine push(ValueResolver valueResolver) {
    Optional<EngineValue> value = valueResolver.get(scope);
    memory.push(value.orElseThrow(
        () -> new IllegalStateException("Unable to get value for " + valueResolver)
    ));
    return this;
  }

  @Override
  public EventHandlerMachine push(EngineValue value) {
    memory.push(value);
    return this;
  }

  @Override
  public EventHandlerMachine applyMap(String strategyName) {
    startConversionGroup();
    EngineValue param = pop();
    EngineValue toHigh = pop();
    EngineValue toLow = pop();
    EngineValue fromHigh = pop();
    EngineValue fromLow = pop();
    EngineValue operand = pop();
    endConversionGroup();

    MapStrategy strategy = switch (strategyName) {
      case "linear" -> new LinearMapStrategy(
          valueFactory,
          new MapBounds(fromLow, fromHigh),
          new MapBounds(toLow, toHigh)
      );
      case "quadratic" -> new QuadraticMapStrategy(
          valueFactory,
          new MapBounds(fromLow, fromHigh),
          new MapBounds(toLow, toHigh),
          param.getAsBoolean()
      );
      case "sigmoid" -> new SigmoidMapStrategy(
          valueFactory,
          new MapBounds(fromLow, fromHigh),
          new MapBounds(toLow, toHigh),
          param.getAsBoolean()
      );
      default -> throw new IllegalArgumentException("Unknown mapping: " + strategyName);
    };

    EngineValue result = strategy.apply(operand);

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
    EngineValue right = pop();
    EngineValue left = pop();

    memory.push(left.multiply(right));

    return this;
  }

  @Override
  public EventHandlerMachine divide() {
    EngineValue right = pop();
    EngineValue left = pop();

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
  public EventHandlerMachine concat() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    Iterable<EngineValue> leftValues = left.getAsDistribution().getContents(
        left.getSize().orElseThrow(),
        false
    );

    Iterable<EngineValue> rightValues = right.getAsDistribution().getContents(
        right.getSize().orElseThrow(),
        false
    );

    List<EngineValue> allValues = new ArrayList<>();
    leftValues.forEach(allValues::add);
    rightValues.forEach(allValues::add);

    memory.push(valueFactory.buildRealizedDistribution(allValues, right.getUnits()));

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

    EngineValue resultDecorated = left.equalTo(right);
    memory.push(resultDecorated);

    return this;
  }

  @Override
  public EventHandlerMachine neq() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    EngineValue resultDecorated = left.notEqualTo(right);
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
    EngineValue selections = pop();
    EngineValue subject = pop();

    Slicer slicer = new Slicer();
    EngineValue result = slicer.slice(subject, selections);

    memory.push(result);

    return this;
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
    MutableEntity parent = scope.get("current").getAsMutableEntity();
    EntityPrototype innerDecorated = new EmbeddedParentEntityPrototype(
        prototype,
        parent
    );
    EntityPrototype decoratedPrototype = new ShadowingEntityPrototype(
        innerDecorated,
        scope
    );

    EngineValue countValue = convert(pop(), COUNT_UNITS);
    long count = countValue.getAsInt();

    String substep = parent.getSubstep().orElseThrow();

    EngineValue result;
    if (count == 1) {
      MutableEntity newEntity = decoratedPrototype.build();
      EntityFastForwarder.fastForward(newEntity, substep);
      result = valueFactory.build(newEntity);
    } else {
      List<EngineValue> values = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        MutableEntity newEntity = decoratedPrototype.build();
        EntityFastForwarder.fastForward(newEntity, substep);
        values.add(valueFactory.build(newEntity));
      }
      result = valueFactory.buildRealizedDistribution(values, Units.of(entityType));
    }

    memory.push(result);
    return this;
  }

  @Override
  public EventHandlerMachine executeSpatialQuery(ValueResolver resolver) {
    EngineValue distance = convert(pop(), METER_UNITS);

    Entity executingEntity = CURRENT_VALUE_RESOLVER.get(scope).orElseThrow().getAsEntity();
    EngineGeometry centerGeometry = executingEntity.getGeometry().orElseThrow();
    EngineGeometry queryGeometry = bridge.getGeometryFactory().createCircle(
        centerGeometry.getCenterX(), centerGeometry.getCenterY(), distance.getAsDecimal()
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

    double doubleResult;
    if (Math.abs(maxDouble - minDouble) < 1e-7) {
      doubleResult = minDouble;
    } else {
      doubleResult = random.nextDouble(minDouble, maxDouble);
    }

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
    EngineValue value = pop();

    if (value.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Cannot apply abs to a distribution.");
    }

    BigDecimal absValue = value.getAsDecimal().abs();
    EngineValue result = valueFactory.build(absValue, value.getUnits());
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine ceil() {
    EngineValue value = pop();

    if (value.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Cannot apply ceil to a distribution.");
    }

    BigDecimal ceilValue = value.getAsDecimal().setScale(0, RoundingMode.CEILING);
    EngineValue result = valueFactory.build(ceilValue, value.getUnits());
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine floor() {
    EngineValue value = pop();

    if (value.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Cannot apply floor to a distribution.");
    }

    BigDecimal floorValue = value.getAsDecimal().setScale(0, RoundingMode.FLOOR);
    EngineValue result = valueFactory.build(floorValue, value.getUnits());
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine round() {
    EngineValue value = pop();

    if (value.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Cannot apply round to a distribution.");
    }

    BigDecimal roundedValue = value.getAsDecimal().setScale(0, RoundingMode.HALF_UP);
    EngineValue result = valueFactory.build(roundedValue, value.getUnits());
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine log10() {
    EngineValue value = pop();

    if (value.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Cannot apply log10 to a distribution.");
    }

    if (value.getAsDecimal().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Logarithm can only be applied to positive numbers.");
    }

    double logValue = Math.log10(value.getAsDecimal().doubleValue());
    BigDecimal resultValue = BigDecimal.valueOf(logValue);
    EngineValue result = valueFactory.build(resultValue, value.getUnits());
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine ln() {
    EngineValue value = pop();

    if (value.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Cannot apply natural log (ln) to a distribution.");
    }

    if (value.getAsDecimal().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(
          "Natural logarithm can only be applied to positive numbers."
      );
    }

    double lnValue = Math.log(value.getAsDecimal().doubleValue());
    BigDecimal resultValue = BigDecimal.valueOf(lnValue);
    EngineValue result = valueFactory.build(resultValue, value.getUnits());
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine count() {
    EngineValue value = pop();
    Distribution distribution = value.getAsDistribution();
    Optional<Integer> size = distribution.getSize();

    if (!size.isPresent()) {
      throw new IllegalArgumentException("Cannot count a virtualized distribution.");
    }

    EngineValue result = valueFactory.build(size.get(), EMPTY_UNITS);
    memory.push(result);
    return this;
  }

  @Override
  public EventHandlerMachine max() {
    EngineValue value = pop();
    Distribution distribution = value.getAsDistribution();
    Optional<Scalar> max = distribution.getMax();

    if (max.isEmpty()) {
      throw new IllegalArgumentException("Cannot find max of a virtualized distribution.");
    }

    memory.push(max.get());
    return this;
  }

  @Override
  public EventHandlerMachine mean() {
    EngineValue value = pop();
    Distribution distribution = value.getAsDistribution();
    Optional<Scalar> mean = distribution.getMean();

    if (mean.isEmpty()) {
      throw new IllegalArgumentException("Cannot calculate mean of a virtualized distribution.");
    }

    memory.push(mean.get());
    return this;
  }

  @Override
  public EventHandlerMachine min() {
    EngineValue value = pop();
    Distribution distribution = value.getAsDistribution();
    Optional<Scalar> min = distribution.getMin();

    if (min.isEmpty()) {
      throw new IllegalArgumentException("Cannot find min of a virtualized distribution.");
    }

    memory.push(min.get());
    return this;
  }

  @Override
  public EventHandlerMachine std() {
    EngineValue value = pop();
    Distribution distribution = value.getAsDistribution();
    Optional<Scalar> std = distribution.getStd();

    if (std.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot calculate standard deviation of a virtualized distribution."
      );
    }

    memory.push(std.get());
    return this;
  }

  @Override
  public EventHandlerMachine sum() {
    EngineValue value = pop();
    Distribution distribution = value.getAsDistribution();
    Optional<Scalar> sum = distribution.getSum();

    if (sum.isEmpty()) {
      throw new IllegalArgumentException("Cannot calculate sum of a virtualized distribution.");
    }

    memory.push(sum.get());
    return this;
  }

  @Override
  public EventHandlerMachine makePosition() {
    EngineValue type2 = pop();
    EngineValue val2 = pop();
    EngineValue type1 = pop();
    EngineValue val1 = pop();

    String firstUnits = val1.getUnits().toString();
    String secondUnits = val2.getUnits().toString();

    boolean firstIsCount = firstUnits.isEmpty();
    boolean secondIsCount = secondUnits.isEmpty();

    String firstUnitsSafe = firstIsCount ? "count" : firstUnits;
    String secondUnitsSafe = secondIsCount ? "count" : secondUnits;

    String complete = String.format(
        "%s %s %s, %s %s %s",
        val1.getAsDecimal(),
        firstUnitsSafe,
        type1.getAsString(),
        val2.getAsDecimal(),
        secondUnitsSafe,
        type2.getAsString()
    );
    EngineValue newValue = valueFactory.build(complete, Units.of("position"));
    push(newValue);

    return this;
  }

  @Override
  public EventHandlerMachine saveLocalVariable(String identifierName) {
    EngineValue value = pop();
    scope.defineConstant(identifierName, value);
    return this;
  }

  @Override
  public EventHandlerMachine end() {
    if (isEnded) {
      throw new IllegalStateException("Machine already ended.");
    }

    this.isEnded = true;
    return this;
  }

  @Override
  public boolean isEnded() {
    return this.isEnded;
  }

  @Override
  public EngineValue getResult() {
    if (memory.isEmpty()) {
      throw new IllegalStateException("No result available or the machine has ended.");
    }

    if (!isEnded) {
      throw new IllegalStateException("Machine has not ended yet.");
    }

    return memory.peek();
  }

  @Override
  public long getStepCount() {
    return bridge.getAbsoluteTimestep();
  }

  @Override
  public void pushExternal(String name, long step) {
    push(bridge.getExternal(scope.get("here").getAsEntity().getKey().orElseThrow(), name, step));
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
