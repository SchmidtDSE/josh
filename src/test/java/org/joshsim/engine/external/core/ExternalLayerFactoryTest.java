package org.joshsim.engine.external.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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

  private Request createUrlRequest(String url, EngineGeometry geometry) {
    return new Request("url", "", url, Optional.of(geometry), Optional.empty());
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

    // Once more, to ensure cache is used

    RealizedDistribution result3 = chain.fulfill(request);

    // Check cache state is still one
    assertEquals(1, cacheLayer.getCacheSize(), "Cache should contain one entry");

    // Verify results match
    assertEquals(result1.getSize(), result2.getSize());
    assertEquals(result2.getSize(), result3.getSize());

    // Verify statistics match
    Optional<Scalar> mean1 = result1.getMean();
    Optional<Scalar> mean2 = result2.getMean();
    assertTrue(mean1.isPresent() && mean2.isPresent());
    assertEquals(0, mean1.get().getAsDecimal().compareTo(mean2.get().getAsDecimal()));
  }

  @Test
  void testDirectCallCogExternalLayer() throws IOException {
    Request request = createFileRequest(COG_NOV_2021, testAreaSmall);

    // Test CogExternalLayer
    CogExternalLayer cogLayer = spy(new CogExternalLayer(units, caster));
    RealizedDistribution cogResult = cogLayer.fulfill(request);
    assertNotNull(cogResult);
    assertTrue(cogResult.getSize().isPresent() && cogResult.getSize().get() > 0);
  }

  @Test
  void testCacheRespectsPrimingGeometryExistence() throws IOException {
    Request request = createFileRequest(COG_NOV_2021, testAreaSmall);
    CogExternalLayer cogLayer = spy(new CogExternalLayer(units, caster));

    // Test ExternalPathCacheLayer
    // Without an PrimingCacheLayer OR the Request having a priming geometry explicitly,
    // the cache layer should not be used and the base CogExternalLayer should be called
    // to fulfill a reqeuest.
    ExternalPathCacheLayer cacheLayer = new ExternalPathCacheLayer(cogLayer);
    assertEquals(0, cacheLayer.getCacheSize());

    final RealizedDistribution cacheResult1 = cacheLayer.fulfill(request);
    assertEquals(0, cacheLayer.getCacheSize());
    verify(cogLayer, times(1)).fulfill(request);

    // Second request should still not use cache
    RealizedDistribution cacheResult2 = cacheLayer.fulfill(request);
    assertEquals(0, cacheLayer.getCacheSize());
    verify(cogLayer, times(2)).fulfill(request);

    // Now, set an explicit priming geometry
    request.setPrimingGeometry(Optional.of(testAreaSmall));

    // Third request should use populate the cache, and should not reach base cog layer
    RealizedDistribution cacheResult3 = cacheLayer.fulfill(request);
    assertNotNull(cacheResult3);
    assertEquals(1, cacheLayer.getCacheSize());
    verify(cogLayer, times(2)).fulfill(request);

    // Fourth request should use the cache, not add a new entry, and not reach base cog layer
    RealizedDistribution cacheResult4 = cacheLayer.fulfill(request);
    assertNotNull(cacheResult4);
    assertEquals(1, cacheLayer.getCacheSize());
    verify(cogLayer, times(2)).fulfill(request);

    // Test cache clearing
    cacheLayer.clearCache();
    assertEquals(0, cacheLayer.getCacheSize());
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

  @Test
  @Tag("remote")
  void testReadCogFromUrl() {
    // Use the remote URL version of the same COG file we use locally
    String cogUrl = "https://storage.googleapis.com/national_park_service/nclimgrid-prcp-202111.tif";

    // Create a request with our test area - use the same test area as in local tests
    Request request = createUrlRequest(cogUrl, testArea1);

    // Create layer chain
    ExternalLayer chain = factory.createExtendingPrimingCogLayer();

    // Fulfill request
    RealizedDistribution result = chain.fulfill(request);

    // Verify result
    assertNotNull(result, "Result should not be null");
    assertTrue(result.getSize().isPresent(), "Result should have size");
    assertTrue(result.getSize().get() > 0, "Result should have values");
  }

  @Test
  @Tag("remote")
  void testCachingWithUrlBasedCogs() {
    // Use the remote URL version of the same COG file we use locally
    String cogUrl = "https://storage.googleapis.com/national_park_service/nclimgrid-prcp-202111.tif";

    // Use the same test area as in local tests for consistency
    Request request = createUrlRequest(cogUrl, testAreaSmall);

    // Create a layer chain for testing
    CogExternalLayer cogLayer = spy(new CogExternalLayer(units, caster));
    ExternalPathCacheLayer cacheLayer = new ExternalPathCacheLayer(cogLayer);
    ExtendingPrimingGeometryLayer chain = new ExtendingPrimingGeometryLayer(cacheLayer);

    // First request - this should populate the priming geometry
    chain.fulfill(request);

    // Second request with the same parameters
    chain.fulfill(request);

    // Cache should have one entry
    assertEquals(1, cacheLayer.getCacheSize(), "Cache should contain one entry");
  }

  @Test
  @Tag("remote")
  void testMultipleUrlCogFiles() {
    // Use the remote URL versions of the same COG files we use locally
    String cogUrl1 = "https://storage.googleapis.com/national_park_service/nclimgrid-prcp-202111.tif";
    String cogUrl2 = "https://storage.googleapis.com/national_park_service/nclimgrid-prcp-202112.tif";

    // Create requests for different areas and months - match the local tests
    Request request1 = createUrlRequest(cogUrl1, testArea1);
    Request request2 = createUrlRequest(cogUrl2, testArea2);

    // Create layer chain
    ExternalLayer chain = factory.createExtendingPrimingCogLayer();

    // Process both requests
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
  void testUrlVersusFileResults() {
    // Create requests for the same area using both file and URL approaches
    Request fileRequest = createFileRequest(COG_NOV_2021, testAreaSmall);
    Request urlRequest = createUrlRequest(
        "https://storage.googleapis.com/national_park_service/nclimgrid-prcp-202111.tif",
        testAreaSmall);

    // Create separate chains to avoid caching effects
    ExternalLayer fileChain = factory.createExtendingPrimingCogLayer();
    ExternalLayer urlChain = factory.createExtendingPrimingCogLayer();

    // Get results from both sources
    RealizedDistribution fileResult = fileChain.fulfill(fileRequest);
    RealizedDistribution urlResult = urlChain.fulfill(urlRequest);

    // Verify both results are valid
    assertNotNull(fileResult);
    assertNotNull(urlResult);
    assertTrue(fileResult.getSize().isPresent() && fileResult.getSize().get() > 0);
    assertTrue(urlResult.getSize().isPresent() && urlResult.getSize().get() > 0);

    // The mean value should be identical since it's the same data
    Optional<Scalar> fileMean = fileResult.getMean();
    Optional<Scalar> urlMean = urlResult.getMean();
    assertTrue(fileMean.isPresent() && urlMean.isPresent());
    assertEquals(0, fileMean.get().getAsDecimal().compareTo(urlMean.get().getAsDecimal()),
        "Mean values should be identical for the same data source whether via file or URL");
  }
}
