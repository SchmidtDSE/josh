package org.joshsim.geo.geometry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.apache.sis.io.wkt.WKTDictionary;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

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
  private static final String GRID_CRS_AUTH = "JOSHSIM_GRID";

  /**
  * Creates a realized grid CRS from a grid CRS definition.
  *
  * @param definition The grid CRS definition
  * @throws FactoryException If CRS creation fails
  * @throws TransformException if transformation fails
  */
  public RealizedGridCrs(GridCrsDefinition definition)
      throws FactoryException, IOException, TransformException {
    this.definition = definition;

    // Create the base CRS from the provided code
    this.baseCrs = CRS.forCode(definition.getBaseCrsCode());

    // Generate a unique code for our grid CRS
    String gridCrsCode = generateGridCrsCode(definition);

    // Create appropriate WKT based on CRS type
    String wkt;
    if (baseCrs instanceof GeographicCRS) {
      wkt = createGridCrsWktFromGeo(definition, (GeographicCRS) baseCrs);
    } else {
      wkt = createGridCrsWktFromProj(definition, baseCrs);
    }

    // Register and retrieve the custom CRS
    WKTDictionary dictionary = new WKTDictionary(new DefaultCitation(GRID_CRS_AUTH));
    dictionary.load(new BufferedReader(new StringReader(wkt)));

    // Create the grid CRS from the WKT string
    this.gridProjectedCrs = CRS.forCode(gridCrsCode);
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
   * Creates a WKT string representing a projected CRS based on a geographic CRS.
   * Uses a two-step approach for clarity:
   * 1. First creates a Transverse Mercator projection from the geographic CRS
   * 2. Then applies an affine transformation to incorporate the cell size
   *
   * @param definition The grid CRS definition
   * @param geoCrs The base geographic CRS
   * @return WKT string for the grid CRS
   * @throws FactoryException If CRS creation fails
   * @throws IOException If WKT parsing fails
   */
  private String createGridCrsWktFromGeo(
      GridCrsDefinition definition,
      GeographicCRS geoCrs
  ) throws FactoryException, IOException {
    PatchBuilderExtents extents = definition.getExtents();
    final double topLeftX = extents.getTopLeftX().doubleValue();
    final double topLeftY = extents.getTopLeftY().doubleValue();

    // Generate a unique identifier for the intermediate and final CRSs
    final String uniqueId = generateGridCrsCode(definition).split(":")[1];

    // Step 1: Create intermediate Transverse Mercator projection
    StringBuilder tmWkt = new StringBuilder();
    tmWkt.append("ProjectedCRS[\"TM_").append(definition.getName()).append("\",\n");
    tmWkt.append("  BaseGeodCRS[\"").append(geoCrs.getName().getCode()).append("\",\n");

    // Add datum information
    tmWkt.append("    Datum[\"").append(geoCrs.getDatum().getName().getCode()).append("\"],\n");
    tmWkt.append("    CS[ellipsoidal, 2],\n");
    tmWkt.append("      Axis[\"Latitude\", north],\n");
    tmWkt.append("      Axis[\"Longitude\", east],\n");
    tmWkt.append("      Unit[\"degree\", 0.017453292519943295]\n");
    tmWkt.append("  ],\n");

    // Add Transverse Mercator projection parameters
    tmWkt.append("  Conversion[\"Transverse Mercator\",\n");
    tmWkt.append("    Method[\"Transverse Mercator\"],\n");
    tmWkt.append("    Parameter[\"central_meridian\", ").append(topLeftX).append("],\n");
    tmWkt.append("    Parameter[\"latitude_of_origin\", ").append(topLeftY).append("],\n");
    tmWkt.append("    Parameter[\"scale_factor\", 1.0],\n");
    tmWkt.append("    Parameter[\"false_easting\", 0.0],\n");
    tmWkt.append("    Parameter[\"false_northing\", 0.0]\n");
    tmWkt.append("  ],\n");

    // Add coordinate system for Transverse Mercator
    tmWkt.append("  CS[Cartesian, 2],\n");
    tmWkt.append("    Axis[\"Easting (E)\", east],\n");
    tmWkt.append("    Axis[\"Northing (N)\", north],\n");
    tmWkt.append("    Unit[\"metre\", 1],\n");

    // Add identifier for TM CRS
    tmWkt.append("  Id[\"").append(GRID_CRS_AUTH)
      .append("\", \"TM_").append(uniqueId).append("\"]]");

    // Register the Transverse Mercator CRS
    WKTDictionary tmDict = new WKTDictionary(new DefaultCitation(GRID_CRS_AUTH));
    tmDict.load(new BufferedReader(new StringReader(tmWkt.toString())));

    // Step 2: Create the Grid CRS with affine transformation
    StringBuilder gridWkt = new StringBuilder();
    gridWkt.append("ProjectedCRS[\"").append(definition.getName()).append("\",\n");

    // Reference the intermediate TM CRS
    gridWkt.append("  BaseProjectedCRS[\"TM_").append(definition.getName()).append("\"],\n");

    // Add affine transformation for cell size scaling
    gridWkt.append("  Conversion[\"Cell Size Scaling\",\n");
    gridWkt.append("    Method[\"Affine parametric transformation\"],\n");
    gridWkt.append("    Parameter[\"num_row\", 3],\n");
    gridWkt.append("    Parameter[\"num_col\", 3],\n");
    gridWkt.append("    Parameter[\"elt_0_0\", ").append(getCellSize()).append("],\n");
    gridWkt.append("    Parameter[\"elt_0_1\", 0.0],\n");
    gridWkt.append("    Parameter[\"elt_0_2\", 0.0],\n");
    gridWkt.append("    Parameter[\"elt_1_0\", 0.0],\n");
    gridWkt.append("    Parameter[\"elt_1_1\", ").append(getCellSize()).append("],\n");
    gridWkt.append("    Parameter[\"elt_1_2\", 0.0],\n");
    gridWkt.append("    Parameter[\"elt_2_0\", 0.0],\n");
    gridWkt.append("    Parameter[\"elt_2_1\", 0.0],\n");
    gridWkt.append("    Parameter[\"elt_2_2\", 1.0]\n");
    gridWkt.append("  ],\n");

    // Add coordinate system for final Grid CRS
    gridWkt.append("  CS[Cartesian, 2],\n");
    gridWkt.append("    Axis[\"Easting (E)\", east],\n");
    gridWkt.append("    Axis[\"Northing (N)\", north],\n");
    gridWkt.append("    Unit[\"metre\", 1],\n");

    // Add identifier for final Grid CRS
    gridWkt.append("  Id[\"").append(GRID_CRS_AUTH)
      .append("\", \"").append(uniqueId).append("\"]]");

    return gridWkt.toString();
  }

  /**
   * Creates a WKT string for a grid CRS based on a projected CRS.
   */
  private String createGridCrsWktFromProj(
        GridCrsDefinition definition,
        CoordinateReferenceSystem projCrs
  ) {
    PatchBuilderExtents extents = definition.getExtents();
    final double topLeftX = extents.getTopLeftX().doubleValue();
    final double topLeftY = extents.getTopLeftY().doubleValue();

    // Get a unique identifier for this grid CRS
    final String uniqueId = generateGridCrsCode(definition).split(":")[1];

    // Build the WKT string for a derived projected CRS
    StringBuilder wkt = new StringBuilder();
    wkt.append("ProjectedCRS[\"").append(definition.getName()).append("\",\n");

    // Reference the base projected CRS (simpler than recreating it)
    wkt.append("  BaseProjectedCRS[\"").append(projCrs.getName().getCode()).append("\"],\n");

    // Define a simple coordinate operation that maps grid to projected space
    wkt.append("  Conversion[\"Grid Mapping\",\n");
    wkt.append("    Method[\"Affine parametric transformation\"],\n");
    wkt.append("    Parameter[\"num_row\", 3],\n");
    wkt.append("    Parameter[\"num_col\", 3],\n");
    wkt.append("    Parameter[\"elt_0_0\", ").append(getCellSize()).append("],\n");
    wkt.append("    Parameter[\"elt_0_1\", 0.0],\n");
    wkt.append("    Parameter[\"elt_0_2\", ").append(topLeftX).append("],\n");
    wkt.append("    Parameter[\"elt_1_0\", 0.0],\n");
    wkt.append("    Parameter[\"elt_1_1\", ").append(getCellSize()).append("],\n");
    wkt.append("    Parameter[\"elt_1_2\", ").append(topLeftY).append("],\n");
    wkt.append("    Parameter[\"elt_2_0\", 0.0],\n");
    wkt.append("    Parameter[\"elt_2_1\", 0.0],\n");
    wkt.append("    Parameter[\"elt_2_2\", 1.0]\n");
    wkt.append("  ],\n");

    // Keep the same coordinate system
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
   * Creates a transform from grid coordinates to any target CRS.
   *
   * @param targetCrs The target coordinate reference system
   * @return A math transform for coordinate conversion
   * @throws FactoryException If the transform cannot be created
   */
  public MathTransform createGridToTargetCrsTransform(CoordinateReferenceSystem targetCrs)
      throws FactoryException {
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
   * Gets the cell size in CRS units after any conversion.
   *
   * @return The cell size in CRS units
   */
  public double getCellSize() {
    return getDefinition().getCellSize().doubleValue();
  }
}
