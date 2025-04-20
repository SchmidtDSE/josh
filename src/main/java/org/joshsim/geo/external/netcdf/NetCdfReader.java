package org.joshsim.geo.external.netcdf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.io.netcdf.NetCDFReader;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.joshsim.geo.external.core.GridCoverageReader;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.geo.geometry.EarthGeometry;

/**
 * Reader for NetCDF files.
 */
public class NetCdfReader extends GridCoverageReader {

  /**
   * Read a coverage from a NetCDF file for the specified geometry.
   *
   * @param path Path to the NetCDF file
   * @param geometry EngineGeometry defining the area of interest
   * @return GridCoverage2D containing the data within the geometry's bounds
   * @throws IOException if there is an error reading the file
   */
  @Override
  public GridCoverage2D getCoverageFromIo(
      String path,
      EarthGeometry geometry
  ) throws IOException {
    NetCDFReader reader = getNetCdfReader(path);

    try {
      // Get available coverages from the NetCDF file
      String[] names = reader.getGridCoverageNames();
      if (names.length == 0) {
        throw new IOException("No coverages found in NetCDF file: " + path);
      }

      // Get the first coverage (can be parameterized later if needed)
      GridCoverage2D coverage = reader.read(names[0], null);

      // Create an envelope from the geometry bounds for subsetting
      ReferencedEnvelope envelope = new ReferencedEnvelope(
          geometry.getEnvelope().getMinimum(0), geometry.getEnvelope().getMaximum(0),
          geometry.getEnvelope().getMinimum(1), geometry.getEnvelope().getMaximum(1),
          coverage.getCoordinateReferenceSystem()
      );

      // Crop the coverage to the bounds of the geometry
      Operations ops = new Operations(null);
      GridCoverage2D croppedCoverage = (GridCoverage2D) ops.crop(coverage, envelope);

      return croppedCoverage;
    } finally {
      reader.dispose();
    }
  }

  private static NetCDFReader getNetCdfReader(String path) throws IOException {
    if (path.startsWith("http://") || path.startsWith("https://")) {
      // Path is already a URL
      return new NetCDFReader(path, null);
    } else {
      // Path is a local file
      URL fileUrl = new File(path).toURI().toURL();
      return new NetCDFReader(fileUrl, null);
    }
  }
}
