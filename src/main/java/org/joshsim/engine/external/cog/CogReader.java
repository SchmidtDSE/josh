package org.joshsim.engine.external.cog;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.type.DecimalScalar;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Reader for Cloud Optimized GeoTIFF (COG) files that extracts data
 * based on specified geometries.
 */
public class CogReader {
  private final EngineValueCaster caster;
  private final Units units;
  
  /**
   * Constructs a CogReader with specified caster and units.
   *
   * @param caster The engine value caster to use for creating scalar values
   * @param units The units for the returned distribution
   */
  public CogReader(EngineValueCaster caster, Units units) {
    this.caster = caster;
    this.units = units;
  }
  
  /**
   * Read values from a COG file for the specified geometry.
   *
   * @param path Path to the COG file
   * @param geometry Geometry defining the area of interest
   * @return Distribution of values from the COG within the geometry
   * @throws IOException if there is an error reading the file
   */
  public RealizedDistribution readValues(String path, Geometry geometry) throws IOException {
    try (DataStore store = DataStores.open(new File(path))) {
      // Get the first image in the GeoTIFF file
      Collection<? extends Resource> allImages = ((Aggregate) store).components();
      GridCoverageResource firstImage = (GridCoverageResource) allImages.iterator().next();
      
      // Create an envelope from the geometry bounds
      GeneralEnvelope areaOfInterest = geometry.getEnvelope();
      
      // Read data from the file for the specified area
      GridCoverage coverage = firstImage.read(new GridGeometry(areaOfInterest), null);
      
      // Convert the grid coverage to a list of engine values
      List<EngineValue> values = extractValuesFromCoverage(coverage, geometry);
      
      // Create and return a realized distribution
      return new RealizedDistribution(caster, values, units);
    } catch (DataStoreException e) {
      throw new IOException("Failed to read COG file: " + path, e);
    }
  }

  /**
   * Extracts values from a grid coverage and converts them to EngineValue objects.
   *
   * @param coverage The grid coverage to extract values from
   * @param geometry The geometry used for filtering points (optional)
   * @return A list of EngineValue objects
   */
  private List<EngineValue> extractValuesFromCoverage(GridCoverage coverage, Geometry geometry) {
    List<EngineValue> values = new ArrayList<>();
    
    // Get the grid extent
    GridExtent extent = coverage.getGridGeometry().getExtent();
    long width = extent.getSize(0);
    long height = extent.getSize(1);
    
    // Get the CRS from the coverage
    CoordinateReferenceSystem coverageCrs = coverage.getCoordinateReferenceSystem();
    // Ensure consistent X,Y ordering (same as GridBuilder)
    coverageCrs = AbstractCRS.castOrCopy(coverageCrs).forConvention(AxesConvention.RIGHT_HANDED);
    
    // Create an evaluator to get pixel values
    GridCoverage.Evaluator evaluator = coverage.evaluator();
    
    try {
      // Iterate through grid cells
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          // Convert grid coordinates to CRS coordinates
          DirectPosition2D gridPos = new DirectPosition2D(coverageCrs, x, y);
          
          // Get direct position in the coverage's CRS
          DirectPosition2D worldPos = convertGridToWorld(coverage, gridPos);
          
          // Check if this point is within our geometry
          if (geometry == null || geometry.intersects(
                  BigDecimal.valueOf(worldPos.getX()),
                  BigDecimal.valueOf(worldPos.getY()))
              ) {
              
            // Apply the evaluator to get the value at this position
            double[] result = evaluator.apply(gridPos);
            
            // Get the first band value (assuming single-band data)
            double value = result[0];
            
            // Skip NaN or invalid values
            if (!Double.isNaN(value) && Double.isFinite(value)) {
              // Create a scalar value and add it to the list
              BigDecimal decimalValue = BigDecimal.valueOf(value);
              DecimalScalar scalar = new DecimalScalar(caster, decimalValue, units);
              values.add(scalar);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error extracting values from coverage", e);
    }
    
    return values;
  }
  
  /**
   * Converts grid coordinates to world coordinates using the coverage's grid geometry.
   *
   * @param coverage The grid coverage
   * @param gridPos The grid position
   * @return The world position
   */
  private DirectPosition2D convertGridToWorld(GridCoverage coverage, DirectPosition2D gridPos) {
    try {
      // Get the transform from grid to CRS coordinates, specifying CELL_CENTER
      MathTransform gridToCrs = coverage.getGridGeometry()
          .getGridToCRS(PixelInCell.CELL_CENTER);
      
      // Create a new position for the result
      DirectPosition2D worldPos = new DirectPosition2D();
      
      // Apply the transformation
      gridToCrs.transform(gridPos, worldPos);
      
      return worldPos;
    } catch (TransformException e) {
      throw new RuntimeException("Failed to transform grid coordinates to world coordinates", e);
    }
  }
}