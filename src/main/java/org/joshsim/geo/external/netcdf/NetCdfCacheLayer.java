package org.joshsim.geo.external.netcdf;

import org.joshsim.geo.external.core.ExternalLayer;
import org.joshsim.geo.external.core.GridCoverageCacheLayer;

/**
 * Concrete implementation of GridCoverageCacheLayer for NetCDF files.
 */
public class NetCdfCacheLayer extends GridCoverageCacheLayer {
  /**
   * Constructs a NetCdfCacheLayer with a decorated external layer.
   *
   * @param decoratedLayer the external layer to be decorated
   */
  public NetCdfCacheLayer(ExternalLayer decoratedLayer) {
    super(decoratedLayer, new NetCdfReader());
  }
}
