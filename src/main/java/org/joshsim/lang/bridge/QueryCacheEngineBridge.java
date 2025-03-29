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
import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.entity.GeoKey;
import org.joshsim.engine.entity.Simulation;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.Converter;


/**
 * Bridge decorator that caches query calls.
 */
public class QueryCacheEngineBridge extends MinimalEngineBridge {

  private final Map<GeometryMomento, List<GeoKey>> cachedPatchesByGeometry;

  /**
   * Constructs a caching EngineBridge to manipulate simulation, replicate, and converter.
   *
   * <p>Version of EngineBridge which caches patches found in queries that cross the bridge,
   * speeding up geospatial operations in exchange for some in-memory space.</p>
   *
   * @param simulation The simulation instance to be used for retrieving or manipulating simulation
   *     data.
   * @param replicate The replicate instance for querying patches and other simulation data.
   * @param converter The converter for handling unit conversions between different engine values.
   */
  public QueryCacheEngineBridge(Simulation simulation, Replicate replicate, Converter converter) {
    super(simulation, replicate, converter);
    cachedPatchesByGeometry = new HashMap<>();
  }

  @Override
  public Iterable<Entity> getPriorPatches(GeometryMomento geometryMomento) {
    if (cachedPatchesByGeometry.containsKey(geometryMomento)) {
      List<GeoKey> keys = cachedPatchesByGeometry.get(geometryMomento);

      Simulation simulation = null;
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
