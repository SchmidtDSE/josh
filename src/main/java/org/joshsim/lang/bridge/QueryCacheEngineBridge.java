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
import org.joshsim.engine.entity.Simulation;
import org.joshsim.engine.geometry.GeoPoint;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.Converter;


/**
 * Bridge that caches query calls.
 */
public class QueryCacheEngineBridge extends EngineBridge {

  private final Map<GeometryMomento, List<Patch.Key>> cachedPatchesByGeometry;

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
  public Optional<ShadowingEntity> getPatch(GeoPoint point) {
    
  }

  @Override
  public Iterable<ShadowingEntity> getPriorPatches(Geometry geometry) {
    
  }

}
