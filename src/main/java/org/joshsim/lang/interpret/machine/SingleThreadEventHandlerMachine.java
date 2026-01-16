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
import java.util.Stack;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EmbeddedParentEntityPrototype;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
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
import org.joshsim.lang.interpret.mapping.MapBounds;
import org.joshsim.lang.interpret.mapping.MapStrategy;
import org.joshsim.lang.interpret.mapping.MappingBuilder;
import org.joshsim.lang.io.CombinedDebugOutputFacade;
import org.joshsim.util.SharedRandom;


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

  private final ValueResolver currentValueResolver;

  private final EngineBridge bridge;
  private final Stack<EngineValue> memory;
  private LocalScope scope;  // Mutable to allow temporary scope swapping in withLocalBinding
  private final EngineValueFactory valueFactory;
  private final boolean favorBigDecimal;
  private final Optional<CombinedDebugOutputFacade> debugOutputFacade;

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
    this(bridge, scope, Optional.empty());
  }

  /**
   * Create a new push-down automaton with debug output support.
   *
   * @param bridge The EngineBridge through which to interact with the engine.
   * @param scope The scope in which to have this automaton perform its operations.
   * @param debugOutputFacade Optional debug output facade for writing debug messages.
   */
  public SingleThreadEventHandlerMachine(EngineBridge bridge, Scope scope,
      Optional<CombinedDebugOutputFacade> debugOutputFacade) {
    this.bridge = bridge;
    this.scope = new LocalScope(scope);
    this.debugOutputFacade = debugOutputFacade;

    memory = new Stack<>();
    inConversionGroup = false;
    conversionTarget = Optional.empty();
    valueFactory = bridge.getEngineValueFactory();
    currentValueResolver = new ValueResolver(valueFactory, "current");
    favorBigDecimal = valueFactory.isFavoringBigDecimal();
    isEnded = false;
  }

  @Override
  public EventHandlerMachine push(ValueResolver valueResolver) {
    String path = valueResolver.getPath();

    // Try normal resolution first
    Optional<EngineValue> value = valueResolver.get(scope);
    if (value.isPresent()) {
      memory.push(value.get());
      return this;
    }

    // Fall back to built-in meta attributes (e.g., meta.year, meta.stepCount)
    if (path.startsWith("meta.")) {
      String attrName = path.substring(5); // Remove "meta." prefix
      Optional<EngineValue> builtin = getBuiltinMetaAttribute(attrName);
      if (builtin.isPresent()) {
        memory.push(builtin.get());
        return this;
      }
    }

    // Fall back to built-in here attributes (e.g., here.x, here.y)
    if (path.startsWith("here.")) {
      String attrName = path.substring(5); // Remove "here." prefix
      Optional<EngineValue> builtin = getBuiltinHereAttribute(attrName);
      if (builtin.isPresent()) {
        memory.push(builtin.get());
        return this;
      }
    }

    throw new IllegalStateException("Unable to get value for " + valueResolver);
  }

  @Override
  public EventHandlerMachine push(EngineValue value) {
    memory.push(value);
    return this;
  }

  @Override
  public EventHandlerMachine applyMap(String strategyName) {
    MappingBuilder mappingBuilder = new MappingBuilder();

    EngineValue param = pop();
    mappingBuilder.setMapBehaviorArgument(param);

    startConversionGroup();
    EngineValue toHigh = pop();
    EngineValue toLow = pop();
    mappingBuilder.setRange(new MapBounds(toLow, toHigh));
    endConversionGroup();

    startConversionGroup();
    EngineValue fromHigh = pop();
    EngineValue fromLow = pop();
    mappingBuilder.setDomain(new MapBounds(fromLow, fromHigh));

    EngineValue operand = pop();
    endConversionGroup();

    mappingBuilder.setValueFactory(valueFactory);
    MapStrategy strategy = mappingBuilder.build(strategyName);

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

    int leftSize = left.getSize().orElseThrow();
    int rightSize = right.getSize().orElseThrow();

    Iterable<EngineValue> leftValues = left.getAsDistribution().getContents(
        leftSize,
        false
    );

    Iterable<EngineValue> rightValues = right.getAsDistribution().getContents(
        rightSize,
        false
    );

    // Pre-size ArrayList to avoid growth overhead
    List<EngineValue> allValues = new ArrayList<>(leftSize + rightSize);
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

    EngineValue result = left.and(right);
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine or() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    EngineValue result = left.or(right);
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine xor() {
    startConversionGroup();
    EngineValue right = pop();
    EngineValue left = pop();
    endConversionGroup();

    EngineValue result = left.xor(right);
    memory.push(result);

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

    // Validate that bounds are not inverted
    if (hasLower && hasUpper && lowerBound.greaterThan(upperBound).getAsBoolean()) {
      throw new IllegalArgumentException(
          "Invalid bounds: lower bound (" + lowerBound.getAsString()
          + ") cannot be greater than upper bound (" + upperBound.getAsString() + ")"
      );
    }

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
        valueFactory,
        innerDecorated,
        scope
    );

    EngineValue countValue = convert(pop(), COUNT_UNITS);
    long count = countValue.getAsInt();

    if (count < 0) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot create entities with negative or undefined count. "
              + "Received count value: %d. The count value must be a concrete positive integer.",
              count
          )
      );
    }

    String substep = parent.getSubstep().orElseThrow();

    EngineValue result;
    if (count == 1) {
      MutableEntity newEntity = decoratedPrototype.build();
      EntityFastForwarder.fastForward(newEntity, substep);
      result = valueFactory.build(newEntity);
    } else {
      // Pre-size ArrayList with known count to avoid growth overhead
      List<EngineValue> values = new ArrayList<>((int) count);
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

    Entity executingEntity = currentValueResolver.get(scope).orElseThrow().getAsEntity();
    EngineGeometry centerGeometry = executingEntity.getGeometry().orElseThrow();
    EngineGeometry queryGeometry = bridge.getGeometryFactory().createCircle(
        centerGeometry.getCenterX(), centerGeometry.getCenterY(), distance.getAsDecimal()
    );

    List<Entity> patches = bridge.getPriorPatches(queryGeometry);

    // Check if querying for patch entities themselves vs attributes on patches
    // This handles cases like "Default within 30 m" where "Default" is the patch type name
    boolean queryingForPatchEntities = !patches.isEmpty()
        && isQueryForPatch(resolver, patches.get(0).getName());

    // Pre-size ArrayList with known patch count to eliminate ArrayList.grow() overhead
    List<EngineValue> resolved = new ArrayList<>(patches.size());

    if (queryingForPatchEntities) {
      // Return the patch entities themselves
      for (Entity patch : patches) {
        resolved.add(valueFactory.build(patch));
      }
    } else {
      // Return attributes from the patches with improved error message
      for (Entity patch : patches) {
        EntityScope patchScope = new EntityScope(patch);
        EngineValue value = resolver.get(patchScope).orElseThrow(
            () -> new IllegalStateException(
                String.format(
                    "Cannot resolve '%s' in spatial query. The attribute may not be available "
                    + "in the 'prior' context at this substep. Check that the attribute is "
                    + "set in a substep that runs before the current one (e.g., 'init', 'start', "
                    + "or 'step' run before 'end').",
                    resolver
                )
            )
        );
        resolved.add(value);
      }
    }

    EngineValue resolvedDistribution = valueFactory.buildRealizedDistribution(
        resolved,
        !resolved.isEmpty() ? resolved.get(0).getUnits() : EMPTY_UNITS
    );
    memory.push(resolvedDistribution);

    return null;
  }

  @Override
  public EventHandlerMachine pushAttribute(ValueResolver resolver) {
    Entity entity = pop().getAsEntity();
    Scope scope = new EntityScope(entity);

    // Try normal resolution first
    Optional<EngineValue> value = resolver.get(scope);
    if (value.isPresent()) {
      memory.push(value.get());
      return this;
    }

    // Fall back to built-in meta attributes on simulation entity
    if (entity.getEntityType() == EntityType.SIMULATION) {
      Optional<EngineValue> builtin = getBuiltinMetaAttribute(resolver.getPath());
      if (builtin.isPresent()) {
        memory.push(builtin.get());
        return this;
      }
    }

    throw new IllegalStateException("Unable to get attribute " + resolver.getPath()
        + " from " + entity.getName());
  }

  /**
   * Get a built-in meta attribute value if the name matches a built-in.
   *
   * <p>Built-in meta attributes are available without user definition per the language spec:
   * meta.stepCount (0-based step counter), meta.year/meta.step (current step value).</p>
   *
   * @param name the attribute name to check.
   * @return Optional containing the value if it's a built-in, empty otherwise.
   */
  private Optional<EngineValue> getBuiltinMetaAttribute(String name) {
    return switch (name) {
      case "stepCount" -> Optional.of(
          valueFactory.build(bridge.getAbsoluteTimestep(), COUNT_UNITS)
      );
      case "year" -> Optional.of(
          valueFactory.build(bridge.getCurrentTimestep(), Units.of("years"))
      );
      default -> Optional.empty();
    };
  }

  /**
   * Get a built-in here attribute value if the name matches a built-in.
   *
   * <p>Built-in here attributes provide spatial information about the current patch:
   * here.x (center X coordinate in meters), here.y (center Y coordinate in meters).</p>
   *
   * @param name the attribute name to check.
   * @return Optional containing the value if it's a built-in, empty otherwise.
   */
  private Optional<EngineValue> getBuiltinHereAttribute(String name) {
    if (!scope.has("here")) {
      return Optional.empty();
    }

    Entity here = scope.get("here").getAsEntity();
    Optional<EngineGeometry> geometry = here.getGeometry();

    if (geometry.isEmpty()) {
      return Optional.empty();
    }

    return switch (name) {
      case "x" -> Optional.of(
          valueFactory.buildForNumber(geometry.get().getCenterX().doubleValue(), Units.of("m"))
      );
      case "y" -> Optional.of(
          valueFactory.buildForNumber(geometry.get().getCenterY().doubleValue(), Units.of("m"))
      );
      default -> Optional.empty();
    };
  }

  @Override
  public EventHandlerMachine randUniform() {
    startConversionGroup();
    EngineValue max = pop();
    EngineValue min = pop();
    endConversionGroup();

    double minDouble = min.getAsDouble();
    double maxDouble = max.getAsDouble();

    double doubleResult;
    if (Math.abs(maxDouble - minDouble) < 1e-7) {
      doubleResult = minDouble;
    } else {
      doubleResult = SharedRandom.nextDouble(minDouble, maxDouble);
    }

    EngineValue decoratedResult = valueFactory.buildForNumber(doubleResult, min.getUnits());
    memory.push(decoratedResult);

    return this;
  }

  @Override
  public EventHandlerMachine randNorm() {
    startConversionGroup();
    EngineValue std = pop();
    EngineValue mean = pop();
    endConversionGroup();

    double meanDouble = mean.getAsDouble();
    double stdDouble = std.getAsDouble();
    double randGauss = SharedRandom.nextGaussian(meanDouble, stdDouble);

    EngineValue result = valueFactory.buildForNumber(randGauss, mean.getUnits());
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine abs() {
    EngineValue value = pop();

    if (value.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Cannot apply abs to a distribution.");
    }

    EngineValue result;
    if (favorBigDecimal) {
      BigDecimal absValue = value.getAsDecimal().abs();
      result = valueFactory.build(absValue, value.getUnits());
    } else {
      double absValue = Math.abs(value.getAsDouble());
      result = valueFactory.build(absValue, value.getUnits());
    }
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine ceil() {
    EngineValue value = pop();

    if (value.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Cannot apply ceil to a distribution.");
    }

    EngineValue result;
    if (favorBigDecimal) {
      BigDecimal ceilValue = value.getAsDecimal().setScale(0, RoundingMode.CEILING);
      result = valueFactory.build(ceilValue, value.getUnits());
    } else {
      double ceilValue = Math.ceil(value.getAsDouble());
      result = valueFactory.build(ceilValue, value.getUnits());
    }
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine floor() {
    EngineValue value = pop();

    if (value.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Cannot apply floor to a distribution.");
    }

    EngineValue result;
    if (favorBigDecimal) {
      BigDecimal floorValue = value.getAsDecimal().setScale(0, RoundingMode.FLOOR);
      result = valueFactory.build(floorValue, value.getUnits());
    } else {
      double floorValue = Math.floor(value.getAsDouble());
      result = valueFactory.build(floorValue, value.getUnits());
    }
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine round() {
    EngineValue value = pop();

    if (value.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Cannot apply round to a distribution.");
    }

    EngineValue result;
    if (favorBigDecimal) {
      BigDecimal roundedValue = value.getAsDecimal().setScale(0, RoundingMode.HALF_UP);
      result = valueFactory.build(roundedValue, value.getUnits());
    } else {
      double roundedValue = Math.round(value.getAsDouble());
      result = valueFactory.build(roundedValue, value.getUnits());
    }
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

    double logValue = Math.log10(value.getAsDouble());
    EngineValue result = valueFactory.buildForNumber(logValue, value.getUnits());
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

    double lnValue = Math.log(value.getAsDouble());
    EngineValue result = valueFactory.buildForNumber(lnValue, value.getUnits());
    memory.push(result);

    return this;
  }

  @Override
  public EventHandlerMachine count() {
    EngineValue value = pop();
    EngineValue result = valueFactory.build(value.getCount(), EMPTY_UNITS);
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
        val1.getAsString(),
        firstUnitsSafe,
        type1.getAsString(),
        val2.getAsString(),
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
      throw new IllegalStateException(
          "Whoops! Something went wrong during simulation processing. This is likely a bug "
          + "in Josh rather than an issue in your own code. Please help us "
          + "improve Josh by reporting this error at https://github.com/SchmidtDSE/josh/issues"
      );
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

  @Override
  public void pushConfig(String name) {
    Optional<EngineValue> configValue = bridge.getConfigOptional(name);
    push(configValue.orElseThrow(
        () -> new IllegalArgumentException("Config value not found: " + name)
    ));
  }

  @Override
  public void pushConfigWithDefault(String name) {
    EngineValue defaultValue = pop(); // Get default from stack
    Optional<EngineValue> configValue = bridge.getConfigOptional(name);
    push(configValue.orElse(defaultValue));
  }

  @Override
  public EventHandlerMachine writeDebug() {
    EngineValue value = pop();

    if (debugOutputFacade.isEmpty()) {
      // No debug output facade configured - push value back (pass-through)
      push(value);
      return this;
    }

    String message = formatDebugValue(value);
    long step = bridge.getAbsoluteTimestep();
    DebugEntityInfo info = getDebugEntityInfo();

    // Write with full context
    CombinedDebugOutputFacade facade = debugOutputFacade.get();
    facade.write(message, step, info.entityCategory, info.identifier, info.x, info.y);

    // Push the original value back (debug is a pass-through function)
    push(value);
    return this;
  }

  @Override
  public EventHandlerMachine debugVariadic(int argCount) {
    // Pop all values in reverse order (last arg is on top of stack)
    List<String> parts = new ArrayList<>();
    for (int i = 0; i < argCount; i++) {
      EngineValue value = pop();
      parts.add(0, formatDebugValue(value)); // Insert at beginning to reverse order
    }

    if (debugOutputFacade.isEmpty()) {
      // No debug output configured - push 0 count as result
      push(valueFactory.build(0, Units.of("count")));
      return this;
    }

    long step = bridge.getAbsoluteTimestep();
    DebugEntityInfo info = getDebugEntityInfo();

    // Write with full context
    CombinedDebugOutputFacade facade = debugOutputFacade.get();
    facade.write(
        String.join(" ", parts),
        step,
        info.entityCategory,
        info.identifier,
        info.x,
        info.y
    );

    // Push 0 count as result (allows debug in expressions)
    push(valueFactory.build(0, Units.of("count")));
    return this;
  }

  /**
   * Formats a value for debug output with cleaner formatting.
   *
   * <p>Applies formatting improvements:
   * <ul>
   *   <li>Strips surrounding quotes from string values</li>
   *   <li>Truncates floating point numbers to 4 decimal places</li>
   * </ul>
   *
   * @param value The EngineValue to format.
   * @return A formatted string representation.
   */
  private String formatDebugValue(EngineValue value) {
    String raw = value.getAsString();

    // Strip surrounding quotes from strings
    if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
      return raw.substring(1, raw.length() - 1);
    }

    // Try to truncate floating point numbers to 4 decimal places
    try {
      // Check if it looks like a decimal number (contains a dot)
      if (raw.contains(".") && !raw.contains(" ")) {
        double d = Double.parseDouble(raw);
        // Format to 4 decimal places, removing trailing zeros
        String formatted = String.format("%.4f", d);
        // Remove trailing zeros after decimal point (but keep at least one decimal place)
        formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", ".0");
        return formatted;
      }
    } catch (NumberFormatException e) {
      // Not a number, return as-is
    }

    return raw;
  }

  /**
   * Helper record to hold entity debug context information.
   */
  private record DebugEntityInfo(String entityCategory, String identifier, double x, double y) {}

  /**
   * Extracts debug context information from the current entity in scope.
   *
   * @return DebugEntityInfo with entity category, identifier, and location.
   */
  private DebugEntityInfo getDebugEntityInfo() {
    String entityCategory = "unknown";
    String identifier = "0";
    double x = 0.0;
    double y = 0.0;

    try {
      EngineValue currentValue = scope.get("current");
      if (currentValue != null) {
        MutableEntity current = currentValue.getAsMutableEntity();
        EntityType type = current.getEntityType();

        // Map EntityType to debug file key
        entityCategory = switch (type) {
          case AGENT -> "organism";
          case PATCH -> "patch";
          case SIMULATION -> "simulation";
          case DISTURBANCE -> "disturbance";
          default -> "unknown";
        };

        // Get unique identifier from Java identity hash
        identifier = Integer.toHexString(System.identityHashCode(current));

        // Get location - try entity's own geometry first, then fall back to "here"
        Optional<EngineGeometry> geometry = current.getGeometry();
        if (geometry.isPresent()) {
          x = geometry.get().getCenterX().doubleValue();
          y = geometry.get().getCenterY().doubleValue();
        } else if (scope.has("here")) {
          // For inner entities (organisms), use containing patch's location
          EngineValue hereValue = scope.get("here");
          if (hereValue != null) {
            Entity here = hereValue.getAsEntity();
            Optional<EngineGeometry> hereGeom = here.getGeometry();
            if (hereGeom.isPresent()) {
              x = hereGeom.get().getCenterX().doubleValue();
              y = hereGeom.get().getCenterY().doubleValue();
            }
          }
        }
      }
    } catch (Exception e) {
      // Fall back to defaults if we can't determine entity info
    }

    return new DebugEntityInfo(entityCategory, identifier, x, y);
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

  /**
   * Checks if the resolver is querying for patch entities directly.
   *
   * @param resolver The ValueResolver being used for the query.
   * @param patchName The name of the patch to check against.
   * @return true if the resolver path matches the patch name, indicating a query for patches.
   */
  private boolean isQueryForPatch(ValueResolver resolver, String patchName) {
    return resolver.toString().contains("ValueResolver(" + patchName + ")");
  }

  @Override
  public EngineValue peek() {
    return memory.peek();
  }

  @Override
  public void withLocalBinding(String name, EngineValue value, Runnable action) {
    // Create nested local scope that allows shadowing parent scope variables
    LocalScope nestedScope = new LocalScope(scope);
    nestedScope.defineConstantAllowShadowing(name, value);

    // Temporarily switch to nested scope
    LocalScope originalScope = this.scope;
    this.scope = nestedScope;

    try {
      action.run();
    } finally {
      // Always restore original scope, even if action throws
      this.scope = originalScope;
    }
  }

}
