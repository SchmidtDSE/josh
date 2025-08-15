/**
 * Structures to cache expensive query operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
  public Iterable<Entity> getPriorPatches(GeometryMomento geometryMomento) {
    if (cachedPatchesByGeometry.containsKey(geometryMomento)) {
      List<GeoKey> keys = cachedPatchesByGeometry.get(geometryMomento);

      long priorTimestep = getPriorTimestep();

      return keys.stream()
          .map((key) -> getReplicate().getPatchByKey(key, priorTimestep))
          .collect(Collectors.toList());
    } else {
      Iterable<Entity> entities = getPriorPatches(geometryMomento.build());

      List<GeoKey> geoKeys = StreamSupport.stream(entities.spliterator(), false)
          .map(Entity::getKey)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toList());

      cachedPatchesByGeometry.put(geometryMomento, geoKeys);
      return entities;
    }
  }

}
