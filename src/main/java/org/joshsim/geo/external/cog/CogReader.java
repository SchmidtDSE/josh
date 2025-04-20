package org.joshsim.geo.external.cog;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.joshsim.geo.external.core.GridCoverageReader;
import org.joshsim.geo.geometry.EarthGeometry;

/**
 * Reader for Cloud Optimized GeoTIFF (COG) files.
 */
public class CogReader extends GridCoverageReader {
  static final CoverageProcessor processor = CoverageProcessor.getInstance();

  /**
   * Read a coverage from a COG file for the specified geometry.
   *
   * @param path Path to the COG file
   * @param geometry EngineGeometry defining the area of interest
   * @return GridCoverage2D containing the data within the geometry's bounds
   * @throws IOException if there is an error reading the file
   */

  public GridCoverage2D getCoverageFromIo(
      String path,
      EarthGeometry geometry
  ) throws IOException {

    GeoTiffReader reader = getCogReader(path);

    try {
      // Get the full coverage
      GridCoverage2D coverage = reader.read(null);

      // Create an envelope from the geometry bounds for subsetting
      ReferencedEnvelope envelope = new ReferencedEnvelope(
          geometry.getEnvelope().getMinimum(0), geometry.getEnvelope().getMaximum(0),
          geometry.getEnvelope().getMinimum(1), geometry.getEnvelope().getMaximum(1),
          coverage.getCoordinateReferenceSystem()
      );

      // Set up parameters for the crop operation
      final ParameterValueGroup parameters = processor.getOperation("CoverageCrop").getParameters();
      parameters.parameter("Source").setValue(coverage);
      parameters.parameter("Envelope").setValue(envelope);

      // Crop the coverage to the bounds of the geometry
      GridCoverage2D croppedCoverage = (GridCoverage2D) processor.doOperation(parameters);

      return croppedCoverage;
    } finally {
      reader.dispose();
    }
  }

  private static GeoTiffReader getCogReader(String path) throws IOException {
    if (path.startsWith("http://") || path.startsWith("https://")) {
      // Path is already a URL
      return new GeoTiffReader(path);
    } else {
      URL fileUrl = new File(path).toURI().toURL();
      // Path is a local file
      return new GeoTiffReader(fileUrl);
    }
  }
}
