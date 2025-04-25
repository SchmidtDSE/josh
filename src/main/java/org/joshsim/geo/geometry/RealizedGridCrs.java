package org.joshsim.geo.geometry;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.measure.Units;
import org.apache.sis.parameter.DefaultParameterValueGroup;
import org.opengis.referencing.operation.OperationMethod;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.FactoryException;

/**
 * Real implementation of Grid CRS using direct construction of coordinate reference systems.
 * Uses a `Transverse Mercator` projection or affine transformation depending on the base CRS,
 * with a custom origin to minimize distortion in the area of interest.
 */
public class RealizedGridCrs {
  private final GridCrsDefinition definition;
  private final CoordinateReferenceSystem gridProjectedCrs;
  private final CoordinateReferenceSystem baseCrs;
  private static final String GRID_CRS_AUTH = "JOSHSIM_GRID";

  // SIS factories
  private final GeodeticObjectFactory geodeticObjectFactory = new GeodeticObjectFactory();
  private final MathTransformFactory mathTransformFactory = new DefaultMathTransformFactory();
  private final DefaultCoordinateOperationFactory coordinateOpsFactory = 
      new DefaultCoordinateOperationFactory();

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

    // Create appropriate CRS based on base CRS type
    if (baseCrs instanceof GeographicCRS) {
      this.gridProjectedCrs = createGridCrsFromGeo((GeographicCRS) baseCrs);
    } else {
      this.gridProjectedCrs = createGridCrsFromProj(baseCrs);
    }
  }

  private CartesianCS getGridCartesianCs() throws FactoryException {
    // Create a GeodeticObjectFactory
    GeodeticObjectFactory factory = new GeodeticObjectFactory();
    
    // Create easting and northing axes using the factory
    CoordinateSystemAxis eastingAxis = factory.createCoordinateSystemAxis(
        Map.of("name", "Easting"),
        "E",
        AxisDirection.EAST, 
        Units.METRE);
    
    CoordinateSystemAxis northingAxis = factory.createCoordinateSystemAxis(
        Map.of("name", "Northing"),
        "N",
        AxisDirection.NORTH,
        Units.METRE);
    
    // Create the cartesian CS with the axes using the factory
    CartesianCS cartCs = factory.createCartesianCS(
        Map.of("name", "Grid Cartesian CS"),
        eastingAxis,
        northingAxis);
        
    return cartCs;
  }

  /**
   * Creates a grid CRS based on a geographic CRS using direct construction.
   *
   * @param geoCrs The base geographic CRS
   * @return The projected grid CRS
   * @throws FactoryException If CRS creation fails
   */
  private CoordinateReferenceSystem createGridCrsFromGeo(GeographicCRS geoCrs) 
      throws FactoryException {
    PatchBuilderExtents extents = definition.getExtents();
    final double topLeftX = extents.getTopLeftX().doubleValue();
    final double topLeftY = extents.getTopLeftY().doubleValue();
    final double cellSize = getCellSize();

    // Set up the Transverse Mercator parameters
    ParameterValueGroup params = mathTransformFactory.getDefaultParameters("Transverse Mercator");
    params.parameter("central_meridian").setValue(topLeftX);
    params.parameter("latitude_of_origin").setValue(topLeftY);
    params.parameter("scale_factor").setValue(1.0);
    params.parameter("false_easting").setValue(0.0);
    params.parameter("false_northing").setValue(0.0);
    
    // Create the projection transform
    MathTransform projTransform = mathTransformFactory.createParameterizedTransform(params);

    // Create the grid scaling transform
    LinearTransform scaleTransform = MathTransforms.scale(cellSize, cellSize);
    
    // Chain the transforms: first project, then scale
    MathTransform concatenated = MathTransforms.concatenate(projTransform, scaleTransform);
    
    // Define operation method for our concatenated transform
    OperationMethod operationMethod = new DefaultOperationMethod(concatenated);

    // Create a conversion from the source CRS
    Map<String, Object> properties = new HashMap<>();
    properties.put("name", "Custom Grid Projection");
    Conversion conversion = coordinateOpsFactory.createDefiningConversion(
        properties,
        operationMethod, 
        params
    );

    // Create a cartesian coordinate system using the factory
    CartesianCS cartCs = getGridCartesianCs();

    // Create the projected CRS using the factory
    Map<String, Object> crsProperties = new HashMap<>();
    crsProperties.put("name", definition.getName());
    String uniqueId = generateGridCrsCode(definition);
    crsProperties.put("identifiers", uniqueId);
    
    ProjectedCRS gridCrs = geodeticObjectFactory.createProjectedCRS(
        crsProperties,
        geoCrs, 
        conversion, 
        cartCs
    );
    
    return gridCrs;
  }
  /**
   * Creates a grid CRS based on a projected CRS.
   *
   * @param projCrs The base projected CRS
   * @return The grid CRS
   * @throws FactoryException If CRS creation fails
   */
  private CoordinateReferenceSystem createGridCrsFromProj(CoordinateReferenceSystem projCrs) 
      throws FactoryException {
    PatchBuilderExtents extents = definition.getExtents();
    final double topLeftX = extents.getTopLeftX().doubleValue();
    final double topLeftY = extents.getTopLeftY().doubleValue();
    final double cellSize = getCellSize();
    
    // Create a math transform factory
    MathTransformFactory mtFactory = new DefaultMathTransformFactory();
    
    // Set up the Affine transform parameters
    ParameterValueGroup params = mtFactory.getDefaultParameters("Affine");
    params.parameter("num_row").setValue(3);
    params.parameter("num_col").setValue(3);
    params.parameter("elt_0_0").setValue(cellSize);
    params.parameter("elt_0_1").setValue(0.0);
    params.parameter("elt_0_2").setValue(topLeftX);
    params.parameter("elt_1_0").setValue(0.0);
    params.parameter("elt_1_1").setValue(cellSize);
    params.parameter("elt_1_2").setValue(topLeftY);
    params.parameter("elt_2_0").setValue(0.0);
    params.parameter("elt_2_1").setValue(0.0);
    params.parameter("elt_2_2").setValue(1.0);
    
    // Create the affine transform
    MathTransform affineTransform = mtFactory.createParameterizedTransform(params);
    
    // Create a cartesian coordinate system
    DefaultCartesianCS cartCS = DefaultCartesianCS.PROJECTED;
    
    // Create a conversion from the source CRS
    Map<String, Object> properties = new HashMap<>();
    properties.put("name", "Grid Mapping");
    DefaultConversion conversion = new DefaultConversion(
            properties,
            affineTransform, params);
    
    // Create the projected CRS
    Map<String, Object> crsProperties = new HashMap<>();
    crsProperties.put("name", definition.getName());
    String uniqueId = generateGridCrsCode(definition);
    crsProperties.put("identifiers", uniqueId);
    
    DefaultProjectedCRS gridCRS = new DefaultProjectedCRS(
            crsProperties,
            projCrs, conversion, cartCS);
    
    return gridCRS;
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