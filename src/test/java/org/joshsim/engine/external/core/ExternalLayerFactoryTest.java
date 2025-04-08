package org.joshsim.engine.external.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.referencing.CRS;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.joshsim.engine.external.cog.CogExternalLayer;
import org.joshsim.engine.external.cog.CogReader;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.joshsim.engine.value.type.Scalar;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Rectangle;
import org.mockito.ArgumentCaptor;
import org.opengis.util.FactoryException;

/**
 * Unit tests for the ExternalLayerFactory class, ensuring that it correctly
 * creates and manages external layers for processing geospatial data with caching.
 */
public class ExternalLayerFactoryTest {
  private EngineValueCaster caster;
  private Units units;
  private ExternalLayerFactory factory;
  private SpatialContext spatialContext;
  private CoordinateReferenceSystem wgs84;
  private static final String COG_NOV_2021 = "assets/test/cog/nclimgrid-prcp-202111.tif";
  private static final String COG_DEC_2021 = "assets/test/cog/nclimgrid-prcp-202112.tif";

  @BeforeAll
  static void setHeadlessMode() {
    // Enable headless mode to avoid X11 dependencies
    System.setProperty("java.awt.headless", "true");
  }

  @BeforeEach
  void setup() throws FactoryException {
    caster = new EngineValueWideningCaster();
    units = new Units("mm");
    factory = new ExternalLayerFactory(caster, units);
    spatialContext = SpatialContext.GEO;
    wgs84 = CRS.forCode("EPSG:4326"); // WGS84
  }

  private EngineGeometry createBoxGeometry(double minX, double minY, double maxX, double maxY) {
    return new Geometry(spatialContext.getShapeFactory().rect(minX, maxX, minY, maxY), wgs84);
  }

  private Request createFileRequest(String path, EngineGeometry geometry) {
    return new Request("file", "", path, Optional.of(geometry), Optional.empty());
  }

  @Test
  void testChainStructureIsCorrect() {
    ExternalLayer chain = factory.createExtendingPrimingCogLayer();

    // Verify the chain structure using instanceof checks
    assertTrue(chain instanceof ExtendingPrimingGeometryLayer,
        "Outer layer should be ExtendingPrimingGeometryLayer");

    ExternalLayer inner1 = ((ExternalLayerDecorator) chain).getDecoratedLayer();
    assertTrue(inner1 instanceof ExternalPathCacheLayer,
        "Middle layer should be ExternalPathCacheLayer");

    ExternalLayer inner2 = ((ExternalLayerDecorator) inner1).getDecoratedLayer();
    assertTrue(inner2 instanceof CogExternalLayer,
        "Inner layer should be CogExternalLayer");
  }

  @Test
  void testChainReadsCogFiles() {
    // Create a EngineGeometry in the US where the test data has coverage
    EngineGeometry testArea = createBoxGeometry(-100.0, 40.0, -97.0, 44.0);
    Request request = createFileRequest(COG_NOV_2021, testArea);

    // Test the entire chain
    ExternalLayer chain = factory.createExtendingPrimingCogLayer();
    RealizedDistribution result = chain.fulfill(request);

    // Verify results
    assertNotNull(result);
    assertTrue(result.getSize().isPresent());
    assertTrue(result.getSize().get() > 0);
  }

  @Test
  void testCachingPerformance() {
    EngineGeometry testArea = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);
    Request request = createFileRequest(COG_NOV_2021, testArea);
    ExternalLayer chain = factory.createExtendingPrimingCogLayer();

    // First request should read from file
    long startTime1 = System.currentTimeMillis();
    RealizedDistribution result1 = chain.fulfill(request);
    long duration1 = System.currentTimeMillis() - startTime1;

    // Second request should use cache
    long startTime2 = System.currentTimeMillis();
    RealizedDistribution result2 = chain.fulfill(request);
    long duration2 = System.currentTimeMillis() - startTime2;

    // Verify cache is working
    assertTrue(duration2 < duration1, "Second request should be faster due to caching");
    assertEquals(result1.getSize(), result2.getSize());

    // Verify statistics match
    Optional<Scalar> mean1 = result1.getMean();
    Optional<Scalar> mean2 = result2.getMean();
    assertTrue(mean1.isPresent() && mean2.isPresent());
    assertEquals(0, mean1.get().getAsDecimal().compareTo(mean2.get().getAsDecimal()));
  }

  @Test
  void testIndividualLayers() throws IOException {
    // Create test EngineGeometry and requests
    EngineGeometry testArea = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);
    Request request = createFileRequest(COG_NOV_2021, testArea);

    // Test CogExternalLayer
    CogExternalLayer cogLayer = new CogExternalLayer(units, caster);
    RealizedDistribution cogResult = cogLayer.fulfill(request);
    assertNotNull(cogResult);
    assertTrue(cogResult.getSize().isPresent() && cogResult.getSize().get() > 0);

    // Test ExternalPathCacheLayer
    ExternalPathCacheLayer cacheLayer = new ExternalPathCacheLayer(cogLayer);
    assertEquals(0, cacheLayer.getCacheSize());

    // First request should populate cache
    RealizedDistribution cacheResult1 = cacheLayer.fulfill(request);
    assertEquals(1, cacheLayer.getCacheSize());

    // Second request should use cache
    RealizedDistribution cacheResult2 = cacheLayer.fulfill(request);
    assertEquals(1, cacheLayer.getCacheSize());
    assertEquals(cacheResult1.getSize(), cacheResult2.getSize());

    // Test cache clearing
    cacheLayer.clearCache();
    assertEquals(0, cacheLayer.getCacheSize());

    // Test ExtendingPrimingGeometryLayer
    ExtendingPrimingGeometryLayer primingLayer = new ExtendingPrimingGeometryLayer(cacheLayer);
    assertFalse(primingLayer.getPrimingGeometry().isPresent());

    // First request should establish priming geometry
    primingLayer.fulfill(request);
    assertTrue(primingLayer.getPrimingGeometry().isPresent());

    // Verify priming EngineGeometry matches our request geometry
    EngineGeometry primingGeom = primingLayer.getPrimingGeometry().get();
    Rectangle primingRect = (Rectangle) primingGeom.getShape();
    assertEquals(-100.0, primingRect.getMinX(), 0.000001);
    assertEquals(-99.0, primingRect.getMaxX(), 0.000001);
    assertEquals(40.0, primingRect.getMinY(), 0.000001);
    assertEquals(41.0, primingRect.getMaxY(), 0.000001);
  }

  @Test
  void testExtendingPrimingGeometry() {
    // Create two different but overlapping test areas
    EngineGeometry area1 = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);
    EngineGeometry area2 = createBoxGeometry(-99.5, 40.5, -98.5, 41.5);

    Request request1 = createFileRequest(COG_NOV_2021, area1);
    Request request2 = createFileRequest(COG_NOV_2021, area2);

    // Create mock layers to verify behavior
    CogExternalLayer mockCogLayer = mock(CogExternalLayer.class);
    ExternalPathCacheLayer cacheLayer = new ExternalPathCacheLayer(mockCogLayer);
    ExtendingPrimingGeometryLayer primingLayer = new ExtendingPrimingGeometryLayer(cacheLayer);

    // First request should set priming EngineGeometry to area1
    primingLayer.fulfill(request1);
    assertTrue(primingLayer.getPrimingGeometry().isPresent());

    // Save the first priming EngineGeometry for comparison
    EngineGeometry firstPrimingGeom = primingLayer.getPrimingGeometry().get();

    // Second request should extend priming EngineGeometry to include area2
    primingLayer.fulfill(request2);
    assertTrue(primingLayer.getPrimingGeometry().isPresent());

    // The new priming EngineGeometry should be different than the first
    EngineGeometry extendedPrimingGeom = primingLayer.getPrimingGeometry().get();
    assertNotEquals(firstPrimingGeom, extendedPrimingGeom);

    // Verify the extended EngineGeometry contains both original areas
    // Note: This test currently uses getConvexHull which isn't implemented yet
    // So we're just verifying the EngineGeometry changed, not its specific shape
  }

  @Test
  void testPrimingGeometryPropagation() {
    // Create a test area
    EngineGeometry testArea = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);
    Request request = createFileRequest(COG_NOV_2021, testArea);

    // Create mock layers to verify behavior
    CogExternalLayer mockCogLayer = mock(CogExternalLayer.class);
    ExternalPathCacheLayer cacheLayer = spy(new ExternalPathCacheLayer(mockCogLayer));
    ExtendingPrimingGeometryLayer primingLayer = new ExtendingPrimingGeometryLayer(cacheLayer);

    // Execute the request
    primingLayer.fulfill(request);

    // Verify that the request received by the cache layer has the priming EngineGeometry set
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(cacheLayer).fulfill(requestCaptor.capture());
    Request capturedRequest = requestCaptor.getValue();

    assertTrue(capturedRequest.getPrimingGeometry().isPresent(),
        "Request should have priming EngineGeometry set");
  }

  @Test
  void testMultipleGeometriesWithDifferentFiles() {
    ExternalLayer chain = factory.createExtendingPrimingCogLayer();

    // Create requests for different areas and months
    EngineGeometry area1 = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);
    EngineGeometry area2 = createBoxGeometry(-98.0, 39.0, -97.0, 40.0);
    Request request1 = createFileRequest(COG_NOV_2021, area1);
    Request request2 = createFileRequest(COG_DEC_2021, area2);

    // Get results
    RealizedDistribution result1 = chain.fulfill(request1);
    RealizedDistribution result2 = chain.fulfill(request2);

    // Verify we got different data
    assertNotNull(result1);
    assertNotNull(result2);
    assertTrue(result1.getSize().isPresent() && result1.getSize().get() > 0);
    assertTrue(result2.getSize().isPresent() && result2.getSize().get() > 0);

    // Different months/areas should have different mean precipitation
    Optional<Scalar> mean1 = result1.getMean();
    Optional<Scalar> mean2 = result2.getMean();
    assertTrue(mean1.isPresent() && mean2.isPresent());
    assertNotEquals(0, mean1.get().getAsDecimal().compareTo(mean2.get().getAsDecimal()));
  }

  @Test
  void testGeometryHasCorrectCrs() {
    EngineGeometry testArea = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);

    // Verify the EngineGeometry has the correct CRS
    assertEquals(wgs84, testArea.getCrs(), "Geometry should have WGS84 CRS");
  }

  @Test
  void testNoPrimingGeometryFallsBackToDirectIo() {
    // Create test area and request with no priming geometry
    EngineGeometry testArea = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);
    Request request = createFileRequest(COG_NOV_2021, testArea);

    // Create mock layers to verify behavior
    CogExternalLayer cogLayer = mock(CogExternalLayer.class);
    RealizedDistribution mockResult = mock(RealizedDistribution.class);
    when(cogLayer.fulfill(any(Request.class))).thenReturn(mockResult);

    ExternalPathCacheLayer cacheLayer = new ExternalPathCacheLayer(cogLayer);

    // When the request has no priming geometry, it should fall back to the decorated layer
    cacheLayer.fulfill(request);

    // Verify cogLayer.fulfill was called directly
    verify(cogLayer).fulfill(request);
  }

  @Test
  void testCacheClearsWhenMemoryPressureIsHigh() {
    // This test verifies the LRUMap behavior in ExternalPathCacheLayer
    ExternalPathCacheLayer cacheLayer =
        new ExternalPathCacheLayer(new CogExternalLayer(units, caster));

    // Create many different geometries to fill the cache
    for (int i = 0; i < 200; i++) {
      EngineGeometry area = createBoxGeometry(-100.0 + (i * 0.1), 40.0, -99.0 + (i * 0.1), 41.0);
      Request request = createFileRequest(COG_NOV_2021, area);
      request.setPrimingGeometry(Optional.of(area)); // Set the area as its own priming geometry
      cacheLayer.fulfill(request);
    }

    // The cache should not grow indefinitely and should be capped by LRUMap
    assertTrue(cacheLayer.getCacheSize() < 200,
        "Cache should have evicted old entries, size: " + cacheLayer.getCacheSize());
  }
}
