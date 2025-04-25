package org.joshsim.geo.geometry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.referencing.operation.DefaultOperationMethod;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
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

  // Custom grid cell auth / unit defintion for Grid CRS
  private static final String GRID_CRS_AUTH = "JOSHSIM_GRID";
  private static final Unit<?> GRID_CELL = Units.valueOf("1").alternate("cell");

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

    // Create easting and northing axes using the factory
    CoordinateSystemAxis axisX = geodeticObjectFactory.createCoordinateSystemAxis(
        Map.of("name", "Grid Easting (Cell Index)"),
        "X",
        AxisDirection.EAST,
        GRID_CELL);

    CoordinateSystemAxis axisY = geodeticObjectFactory.createCoordinateSystemAxis(
        Map.of("name", "Grid Northing (Cell Index)"),
        "Y",
        AxisDirection.NORTH,
        GRID_CELL);

    // Create the cartesian CS with the axes using the factory
    CartesianCS cartCs = geodeticObjectFactory.createCartesianCS(
        Map.of("name", "Grid Cartesian CS"),
        axisX,
        axisY
    );

    return cartCs;
  }

  /**
   * Gets the top-left coordinates and cell size from the definition.
   *
   * @return Array containing [topLeftX, topLeftY, cellSize]
   */
  private double[] getGridParameters() {
    PatchBuilderExtents extents = definition.getExtents();
    final double topLeftX = extents.getTopLeftX().doubleValue();
    final double topLeftY = extents.getTopLeftY().doubleValue();
    final double cellSize = getCellSize();

    return new double[] {topLeftX, topLeftY, cellSize};
  }

  /**
   * Creates a defining conversion with the given name, method and parameters.
   *
   * @throws FactoryException if the conversion cannot be created
   */
  private Conversion createDefiningConversion(
        String name,
        OperationMethod method,
        ParameterValueGroup params
  ) throws FactoryException {
    return coordinateOpsFactory.createDefiningConversion(
        Map.of("name", name),
        method,
        params
    );
  }

  /**
   * Creates an operation method from a math transform.
   */
  private OperationMethod createOperationMethod(MathTransform transform) {
    return new DefaultOperationMethod(transform);
  }

  /**
   * Creates common CRS properties used by both CRS creation methods.
   *
   * @param name Name for the CRS
   * @return Map with common properties
   */
  private Map<String, Object> createCrsProperties(String name) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("name", name);
    String uniqueId = generateGridCrsCode(definition);
    properties.put("identifiers", uniqueId);
    return properties;
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
    // Get common parameters
    double[] params = getGridParameters();
    double topLeftX = params[0];
    double topLeftY = params[1];
    double cellSize = params[2];

    // Set up the Transverse Mercator parameters
    ParameterValueGroup tmParams = mathTransformFactory.getDefaultParameters("Transverse Mercator");
    tmParams.parameter("central_meridian").setValue(topLeftX);
    tmParams.parameter("latitude_of_origin").setValue(topLeftY);
    tmParams.parameter("scale_factor").setValue(1.0);
    tmParams.parameter("false_easting").setValue(0.0);
    tmParams.parameter("false_northing").setValue(0.0);

    // Create transforms
    MathTransform projTransform = mathTransformFactory.createParameterizedTransform(tmParams);
    LinearTransform scaleTransform = MathTransforms.scale(cellSize, cellSize);
    MathTransform concatenated = MathTransforms.concatenate(projTransform, scaleTransform);

    // Create method and conversion
    OperationMethod operationMethod = createOperationMethod(concatenated);
    Conversion conversion = createDefiningConversion(
        "Custom Grid Projection", operationMethod, tmParams);

    // Create the CRS
    CartesianCS cartCs = getGridCartesianCs();
    Map<String, Object> crsProperties = createCrsProperties(definition.getName());

    return geodeticObjectFactory.createProjectedCRS(
        crsProperties,
        geoCrs,
        conversion,
        cartCs
    );
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
    // Get common parameters
    double[] params = getGridParameters();
    double topLeftX = params[0];
    double topLeftY = params[1];
    double cellSize = params[2];

    // Set up the Affine transform parameters for our grid
    ParameterValueGroup affineParams = mathTransformFactory.getDefaultParameters("Affine");
    affineParams.parameter("num_row").setValue(3);
    affineParams.parameter("num_col").setValue(3);
    affineParams.parameter("elt_0_0").setValue(cellSize);
    affineParams.parameter("elt_0_1").setValue(0.0);
    affineParams.parameter("elt_0_2").setValue(topLeftX);
    affineParams.parameter("elt_1_0").setValue(0.0);
    affineParams.parameter("elt_1_1").setValue(cellSize);
    affineParams.parameter("elt_1_2").setValue(topLeftY);
    affineParams.parameter("elt_2_0").setValue(0.0);
    affineParams.parameter("elt_2_1").setValue(0.0);
    affineParams.parameter("elt_2_2").setValue(1.0);

    // Create the affine transform
    MathTransform affineTransform = mathTransformFactory.createParameterizedTransform(affineParams);

    // Define operation method for our affine transform
    OperationMethod operationMethod = createOperationMethod(affineTransform);

    // Create a cartesian coordinate system
    CartesianCS cartCs = getGridCartesianCs();

    // Create a conversion using the factory
    Conversion conversion = createDefiningConversion(
        "Grid Mapping",
        operationMethod,
        affineParams
    );

    // Create properties for the derived CRS
    Map<String, Object> crsProperties = createCrsProperties(definition.getName());

    // Add type check to prevent runtime errors
    if (!(projCrs instanceof SingleCRS)) {
      throw new FactoryException("Base CRS must be a SingleCRS for DefaultDerivedCRS");
    }

    // Create a derived CRS instead of a projected CRS
    return geodeticObjectFactory.createDerivedCRS(
        crsProperties,
        (SingleCRS) projCrs,
        conversion,
        cartCs
    );
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
