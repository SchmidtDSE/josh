/**
 * Structures for managing stateful step operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.joshsim.engine.config.Config;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.SingleValueScope;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.EnginePoint;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.precompute.DataGridLayer;


/**
 * Bridge that decouples the engine from the language interpreter and manages step state.
 */
public class MinimalEngineBridge implements EngineBridge {

  private static final long DEFAULT_START_STEP = 0;
  private static final long DEFAULT_END_STEP = 100;

  private final EngineGeometryFactory geometryFactory;
  private final MutableEntity simulation;
  private final ValueSupportFactory engineValueFactory;
  private final EngineValue startStep;
  private final EngineValue endStep;
  private final Converter converter;
  private final EntityPrototypeStore prototypeStore;
  private final Map<String, DataGridLayer> externalData;
  private final ExternalResourceGetter externalResourceGetter;
  private final Map<String, Optional<Config>> configData;
  private final ConfigGetter configGetter;

  private Optional<Replicate> replicate;
  private long absoluteStep;
  private EngineValue currentStep;
  private boolean inStep;

  // Spin-up / spin-down phase configuration. With no phase blocks these are zero/empty and the
  // clock behaves exactly as before: currentStep runs steps.low..steps.high.
  private long spinupSteps;
  private long spindownSteps;
  private long observedLow;
  private long observedHigh;

  private final Map<MutableEntity, MutableEntity> patchWrapperCache = new IdentityHashMap<>();

  /**
   * Constructs an EngineBridge to manipulate the specified simulation, replicate, and converter.
   *
   * <p>This class provides a bridge to decouple the simulation engine from the language
   * interpreter. It facilitates interactions with the engine's simulation and replicate components,
   * as well as value conversion.</p>
   *
   * @param engineValueFactory The factory to use for building engine values.
   * @param geometryFactory The factory to use for building engine geometries.
   * @param simulation The simulation instance to be used for retrieving or manipulating simulation
   *     data.
   * @param converter The converter for handling unit conversions between different engine values.
   * @param prototypeStore The set of prototypes to use to build new entities.
   * @param externalResourceGetter Strategy to use in loading external resources.
   * @param configGetter Strategy to use in loading configuration resources.
   */
  public MinimalEngineBridge(ValueSupportFactory engineValueFactory,
        EngineGeometryFactory geometryFactory, MutableEntity simulation,
        Converter converter, EntityPrototypeStore prototypeStore,
        ExternalResourceGetter externalResourceGetter, ConfigGetter configGetter) {
    this.engineValueFactory = engineValueFactory;
    this.geometryFactory = geometryFactory;
    this.simulation = simulation;
    this.converter = converter;
    this.prototypeStore = prototypeStore;
    this.externalResourceGetter = externalResourceGetter;
    this.configGetter = configGetter;
    this.configData = new ConcurrentHashMap<>();

    replicate = Optional.empty();

    simulation.startSubstep("constant");

    startStep = simulation
      .getAttributeValue("steps.low")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_START_STEP, Units.of("count")));

    endStep = simulation
      .getAttributeValue("steps.high")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_END_STEP, Units.of("count")));

    spinupSteps = readPhaseDuration("__spinupSteps");
    spindownSteps = readPhaseDuration("__spindownSteps");

    simulation.endSubstep();

    observedLow = startStep.getAsInt();
    observedHigh = endStep.getAsInt();

    // Anchor the clock at the observed period: spin-up counts backward into the negatives, so the
    // first observed step is always the same value regardless of spin-up length.
    currentStep = engineValueFactory.build(observedLow - spinupSteps, Units.of("count"));

    absoluteStep = 0;
    inStep = false;
    externalData = new ConcurrentHashMap<>();
  }

  /**
   * Constructs an EngineBridge with a given Replicate for testing.
   *
   * @param engineValueFactory The factory to use for building engine values.
   * @param simulation The simulation instance to be used for retrieving or manipulating simulation
   *     data.
   * @param converter The converter for handling unit conversions between different engine values.
   * @param externalResourceGetter Strategy to use in loading external resources.
   * @param replicate The replicate to use for testing.
   */
  public MinimalEngineBridge(ValueSupportFactory engineValueFactory,
        EngineGeometryFactory geometryFactory, MutableEntity simulation, Converter converter,
        EntityPrototypeStore prototypeStore, ExternalResourceGetter externalResourceGetter,
        ConfigGetter configGetter, Replicate replicate) {
    this.engineValueFactory = engineValueFactory;
    this.geometryFactory = geometryFactory;
    this.simulation = simulation;
    this.converter = converter;
    this.prototypeStore = prototypeStore;
    this.externalResourceGetter = externalResourceGetter;
    this.configGetter = configGetter;
    this.configData = new ConcurrentHashMap<>();
    this.replicate = Optional.of(replicate);

    simulation.startSubstep("constant");

    startStep = simulation
      .getAttributeValue("steps.low")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_START_STEP, Units.of("count")));

    endStep = simulation
      .getAttributeValue("steps.high")
      .orElseGet(() -> engineValueFactory.build(DEFAULT_END_STEP, Units.of("count")));

    spinupSteps = readPhaseDuration("__spinupSteps");
    spindownSteps = readPhaseDuration("__spindownSteps");

    simulation.endSubstep();

    observedLow = startStep.getAsInt();
    observedHigh = endStep.getAsInt();

    // Anchor the clock at the observed period: spin-up counts backward into the negatives, so the
    // first observed step is always the same value regardless of spin-up length.
    currentStep = engineValueFactory.build(observedLow - spinupSteps, Units.of("count"));

    absoluteStep = 0;
    inStep = false;
    externalData = new ConcurrentHashMap<>();
  }

  @Override
  public EngineGeometryFactory getGeometryFactory() {
    return geometryFactory;
  }

  @Override
  public MutableEntity getSimulation() {
    return simulation;
  }

  @Override
  public void startStep() {
    if (inStep) {
      throw new IllegalStateException("Tried to start a step before finishing the current one.");
    }

    inStep = true;
  }

  @Override
  public void endStep() {
    if (!inStep) {
      throw new IllegalStateException("Tried to end a step before starting the current one.");
    }

    getReplicate().saveTimeStep(currentStep.getAsInt());

    // Increment both the bridge's current step and the replicate's step number
    currentStep = engineValueFactory.build(currentStep.getAsInt() + 1, Units.of("count"));
    getReplicate().incrementStepNumber();
    absoluteStep++;
    inStep = false;
  }

  @Override
  public boolean isComplete() {
    return currentStep.getAsInt() > observedHigh + spindownSteps;
  }

  @Override
  public EngineValue getExternal(GeoKey key, String name, long step) {
    DataGridLayer layer = externalData.computeIfAbsent(name,
        k -> externalResourceGetter.getResource(name));
    return normalizePercent(layer.getAt(key, step));
  }

  /**
   * Reconcile "%" / "percent" tagged external values with Josh's percent convention.
   *
   * <p>Script and config percent literals (e.g. {@code 2 %}) are parsed as a fraction in
   * dimensionless {@code count} units (see {@code JoshValueVisitor} and
   * {@code JoshConfigParserVisitor}). Data preprocessed with {@code --units %} or
   * {@code --units percent} keeps the raw magnitude instead, so an external value like
   * {@code 0.4 percent} would otherwise compare directly against a config threshold like
   * {@code 0.004 count} (from {@code 0.4 %}) and appear ~100x larger than intended. Normalizing
   * here, at the single point all {@code external} reads pass through, avoids re-preprocessing
   * already-generated {@code .jshd}/{@code .jshdz} files.</p>
   *
   * @param value the raw value read from the external data source.
   * @return the value unchanged, or converted to a count-unit fraction if tagged percent.
   */
  private EngineValue normalizePercent(EngineValue value) {
    String unitsStr = value.getUnits().toString();
    boolean isPercent = unitsStr.equals("%") || unitsStr.equals("percent");
    if (!isPercent) {
      return value;
    }
    return engineValueFactory.buildForNumber(value.getAsDouble() / 100.0, Units.of("count"));
  }

  @Override
  public Optional<EngineValue> getConfigOptional(String name) {
    String[] parts = name.split("\\.", 2);
    if (parts.length != 2) {
      return Optional.empty();
    }
    String configName = parts[0];
    String variableName = parts[1];

    String configFileName = configName.endsWith(".jshc") ? configName : configName + ".jshc";

    Optional<Config> configOptional = configData.computeIfAbsent(configName,
        k -> configGetter.getConfig(configFileName));

    if (configOptional.isEmpty()) {
      return Optional.empty();
    }

    EngineValue value = configOptional.get().getValue(variableName);
    return Optional.ofNullable(value);
  }


  @Override
  public ValueSupportFactory getValueSupportFactory() {
    return engineValueFactory;
  }

  @Override
  public Optional<Entity> getPatch(EnginePoint enginePoint) {
    Query query = new Query(currentStep.getAsInt(), enginePoint);
    Iterable<Entity> patches = getReplicate().query(query);

    Iterator<Entity> iterator = patches.iterator();

    if (!iterator.hasNext()) {
      throw new IllegalStateException("Expected exactly one Patch, but found none.");
    }

    Entity patch = iterator.next();

    if (iterator.hasNext()) {
      throw new IllegalStateException("Expected exactly one Patch, but found more.");
    }

    return Optional.of(patch);
  }

  @Override
  public Iterable<MutableEntity> getCurrentPatches() {
    Query query = new Query(getCurrentTimestep());
    Iterable<MutableEntity> patches = getReplicate().getCurrentPatches();
    Iterable<MutableEntity> decorated = () -> new DecoratingShadowIterator(patches.iterator());
    return decorated;
  }

  @Override
  public List<Entity> getPriorPatches(EngineGeometry geometry) {
    Query query = new Query(getPriorTimestep(), geometry);
    List<Entity> patches = getReplicate().query(query);
    return patches;
  }

  @Override
  public List<Entity> getPriorPatches(GeometryMomento geometryMomento) {
    return getPriorPatches(geometryMomento.build());
  }

  @Override
  public EngineValue convert(EngineValue current, Units newUnits) {
    Conversion conversion = converter.getConversion(current.getUnits(), newUnits);
    CompiledCallable callable = conversion.getConversionCallable();
    EngineValue newValue = callable.evaluate(new SingleValueScope(current));
    return newValue.replaceUnits(newUnits);
  }

  @Override
  public long getCurrentTimestep() {
    return currentStep.getAsInt();
  }

  @Override
  public long getDataTimestep() {
    // The simulation's own "year" attribute (accessible in-model as meta.year) wins if the model
    // defines one at all -- the same override a direct meta.year read already honors via ordinary
    // attribute resolution (see SingleThreadEventHandlerMachine#pushAttribute). This lets a model
    // resample the forcing year during spin-up/spin-down (or anywhere else) with an ordinary
    // handler instead of a dedicated phase-only property, and keeps this the single place that
    // decides the answer so external reads and meta.year can never disagree.
    return simulation.getAttributeValue("year")
        .map(EngineValue::getAsInt)
        .orElseGet(this::getCurrentTimestep);
  }

  @Override
  public String getPhase() {
    return getPhase(currentStep.getAsInt());
  }

  @Override
  public String getPhase(long step) {
    if (step < observedLow) {
      return "spinup";
    }
    if (step > observedHigh) {
      return "spindown";
    }
    return "observed";
  }

  @Override
  public long getEarliestTimestep() {
    return observedLow - spinupSteps;
  }

  @Override
  public long getPriorTimestep() {
    return currentStep.getAsInt() - 1;
  }

  @Override
  public long getAbsoluteTimestep() {
    return absoluteStep;
  }

  @Override
  public long getStartTimestep() {
    return startStep.getAsInt();
  }

  @Override
  public long getEndTimestep() {
    return endStep.getAsInt();
  }

  @Override
  public Replicate getReplicate() {
    if (replicate.isEmpty()) {
      GridFromSimFactory factory = new GridFromSimFactory(this);
      PatchSet grid = factory.build(simulation);
      replicate = Optional.of(new Replicate(simulation, grid));
    }

    return replicate.get();
  }

  @Override
  public EntityPrototype getPrototype(String name) {
    if (!prototypeStore.has(name)) {
      throw new IllegalArgumentException("Unknown entity type: " + name);
    }

    return prototypeStore.get(name);
  }

  /**
   * Read a phase duration (in steps) from a synthetic constant-substep attribute.
   *
   * <p>Must be called while the {@code "constant"} substep is open. Returns 0 when the phase is
   * absent, which keeps the clock identical to a simulation without spin-up/spin-down.</p>
   *
   * @param attribute The synthetic duration attribute (e.g. {@code "__spinupSteps"}).
   * @return The phase length in steps, or 0 if not declared.
   */
  private long readPhaseDuration(String attribute) {
    return simulation.getAttributeValue(attribute).map(EngineValue::getAsInt).orElse(0L);
  }

  /**
   * Iterator that decorates patches with shadow tracking.
   */
  private class DecoratingShadowIterator implements Iterator<MutableEntity> {

    private final Iterator<MutableEntity> patches;

    /**
     * Create a new decorating iterator.
     *
     * @param patches the iterator of patches to decorate.
     */
    public DecoratingShadowIterator(Iterator<MutableEntity> patches) {
      this.patches = patches;
    }

    @Override
    public boolean hasNext() {
      return patches.hasNext();
    }

    @Override
    public MutableEntity next() {
      MutableEntity patch = patches.next();
      return patchWrapperCache.computeIfAbsent(
          patch,
          (key) -> new ShadowingEntity(engineValueFactory, key, simulation)
      );
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
