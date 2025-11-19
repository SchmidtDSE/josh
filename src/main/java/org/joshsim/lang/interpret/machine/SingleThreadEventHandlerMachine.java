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
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
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
  private final LocalScope scope;
  private final EngineValueFactory valueFactory;
  private final Random random;
  private final boolean favorBigDecimal;
  private final Optional<org.joshsim.lang.io.CombinedTextWriter> debugWriter;

  private boolean inConversionGroup;
  private Optional<Units> conversionTarget;
  private boolean isEnded;

  /**
   * Create a new push-down automaton which operates on the given scope.
   *
   * @param bridge The EngineBridge through which to interact with the engine.
   * @param scope The scope in which to have this automaton perform its operations.
   * @param debugWriter Optional debug writer for writing debug messages.
   * @param seed Optional seed for random number generation. If present, provides deterministic
   *     behavior for testing. If empty, uses system time for truly random behavior.
   */
  public SingleThreadEventHandlerMachine(EngineBridge bridge, Scope scope,
      Optional<org.joshsim.lang.io.CombinedTextWriter> debugWriter, Optional<Long> seed) {
    this.bridge = bridge;
    this.scope = new LocalScope(scope);
    this.debugWriter = debugWriter;

    memory = new Stack<>();
    inConversionGroup = false;
    conversionTarget = Optional.empty();
    valueFactory = bridge.getEngineValueFactory();
    currentValueResolver = new ValueResolver(valueFactory, "current");
    favorBigDecimal = valueFactory.isFavoringBigDecimal();
    random = seed.isPresent() ? new Random(seed.get()) : new Random();
    isEnded = false;
  }

  /**
   * Create a new push-down automaton which operates on the given scope (legacy constructor).
   *
   * @param bridge The EngineBridge through which to interact with the engine.
   * @param scope The scope in which to have this automaton perform its operations.
   */
  public SingleThreadEventHandlerMachine(EngineBridge bridge, Scope scope) {
    this(bridge, scope, Optional.empty(), Optional.empty());
  }

  /**
   * Creates a descriptive error message when a value cannot be resolved.
   *
   * @param valueResolver The resolver that failed to find a value
   * @return An IllegalStateException with a helpful error message
   */
  private IllegalStateException createValueResolverError(ValueResolver valueResolver) {
    String path = valueResolver.toString().replace("ValueResolver(", "").replace(")", "");

    // Check if this is a meta attribute access
    if (path.startsWith("meta.")) {
      String attributeName = path.substring(5); // Remove "meta." prefix

      // Try to get available attributes from the scope
      java.util.Set<String> availableAttributes = scope.getAttributes();

      String message = String.format(
          "Unable to access '%s'. The attribute '%s' is not defined in your simulation block. "
              + "To access '%s' via the 'meta' namespace, define it in your simulation. "
              + "For example, add:\n\n"
              + "  %s.init = <initial value>\n"
              + "  %s.step = <step logic>\n\n"
              + "Available simulation attributes: %s",
          path,
          attributeName,
          path,
          attributeName,
          attributeName,
          availableAttributes.isEmpty() ? "none" : String.join(", ", availableAttributes)
      );

      return new IllegalStateException(message);
    }

    // For non-meta attributes, provide a simpler error with available attributes if possible
    java.util.Set<String> availableAttributes = scope.getAttributes();
    String attributeList = availableAttributes.isEmpty()
        ? ""
        : " Available attributes: " + String.join(", ", availableAttributes);

    return new IllegalStateException(
        String.format("Unable to get value for %s.%s", valueResolver, attributeList)
    );
  }

  @Override
  public EventHandlerMachine push(ValueResolver valueResolver) {
    Optional<EngineValue> value = valueResolver.get(scope);
    memory.push(value.orElseThrow(
        () -> createValueResolverError(valueResolver)
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
    // Logical operators expect boolean operands - no conversion needed
    EngineValue right = memory.pop();
    EngineValue left = memory.pop();

    boolean result = right.getAsBoolean() && left.getAsBoolean();
    EngineValue resultDecorated = valueFactory.build(result, EMPTY_UNITS);
    memory.push(resultDecorated);

    return this;
  }

  @Override
  public EventHandlerMachine or() {
    // Logical operators expect boolean operands - no conversion needed
    EngineValue right = memory.pop();
    EngineValue left = memory.pop();

    boolean result = right.getAsBoolean() || left.getAsBoolean();
    EngineValue resultDecorated = valueFactory.build(result, EMPTY_UNITS);
    memory.push(resultDecorated);

    return this;
  }

  @Override
  public EventHandlerMachine xor() {
    // Logical operators expect boolean operands - no conversion needed
    EngineValue right = memory.pop();
    EngineValue left = memory.pop();

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

    // Get next sequence ID from parent patch
    long sequence = 0L;
    if (parent instanceof org.joshsim.engine.entity.type.Patch) {
      sequence = ((org.joshsim.engine.entity.type.Patch) parent).getNextSequence();
    }

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
      MutableEntity newEntity;
      if (decoratedPrototype instanceof ShadowingEntityPrototype) {
        newEntity = ((ShadowingEntityPrototype) decoratedPrototype).build(sequence);
      } else {
        newEntity = decoratedPrototype.build();
      }

      // Add geoKey as real attribute BEFORE fast-forward (uses cached GeoKey from entity)
      // This ensures geoKey is available during init phase
      Optional<GeoKey> geoKeyOpt = newEntity.getKey();
      if (geoKeyOpt.isPresent()) {
        String geoKeyString = geoKeyOpt.get().toString();
        EngineValue geoKeyValue = valueFactory.build(geoKeyString, Units.EMPTY);
        newEntity.setAttributeValue("geoKey", geoKeyValue);
      }

      EntityFastForwarder.fastForward(newEntity, substep);
      result = valueFactory.build(newEntity);
    } else {
      // Pre-size ArrayList with known count to avoid growth overhead
      List<EngineValue> values = new ArrayList<>((int) count);
      for (int i = 0; i < count; i++) {
        MutableEntity newEntity;
        if (decoratedPrototype instanceof ShadowingEntityPrototype) {
          newEntity = ((ShadowingEntityPrototype) decoratedPrototype).build(sequence);
          sequence++; // Increment for next entity in batch
        } else {
          newEntity = decoratedPrototype.build();
        }

        // Add geoKey as real attribute BEFORE fast-forward (uses cached GeoKey from entity)
        // This ensures geoKey is available during init phase
        Optional<GeoKey> geoKeyOpt = newEntity.getKey();
        if (geoKeyOpt.isPresent()) {
          String geoKeyString = geoKeyOpt.get().toString();
          EngineValue geoKeyValue = valueFactory.build(geoKeyString, Units.EMPTY);
          newEntity.setAttributeValue("geoKey", geoKeyValue);
        }

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

    double minDouble = min.getAsDouble();
    double maxDouble = max.getAsDouble();

    double doubleResult;
    if (Math.abs(maxDouble - minDouble) < 1e-7) {
      doubleResult = minDouble;
    } else {
      doubleResult = random.nextDouble(minDouble, maxDouble);
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
    double randGauss = random.nextGaussian(meanDouble, stdDouble);

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
    conversionTarget = Optional.empty();  // Clear stale conversion target
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
  public EventHandlerMachine writeDebug(String message) {
    if (debugWriter.isEmpty()) {
      return this; // Zero overhead when not configured
    }

    // Get current entity type category (not name)
    String entityCategory = "unknown";
    try {
      MutableEntity current = scope.get("current").getAsMutableEntity();
      EntityType type = current.getEntityType();
      // Map EntityType to debug writer key
      entityCategory = switch (type) {
        case AGENT -> "organism";  // organisms map to AGENT type
        case PATCH -> "patch";
        case SIMULATION -> "simulation";
        default -> "unknown";
      };
    } catch (Exception e) {
      // Fallback if current not available
    }

    // Get current step
    long step = bridge.getCurrentTimestep();

    // Write to debug writer with entity category
    org.joshsim.lang.io.CombinedTextWriter writer = debugWriter.get();
    writer.setCurrentEntityType(entityCategory);
    writer.write(message, step);

    return this;
  }

  @Override
  public EventHandlerMachine debugVariadic(int count) {
    if (debugWriter.isEmpty()) {
      // Pop values from stack even if not using them (maintain stack state)
      for (int i = 0; i < count; i++) {
        pop();
      }
      return this; // Zero overhead when not configured
    }

    // Pop all values and collect as strings (in reverse order)
    List<String> values = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      values.add(pop().getAsString());
    }

    // Reverse to get original order
    java.util.Collections.reverse(values);

    // Concatenate all values with spaces
    String message = String.join(" ", values);

    // Write using the single-argument writeDebug
    return writeDebug(message);
  }

}
