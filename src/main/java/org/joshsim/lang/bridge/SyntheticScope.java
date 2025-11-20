/**
 * Structures to describe a scope which has synthetic attributes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MemberSpatialEntity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Scope which adds synthetic attributes to an entity.
 *
 * <p>Create a scope which adds synthetic attributes that allow referring to various entities within
 * a hierarchy like current, prior, here, meta, and parent. These are Josh language keywords that
 * are provided as convienence.</p>
 */
public class SyntheticScope implements Scope {

  public static final Set<String> SYNTHETIC_ATTRS =
      Set.of("current", "prior", "here", "meta", "parent", "stepCount", "year");

  private final ShadowingEntity inner;
  private final EngineValueFactory valueFactory;

  /**
   * Create a scope decorator around this entity.
   *
   * @param inner ShadowingEntity to use for the root.
   */
  public SyntheticScope(ShadowingEntity inner) {
    this.inner = inner;
    this.valueFactory = inner.getValueFactory();
  }

  /**
   * Create a scope decorator around this entity with a specified EngineValueFactory.
   *
   * @param inner ShadowingEntity to use for the root.
   * @param valueFactory EngineValueFactory to generate EngineValue instances.
   */
  public SyntheticScope(ShadowingEntity inner, EngineValueFactory valueFactory) {
    this.inner = inner;
    this.valueFactory = valueFactory;
  }

  @Override
  public EngineValue get(String name) {
    // Special handling for geoKey: check real attribute first, then fallback to synthetic
    if ("geoKey".equals(name)) {
      // CRITICAL: Bypass ShadowingEntity resolution to prevent circular dependencies
      // Use getInner() to read directly from storage without triggering handler execution
      MutableEntity innerEntity = inner.getInner();
      Optional<EngineValue> realValue = innerEntity.getAttributeValue(name);
      if (realValue.isPresent()) {
        return realValue.get();
      }
      // Fallback to synthetic computation for entities created without real geoKey attribute
      Optional<EngineValue> syntheticValue = computeGeoKey();
      if (syntheticValue.isPresent()) {
        return syntheticValue.get();
      }
      throw new RuntimeException("Could not find value for geoKey");
    }

    Optional<EngineValue> syntheticValue = getSynthetic(name);
    if (syntheticValue.isPresent()) {
      return syntheticValue.get();
    }

    // Try normal resolution path first (checks cache, resolves if needed)
    // This handles cross-attribute references (e.g., "fireProbability" accessing "isHighCover")
    try {
      Optional<EngineValue> currentValue = inner.getAttributeValue(name);
      return currentValue.orElseThrow(
          () -> new RuntimeException("Could not find value for " + name)
      );
    } catch (RuntimeException e) {
      // CIRCULAR DEPENDENCY: When evaluating handler RHS (e.g., "Trees" in
      // "Trees.end = prior.Trees | Trees"), bypass ShadowingEntity resolution to prevent
      // infinite recursion.
      //
      // WHY: ShadowingEntity.getAttributeValue() triggered resolveAttribute() for an attribute
      // that's already being resolved, causing circular dependency.
      //
      // SOLUTION: Call getInner() to get the DirectLockMutableEntity, then read from
      // its attributes[] array directly. This returns the stored (prior) value without
      // triggering handler execution.
      //
      // NOTE: ShadowingEntity throws RuntimeException with message
      // "Encountered a loop when resolving"
      if (e.getMessage() != null
          && e.getMessage().contains("Encountered a loop when resolving")) {
        MutableEntity innerEntity = inner.getInner();
        if (innerEntity == null) {
          throw e; // Re-throw if we can't bypass
        }
        Optional<EngineValue> storedValue = innerEntity.getAttributeValue(name);
        return storedValue.orElseThrow(
            () -> new RuntimeException("Could not find value for " + name)
        );
      }
      // Not a circular dependency - re-throw
      throw e;
    }
  }

  @Override
  public boolean has(String name) {
    if (inner.hasAttribute(name)) {
      return true;
    } else {
      return SYNTHETIC_ATTRS.contains(name);
    }
  }

  @Override
  public Set<String> getAttributes() {
    Set<String> newSet = new HashSet<>(SYNTHETIC_ATTRS);
    newSet.addAll(inner.getAttributeNames());
    return newSet;
  }

  private Optional<EngineValue> getSynthetic(String name) {
    return switch (name) {
      case "current" -> Optional.of(valueFactory.build(inner));
      case "prior" -> Optional.of(valueFactory.build(new PriorShadowingEntityDecorator(inner)));
      case "here" -> Optional.of(valueFactory.build(inner.getHere()));
      case "meta" -> {
        // Wrap meta entity in MetaScopeEntity to enable synthetic attribute access (meta.year)
        // Then wrap in CircularSafeEntity to propagate circular dependency protection
        // to nested attribute access (e.g., meta.fire.trigger.coverThreshold)
        Entity metaEntity = inner.getMeta();
        Entity wrappedMeta = new MetaScopeEntity(metaEntity, this);
        Entity safeEntity = new CircularSafeEntity(wrappedMeta);
        yield Optional.of(valueFactory.build(safeEntity));
      }
      case "parent" -> {
        // Get the wrapped inner entity from ShadowingEntity
        MutableEntity innerEntity = inner.getInner();

        // Check if this entity type has a parent (only MemberSpatialEntity does)
        if (innerEntity instanceof MemberSpatialEntity spatialEntity) {
          Entity parent = spatialEntity.getParent();

          // Safety check: ensure parent is not null (should never happen)
          if (parent != null) {
            yield Optional.of(valueFactory.build(parent));
          }
        }

        // No parent available for this entity type (Patch, Simulation, etc.)
        yield Optional.empty();
      }
      case "stepCount" -> computeStepCount();
      case "year" -> computeYear();
      default -> Optional.empty();
    };
  }

  /**
   * Compute geoKey synthetically for entities that don't have it as a real attribute.
   *
   * <p>This fallback ensures backward compatibility and handles entities created
   * outside the normal createEntity() flow (like patches).</p>
   *
   * @return Optional containing the computed geoKey value, or empty if no geometry available
   */
  private Optional<EngineValue> computeGeoKey() {
    // Get the wrapped inner entity from ShadowingEntity
    MutableEntity innerEntity = inner.getInner();

    // Get the GeoKey from the entity (returns Optional<GeoKey>)
    Optional<GeoKey> geoKeyOpt = innerEntity.getKey();

    // If GeoKey is present, convert to string and wrap in EngineValue
    if (geoKeyOpt.isPresent()) {
      GeoKey geoKey = geoKeyOpt.get();
      String geoKeyString = geoKey.toString();
      return Optional.of(valueFactory.build(geoKeyString, Units.EMPTY));
    }

    // No geometry available for this entity (e.g., Simulation)
    return Optional.empty();
  }

  /**
   * Compute meta.stepCount - current timestep (0-based).
   *
   * <p>This method returns the stepCount attribute from the meta (simulation) entity
   * if it's defined. Many tests explicitly define stepCount on the simulation, so this
   * allows access via the synthetic attribute.</p>
   *
   * @return Optional containing the stepCount value, or empty if not defined
   */
  private Optional<EngineValue> computeStepCount() {
    // Get stepCount from meta entity (simulation)
    Entity metaEntity = inner.getMeta();
    Optional<EngineValue> stepCountMaybe = metaEntity.getAttributeValue("stepCount");
    return stepCountMaybe;
  }

  /**
   * Compute meta.year - current simulation year.
   *
   * <p>This method returns the year attribute from the meta (simulation) entity
   * if it's defined. The year is typically derived from steps.low + stepCount,
   * but many simulations define it explicitly.</p>
   *
   * @return Optional containing the year value, or empty if not defined
   */
  private Optional<EngineValue> computeYear() {
    Entity metaEntity = inner.getMeta();

    // Try to get year directly from simulation if defined
    Optional<EngineValue> yearMaybe = metaEntity.getAttributeValue("year");
    return yearMaybe;
  }

  /**
   * Get a synthetic attribute value for the meta entity wrapper.
   *
   * <p>This method is used by MetaScopeEntity to provide synthetic attributes like
   * {@code year} and {@code stepCount} when accessed as attributes of the meta entity
   * (e.g., {@code meta.year}).</p>
   *
   * <p>For {@code year}: First tries to get it from the meta entity. If not defined,
   * computes it as steps.low + stepCount.</p>
   *
   * <p>For {@code stepCount}: Gets it from the meta entity (should be defined on simulation).</p>
   *
   * <p>We pass the unwrapped meta entity to avoid infinite recursion (MetaScopeEntity
   * wraps the meta entity, so we need to access the underlying entity directly).</p>
   *
   * @param name The name of the synthetic attribute to retrieve ("year" or "stepCount").
   * @param unwrappedMetaEntity The actual meta entity (not wrapped in MetaScopeEntity).
   * @return Optional containing the synthetic value, or empty if not a recognized synthetic.
   */
  public Optional<EngineValue> getSyntheticForMeta(String name, Entity unwrappedMetaEntity) {
    // If the meta entity is a ShadowingEntity, get the underlying MutableEntity
    // to avoid resolution logic that might fail for synthetic attributes
    Entity metaEntity = unwrappedMetaEntity;
    if (unwrappedMetaEntity instanceof ShadowingEntity shadowingMeta) {
      metaEntity = shadowingMeta.getInner();
    }

    return switch (name) {
      case "year", "step" -> {
        // Try to get year/step from meta entity first
        Optional<EngineValue> valueMaybe = metaEntity.getAttributeValue(name);
        if (valueMaybe.isPresent()) {
          yield valueMaybe;
        }

        // If not defined, compute as steps.low + stepCount
        // (both year and step represent the current simulation time)
        Optional<EngineValue> stepsLowMaybe = metaEntity.getAttributeValue("steps.low");
        Optional<EngineValue> stepCountMaybe = metaEntity.getAttributeValue("stepCount");

        if (stepsLowMaybe.isPresent() && stepCountMaybe.isPresent()) {
          EngineValue stepsLow = stepsLowMaybe.get();
          EngineValue stepCount = stepCountMaybe.get();
          EngineValue computed = stepsLow.add(stepCount);
          yield Optional.of(computed);
        }

        // Cannot compute year/step
        yield Optional.empty();
      }
      case "stepCount" -> metaEntity.getAttributeValue("stepCount");
      default -> Optional.empty();
    };
  }

}
