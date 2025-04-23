package org.joshsim.geo.geometry;

import java.math.BigDecimal;
import java.util.Map;
import java.io.StringReader;
import java.io.IOException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.io.wkt.WKTDictionary;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import java.io.BufferedReader;

/**
 * Real implementation of Grid CRS using SIS's preffered route for a custom CRS.
 * Uses a `Transverse Mercator` projection, with a custom origin to ensure that 
 * we minimize the distortion of the grid in the area of interest. 
 * 
 */
public class RealizedGridCrs {
  private final GridCrsDefinition definition;
  private final CoordinateReferenceSystem gridProjectedCrs;
  private final CoordinateReferenceSystem baseCrs;
  private final double effectiveCellSize;
  private static final String GRID_CRS_AUTH = "JOSHSIM_GRID";
  
  /**
   * Creates a realized grid CRS from a grid CRS definition.
   *
   * @param definition The grid CRS definition
   * @throws FactoryException If CRS creation fails
   */
  public RealizedGridCrs(GridCrsDefinition definition) throws FactoryException, IOException {
    this.definition = definition;
    
    // Create the base CRS from the provided code
    this.baseCrs = CRS.forCode(definition.getBaseCrsCode());
    
    // Handle unit conversion if needed
    if (!definition.hasSameUnits()) {
      throw new UnsupportedOperationException("Implicit unit conversion not implemented yet. "
          + "Please use same units for cell size and CRS.");
    } else {
      this.effectiveCellSize = definition.getCellSize().doubleValue();
    }
    
    // If base CRS is geographic, create a proper projected CRS
    if (baseCrs instanceof GeographicCRS) {
      String gridCrsCode = generateGridCrsCode(definition);
      String wkt = createProjectedCrsWkt(definition, (GeographicCRS) baseCrs);
      
      // Create in-memory dictionary for the CRS and register it
      WKTDictionary dictionary = new WKTDictionary(new DefaultCitation(GRID_CRS_AUTH));
      dictionary.load(new BufferedReader(new StringReader(wkt)));
      
      // Now we can retrieve our CRS using the generated code
      this.gridProjectedCrs = CRS.forCode(gridCrsCode);
    } else {
      // For already-projected CRSs, we might handle differently
      // In this implementation, we'll throw an exception to be explicit
      throw new UnsupportedOperationException("Base CRS must be geographic. "
          + "Projected CRS as base not yet supported.");
    }
  }
  
  /**
   * Generates a unique CRS code for the grid CRS.
   */
  private String generateGridCrsCode(GridCrsDefinition definition) {
    // Create a unique identifier based on the definition properties
    String uniqueId = String.format("%s_%s_%s_%s_%s",
        definition.getName().replaceAll("\\W+", "_"),
        definition.getExtents().getTopLeftX().toString().replace(".", "_"),
        definition.getExtents().getTopLeftY().toString().replace(".", "_"),
        definition.getCellSize().toString().replace(".", "_"),
        definition.getBaseCrsCode().replace(":", "_"));
    
    // Format as authority:code
    return GRID_CRS_AUTH + ":" + uniqueId;
  }
  
  /**
   * Creates a WKT string representing a projected CRS based on the Transverse Mercator projection.
   */
  private String createProjectedCrsWkt(GridCrsDefinition definition, GeographicCRS geoCrs) {
    PatchBuilderExtents extents = definition.getExtents();
    final double topLeftX = extents.getTopLeftX().doubleValue();
    final double topLeftY = extents.getTopLeftY().doubleValue();
    
    // Get a unique identifier for this grid CRS
    final String uniqueId = generateGridCrsCode(definition).split(":")[1];
    
    // Build the WKT string for a Transverse Mercator projection
    StringBuilder wkt = new StringBuilder();
    wkt.append("ProjectedCRS[\"").append(definition.getName()).append("\",\n");
    wkt.append("  BaseGeodCRS[\"").append(geoCrs.getName().getCode()).append("\",\n");
    
    // Add datum information
    wkt.append("    Datum[\"").append(geoCrs.getDatum().getName().getCode()).append("\"],\n");
    wkt.append("    CS[ellipsoidal, 2],\n");
    wkt.append("      Axis[\"Latitude\", north],\n");
    wkt.append("      Axis[\"Longitude\", east],\n");
    wkt.append("      Unit[\"degree\", 0.017453292519943295]\n");
    wkt.append("  ],\n");
    
    // Add the projection parameters
    wkt.append("  Conversion[\"Grid Transverse Mercator\",\n");
    wkt.append("    Method[\"Transverse Mercator\"],\n");
    wkt.append("    Parameter[\"central_meridian\", ").append(topLeftX).append("],\n");
    wkt.append("    Parameter[\"latitude_of_origin\", ").append(topLeftY).append("],\n");
    wkt.append("    Parameter[\"scale_factor\", 1.0],\n");
    wkt.append("    Parameter[\"false_easting\", 0.0],\n");
    wkt.append("    Parameter[\"false_northing\", 0.0]\n");
    wkt.append("  ],\n");
    
    // Add coordinate system information
    wkt.append("  CS[Cartesian, 2],\n");
    wkt.append("    Axis[\"Easting (E)\", east],\n");
    wkt.append("    Axis[\"Northing (N)\", north],\n");
    wkt.append("    Unit[\"metre\", 1],\n");
    
    // Add the identifier
    wkt.append("  Id[\"").append(GRID_CRS_AUTH).append("\", \"").append(uniqueId).append("\"]]");
    
    return wkt.toString();
  }
  
  /**
   * Gets the grid coordinate reference system.
   *
   * @return The projected CRS for the grid
   */
  public CoordinateReferenceSystem getGridCrs() {
    return gridProjectedCrs;
  }
  
  /**
   * Gets the base Earth coordinate reference system.
   *
   * @return The base CRS (typically geographic)
   */
  public CoordinateReferenceSystem getBaseCrs() {
    return baseCrs;
  }
  
  /**
   * Creates a transform from grid coordinates to base CRS coordinates.
   *
   * @return A math transform for coordinate conversion
   * @throws FactoryException If the transform cannot be created
   */
  public MathTransform createGridToBaseCrsTransform() throws FactoryException {
    return CRS.findOperation(gridProjectedCrs, baseCrs, null).getMathTransform();
  }
  
  /**
   * Creates a transform from base CRS coordinates to grid coordinates.
   *
   * @return A math transform for coordinate conversion
   * @throws FactoryException If the transform cannot be created
   */
  public MathTransform createBaseCrsToGridTransform() throws FactoryException {
    return CRS.findOperation(baseCrs, gridProjectedCrs, null).getMathTransform();
  }
  
  /**
   * Creates a transform from grid coordinates to any target CRS.
   *
   * @param targetCrs The target coordinate reference system
   * @return A math transform for coordinate conversion
   * @throws FactoryException If the transform cannot be created
   */
  public MathTransform createGridToTargetCrsTransform(CoordinateReferenceSystem targetCrs) 
      throws FactoryException {
    if (Utilities.equalsIgnoreMetadata(baseCrs, targetCrs)) {
      return createGridToBaseCrsTransform();
    }
    
    return CRS.findOperation(gridProjectedCrs, targetCrs, null).getMathTransform();
  }
  
  /**
   * Gets the grid CRS definition used to create this realized CRS.
   *
   * @return The grid CRS definition
   */
  public GridCrsDefinition getDefinition() {
    return definition;
  }
  
  /**
   * Gets the effective cell size in CRS units after any conversion.
   *
   * @return The cell size in CRS units
   */
  public double getEffectiveCellSize() {
    return effectiveCellSize;
  }
}