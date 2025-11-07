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
      Set.of("current", "prior", "here", "meta", "parent");

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
      Optional<EngineValue> realValue = inner.getAttributeValue(name);
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

    Optional<EngineValue> currentValue = inner.getAttributeValue(name);
    return currentValue.orElseThrow(
        () -> new RuntimeException("Could not find value for " + name)
    );
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
      case "meta" -> Optional.of(valueFactory.build(inner.getMeta()));
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

}
