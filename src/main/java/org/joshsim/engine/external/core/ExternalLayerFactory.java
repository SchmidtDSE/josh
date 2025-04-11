package org.joshsim.engine.external.core;

import org.joshsim.engine.external.cog.CogCacheLayer;
import org.joshsim.engine.external.cog.CogReader;
import org.joshsim.engine.external.netcdf.NetCdfCacheLayer;
import org.joshsim.engine.external.netcdf.NetCdfReader;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;

/**
 * Factory class for creating external layer chains.
 * It initializes the necessary components and layers
 * for external processing in the simulation engine.
 */
public class ExternalLayerFactory {
  private final EngineValueCaster caster;
  private final Units units;

  /**
   * Constructs an ExternalLayerFactory with the specified value caster and units.
   *
   * @param caster the engine value caster used for value conversions
   * @param units the units system used for value processing
   */
  public ExternalLayerFactory(EngineValueCaster caster, Units units) {
    this.caster = caster;
    this.units = units;
  }

  /**
   * Creates and initializes various decorators into a chain for processing.
   * This chain includes a COG reader layer, a cache layer, and a priming geometry layer which
   * iteratively builds the priming layer as a running intersection of all of the geometry
   * it has seen.
   *
   * @return the initialized external layer chain
   */
  public ExternalLayer createExtendingPrimingCogLayer() {
    // Create the base layer with COG reader
    ExternalLayer cogLayer = new GridCoverageExternalLayer(units, caster, new CogReader());

    // Add cache layer
    ExternalLayer cacheLayer = new CogCacheLayer(cogLayer);

    // Add priming geometry layer
    ExternalLayer primingLayer = new ExtendingPrimingGeometryLayer(cacheLayer);

    // Return decorated layers
    return primingLayer;
  }

  /**
   * Creates and initializes various decorators into a chain for NetCDF processing.
   * This chain includes a NetCDF reader layer, a cache layer, and a priming geometry layer which
   * iteratively builds the priming layer as a running intersection of all geometries it has seen.
   *
   * @return the initialized external layer chain
   */
  public ExternalLayer createExtendingPrimingNetcdfLayer() {
    // Create the base layer with NetCDF reader
    ExternalLayer netcdfLayer = new GridCoverageExternalLayer(units, caster, new NetCdfReader());

    // Add cache layer
    ExternalLayer cacheLayer = new NetCdfCacheLayer(netcdfLayer);

    // Add priming geometry layer
    ExternalLayer primingLayer = new ExtendingPrimingGeometryLayer(cacheLayer);

    // Return decorated layers
    return primingLayer;
  }

  /**
   * Creates and initializes a static priming geometry layer chain.
   * This chain includes a COG reader layer, a cache layer, and a static
   * priming geometry layer which does not change after initialization. This
   * can be used to create a very conservative layer (for eg, the simulation bounds)
   * as a primer, which may use more memory than absolutely necessary but reduce
   * unnecessary repeated hits to the COG itself.
   *
   * @param initialGeometry the geometry to use as a static primer
   * @return the initialized external layer chain
   */
  public ExternalLayer createStaticPrimingGeometryLayer(EngineGeometry initialGeometry) {
    // Create the base layer with COG reader
    ExternalLayer cogLayer = new GridCoverageExternalLayer(units, caster, new CogReader());

    // Add cache layer
    ExternalLayer cacheLayer = new CogCacheLayer(cogLayer);

    // Add static priming geometry layer
    StaticPrimingGeometryLayer staticPrimingLayer = new StaticPrimingGeometryLayer(cacheLayer);
    staticPrimingLayer.setPrimingGeometry(initialGeometry);

    // Return decorated layers
    return staticPrimingLayer;
  }

  /**
   * Creates and initializes a static priming geometry layer chain for NetCDF data.
   * This chain includes a NetCDF reader layer, a cache layer, and a static
   * priming geometry layer which does not change after initialization.
   *
   * @param initialGeometry the geometry to use as a static primer
   * @return the initialized external layer chain
   */
  public ExternalLayer createStaticPrimingNetcdfLayer(EngineGeometry initialGeometry) {
    // Create the base layer with NetCDF reader
    ExternalLayer netcdfLayer = new GridCoverageExternalLayer(units, caster, new NetCdfReader());

    // Add cache layer
    ExternalLayer cacheLayer = new NetCdfCacheLayer(netcdfLayer);

    // Add static priming geometry layer
    StaticPrimingGeometryLayer staticPrimingLayer = new StaticPrimingGeometryLayer(cacheLayer);
    staticPrimingLayer.setPrimingGeometry(initialGeometry);

    // Return decorated layers
    return staticPrimingLayer;
  }
}