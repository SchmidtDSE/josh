package org.joshsim.geo.geometry;

import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.geometry.DirectPosition2D;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


class VanillaSisTest {

  /**
   * Demo entry point.
   *
   * @param  args  ignored.
   * @throws FactoryException   if an error occurred while creating the Coordinate Reference System (CRS).
   * @throws TransformException if an error occurred while transforming coordinates to the target CRS.
   */

  @Test
  public void testBasicConversion() throws FactoryException, TransformException {

    CoordinateReferenceSystem sourceCRS = CommonCRS.WGS84.geographic();
    CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.universal(35, -115);  // UTM zone for 40°N 14°E.
    CoordinateOperation operation = CRS.findOperation(sourceCRS, targetCRS, null);

    /*
      * The above lines are costly and should be performed only once before to project many points.
      * In this example, the operation that we got is valid for coordinates in geographic area from
      * 12°E to 18°E (UTM zone 33) and 0°N to 84°N.
      */
    System.out.println("Domain of validity:");
    System.out.println(CRS.getGeographicBoundingBox(operation));

    DirectPosition ptSrc = new DirectPosition2D(35, -115);           // 40°N 14°E
    DirectPosition ptDst = operation.getMathTransform().transform(ptSrc, null);

    System.out.println("Source: " + ptSrc);
    System.out.println("Target: " + ptDst);
  }
}
