/**
 * Structures to cache expensive query operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.engine.EngineValueFactory;


/**
 * Bridge decorator that caches query calls.
 */
public class QueryCacheEngineBridge extends MinimalEngineBridge {

  private final Map<GeometryMomento, List<GeoKey>> cachedPatchesByGeometry;

  /**
   * Constructs a caching EngineBridge to manipulate simulation and converter.
   *
   * <p>Version of EngineBridge which caches patches found in queries that cross the bridge,
   * speeding up geospatial operations in exchange for some in-memory space.</p>
   *
   * @param valueFactory Factory to use in constructing engine values.
   * @param geometryFactory Factory to use in constructing engine geometries.
   * @param simulation The simulation instance to be used for retrieving or manipulating simulation
   *     data.
   * @param converter The converter for handling unit conversions between different engine values.
   * @param prototypeStore The set of prototypes to use to build new entities.
   * @param externalResourceGetter Strategy to get external resources.
   * @param configGetter Strategy to get configuration resources.
   */
  public QueryCacheEngineBridge(EngineValueFactory valueFactory,
        EngineGeometryFactory geometryFactory, MutableEntity simulation, Converter converter,
        EntityPrototypeStore prototypeStore, ExternalResourceGetter externalResourceGetter,
        ConfigGetter configGetter) {

    super(
        valueFactory,
        geometryFactory,
        simulation,
        converter,
        prototypeStore,
        externalResourceGetter,
        configGetter
    );
    cachedPatchesByGeometry = new HashMap<>();
  }

  /**
   * Constructs a caching EngineBridge with a given Replicate for testing.
   *
   * @param valueFactory Factory to use in constructing engine values.
   * @param geometryFactory Factory to use in constructing engine geometries.
   * @param simulation The simulation instance to be used for retrieving or manipulating simulation
   *     data.
   * @param converter The converter for handling unit conversions between different engine values.
   * @param prototypeStore The set of prototypes to use to build new entities.
   * @param externalResourceGetter Strategy to get external resources.
   * @param configGetter Strategy to get configuration resources.
   * @param replicate The replicate to use for testing.
   */
  QueryCacheEngineBridge(EngineValueFactory valueFactory, EngineGeometryFactory geometryFactory,
        MutableEntity simulation, Converter converter, EntityPrototypeStore prototypeStore,
        ExternalResourceGetter externalResourceGetter, ConfigGetter configGetter,
        Replicate replicate) {
    super(
        valueFactory,
        geometryFactory,
        simulation,
        converter,
        prototypeStore,
        externalResourceGetter,
        configGetter,
        replicate
    );
    cachedPatchesByGeometry = new HashMap<>();
  }

  @Override
  public List<Entity> getPriorPatches(GeometryMomento geometryMomento) {
    if (cachedPatchesByGeometry.containsKey(geometryMomento)) {
      // Cache hit: retrieve patches by keys using direct iteration
      List<GeoKey> keys = cachedPatchesByGeometry.get(geometryMomento);
      long priorTimestep = getPriorTimestep();

      List<Entity> result = new ArrayList<>(keys.size());
      for (GeoKey key : keys) {
        result.add(getReplicate().getPatchByKey(key, priorTimestep));
      }
      return result;
    } else {
      // Cache miss: query patches and extract keys using direct iteration
      List<Entity> entities = getPriorPatches(geometryMomento.build());

      List<GeoKey> geoKeys = new ArrayList<>();
      for (Entity entity : entities) {
        Optional<GeoKey> keyMaybe = entity.getKey();
        if (keyMaybe.isPresent()) {
          geoKeys.add(keyMaybe.get());
        }
      }

      cachedPatchesByGeometry.put(geometryMomento, geoKeys);
      return entities;
    }
  }

}
