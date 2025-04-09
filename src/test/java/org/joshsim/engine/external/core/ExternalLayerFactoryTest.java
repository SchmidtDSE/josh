package org.joshsim.engine.external.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.joshsim.engine.external.cog.CogExternalLayer;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.joshsim.engine.value.type.Scalar;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the ExternalLayerFactory class, ensuring that it correctly
 * creates and manages external layers for processing geospatial data with caching.
 */
public class ExternalLayerFactoryTest {
  private EngineValueCaster caster;
  private Units units;
  private ExternalLayerFactory factory;
  private CoordinateReferenceSystem wgs84;
  private CoordinateReferenceSystem utm11n;
  private static final String COG_NOV_2021 = "assets/test/cog/nclimgrid-prcp-202111.tif";
  private static final String COG_DEC_2021 = "assets/test/cog/nclimgrid-prcp-202112.tif";
  
  // Valid coordinates for UTM Zone 11N (approximately -120° to -114° longitude)
  private double[][] validUtm11nCoordinates;
  private double[] defaultValidCoordinate;
  
  // Test areas within UTM Zone 11N
  private EngineGeometry testArea1;
  private EngineGeometry testArea2;
  private EngineGeometry testAreaSmall;

  @BeforeAll
  static void setHeadlessMode() {
    // Enable headless mode to avoid X11 dependencies
    System.setProperty("java.awt.headless", "true");

    // Disable native acceleration which can cause issues
    System.setProperty("javax.media.jai.disableMediaLib", "true");
  }

  @BeforeEach
  void setUp() throws FactoryException {
    caster = new EngineValueWideningCaster();
    units = new Units("mm");
    factory = new ExternalLayerFactory(caster, units);
    
    wgs84 = CRS.decode("EPSG:4326", true); // WGS84, lefthanded (lon first)
    utm11n = CRS.decode("EPSG:32611"); // UTM Zone 11N

    // Initialize valid coordinates for UTM Zone 11N (approximately -120° to -114° longitude)
    validUtm11nCoordinates = new double[][] {
        {-117.0, 34.0},   // Southern California
        {-118.2, 34.0},   // Los Angeles area
        {-116.5, 33.8},   // Palm Springs area
        {-119.8, 36.7},   // Central California
        {-115.0, 35.0}    // Mojave Desert area
    };
    
    // Set up a default valid coordinate for simple tests
    defaultValidCoordinate = validUtm11nCoordinates[0];
    
    // Create test areas for our tests
    double lonWidth1 = 0.5;
    double latHeight1 = 0.5;
    testArea1 = createBoxGeometry(
        defaultValidCoordinate[0] - lonWidth1, 
        defaultValidCoordinate[1] - latHeight1, 
        defaultValidCoordinate[0] + lonWidth1, 
        defaultValidCoordinate[1] + latHeight1
    );
    
    // Second test area that overlaps with the first
    testArea2 = createBoxGeometry(
        defaultValidCoordinate[0] - 0.25, 
        defaultValidCoordinate[1], 
        defaultValidCoordinate[0] + 0.75, 
        defaultValidCoordinate[1] + 1.0
    );
    
    // Small test area for more precise tests
    testAreaSmall = createBoxGeometry(
        defaultValidCoordinate[0] - 0.1, 
        defaultValidCoordinate[1] - 0.1, 
        defaultValidCoordinate[0] + 0.1, 
        defaultValidCoordinate[1] + 0.1
    );
  }

  private EngineGeometry createBoxGeometry(double minX, double minY, double maxX, double maxY) {
    return EngineGeometryFactory.createSquare(
        BigDecimal.valueOf(minX),
        BigDecimal.valueOf(maxY),  // topLeftY
        BigDecimal.valueOf(maxX),
        BigDecimal.valueOf(minY),  // bottomRightY
        wgs84
    );
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
    Request request = createFileRequest(COG_NOV_2021, testArea1);

    // Test the entire chain
    ExternalLayer chain = factory.createExtendingPrimingCogLayer();
    RealizedDistribution result = chain.fulfill(request);

    // Verify results
    assertNotNull(result);
    assertTrue(result.getSize().isPresent());
    assertTrue(result.getSize().get() > 0);
  }

  @Test
  void testCachingBehavior() {
    Request request = createFileRequest(COG_NOV_2021, testAreaSmall);
    
    // Create a spy on the real CogExternalLayer
    CogExternalLayer cogLayer = spy(new CogExternalLayer(units, caster));
    
    // Create the cache layer with our spy
    ExternalPathCacheLayer cacheLayer = new ExternalPathCacheLayer(cogLayer);
    
    // Create the full chain
    ExtendingPrimingGeometryLayer chain = new ExtendingPrimingGeometryLayer(cacheLayer);
    
    // First request
    final RealizedDistribution result1 = chain.fulfill(request);

    // Check cache state
    assertEquals(1, cacheLayer.getCacheSize(), "Cache should contain one entry");
    
    // Second request with the same parameters
    RealizedDistribution result2 = chain.fulfill(request);

    // Check cache state is still one
    assertEquals(1, cacheLayer.getCacheSize(), "Cache should contain one entry");
    
    // Verify results match
    assertEquals(result1.getSize(), result2.getSize());
    
    // Verify statistics match
    Optional<Scalar> mean1 = result1.getMean();
    Optional<Scalar> mean2 = result2.getMean();
    assertTrue(mean1.isPresent() && mean2.isPresent());
    assertEquals(0, mean1.get().getAsDecimal().compareTo(mean2.get().getAsDecimal()));
  }

  @Test
  void testRequestWithoutPrimingGeometryReachesBaseCogLayer() {
    // Create a spy on the CogExternalLayer to track method calls
    CogExternalLayer cogLayerSpy = spy(new CogExternalLayer(units, caster));
    
    // Create the chain with our spy
    ExternalPathCacheLayer cacheLayer = new ExternalPathCacheLayer(cogLayerSpy);
    ExtendingPrimingGeometryLayer primingLayer = new ExtendingPrimingGeometryLayer(cacheLayer);
    
    // Create a request WITHOUT setting a priming geometry
    Request request = createFileRequest(COG_NOV_2021, testAreaSmall);
    
    // Ensure priming geometry isn't set by the request creation
    assertEquals(Optional.empty(), request.getPrimingGeometry());
    
    // Execute request through the chain
    primingLayer.fulfill(request);
    
    // Verify CogExternalLayer.fulfill was called with this request
    // The request should pass through both decorators to the base layer
    verify(cogLayerSpy, times(1)).fulfill(any(Request.class));
    
    // After processing, the request should have a priming geometry set
    // (set by the ExtendingPrimingGeometryLayer)
    assertTrue(request.getPrimingGeometry().isPresent());
  }

  @Test
  void testIndividualLayers() throws IOException {
    Request request = createFileRequest(COG_NOV_2021, testAreaSmall);

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
    Geometry geometry = primingGeom.getInnerGeometry();
    Polygon polygon = (Polygon) geometry;

    // Get the bounding box of the geometry and verify it matches our test area
    double minX = defaultValidCoordinate[0] - 0.1;
    double maxX = defaultValidCoordinate[0] + 0.1;
    double minY = defaultValidCoordinate[1] - 0.1;
    double maxY = defaultValidCoordinate[1] + 0.1;
    
    assertEquals(minX, polygon.getEnvelopeInternal().getMinX(), 0.000001);
    assertEquals(maxX, polygon.getEnvelopeInternal().getMaxX(), 0.000001);
    assertEquals(minY, polygon.getEnvelopeInternal().getMinY(), 0.000001);
    assertEquals(maxY, polygon.getEnvelopeInternal().getMaxY(), 0.000001);
  }

  @Test
  void testExtendingPrimingGeometry() {
    Request request1 = createFileRequest(COG_NOV_2021, testArea1);
    Request request2 = createFileRequest(COG_NOV_2021, testArea2);

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
    assertTrue(extendedPrimingGeom.getInnerGeometry().contains(testArea1.getInnerGeometry()),
        "Extended geometry should contain the first area");
    assertTrue(extendedPrimingGeom.getInnerGeometry().contains(testArea2.getInnerGeometry()),
        "Extended geometry should contain the second area");

    // Calculate expected convex hull manually and verify it matches
    EngineGeometry expectedConvexHull = testArea1.getConvexHull(testArea2);
    assertTrue(expectedConvexHull.getInnerGeometry().equals(extendedPrimingGeom.getInnerGeometry()),
        "Extended geometry should equal the convex hull of both areas");

    // Verify envelope contains both areas
    ReferencedEnvelope envelope = extendedPrimingGeom.getEnvelope();
    assertTrue(envelope.contains(testArea1.getInnerGeometry().getEnvelopeInternal()),
        "Envelope should contain the first area");
    assertTrue(envelope.contains(testArea2.getInnerGeometry().getEnvelopeInternal()),
        "Envelope should contain the second area");
  }

  @Test
  void testPrimingGeometryPropagation() {
    Request request = createFileRequest(COG_NOV_2021, testAreaSmall);

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
    Request request1 = createFileRequest(COG_NOV_2021, testArea1);
    Request request2 = createFileRequest(COG_DEC_2021, testArea2);

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
    // Verify the EngineGeometry has the correct CRS
    assertEquals(wgs84, testArea1.getCrs(), "Geometry should have WGS84 CRS");
  }

  @Test
  void testNoPrimingGeometryFallsBackToDirectIo() {
    Request request = createFileRequest(COG_NOV_2021, testAreaSmall);

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
      double offsetLon = validUtm11nCoordinates[0][0] + (i * 0.01);
      double offsetLat = validUtm11nCoordinates[0][1];
      EngineGeometry area = createBoxGeometry(
          offsetLon - 0.05, offsetLat - 0.05,
          offsetLon + 0.05, offsetLat + 0.05);
          
      Request request = createFileRequest(COG_NOV_2021, area);
      request.setPrimingGeometry(Optional.of(area)); // Set the area as its own priming geometry
      cacheLayer.fulfill(request);
    }

    // The cache should not grow indefinitely and should be capped by LRUMap
    assertTrue(cacheLayer.getCacheSize() < 200,
        "Cache should have evicted old entries, size: " + cacheLayer.getCacheSize());
  }
}