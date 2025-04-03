package org.joshsim.engine.external.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.joshsim.engine.external.cog.CogExternalLayer;
import org.joshsim.engine.external.cog.CogReader;
import org.joshsim.engine.external.core.Request;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.joshsim.engine.value.type.Scalar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;

/**
 * Unit tests for the ExternalLayerFactory class, ensuring that it correctly
 * creates and manages external layers for processing geospatial data.
 */
public class ExternalLayerFactoryTest {
  private EngineValueWideningCaster caster;
  private Units units;
  private ExternalLayerFactory factory;
  private SpatialContext spatialContext;
  private static final String COG_NOV_2021 = "assets/test/cog/nclimgrid-prcp-202111.tif";
  private static final String COG_DEC_2021 = "assets/test/cog/nclimgrid-prcp-202112.tif";
  
  @BeforeEach
  void setup() {
    caster = new EngineValueWideningCaster();
    units = new Units("mm");
    factory = new ExternalLayerFactory(caster, units);
    spatialContext = SpatialContext.GEO;
  }
  
  private Geometry createBoxGeometry(double minX, double minY, double maxX, double maxY) {
    return new Geometry(spatialContext.getShapeFactory().rect(minX, maxX, minY, maxY));
  }

  @Test
  void testChainReadsCogFiles() {
    // Create a geometry in the US where the test data has coverage
    Geometry testArea = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);
    Request request = new Request(COG_NOV_2021, Optional.of(testArea));
    
    // Test the entire chain
    ExternalLayer chain = factory.createCogExternalLayerChain();
    RealizedDistribution result = chain.fulfill(request);
    
    // Verify results
    assertNotNull(result);
    assertTrue(result.getSize().isPresent());
    assertTrue(result.getSize().get() > 0);
  }

  @Test
  void testCachingLayer() {
    Geometry testArea = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);
    Request request = new Request(COG_NOV_2021, Optional.of(testArea));
    ExternalLayer chain = factory.createCogExternalLayerChain();
    
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
  void testIndividualLayers() {
    Geometry testArea = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);
    Request request = new Request(COG_NOV_2021, Optional.of(testArea));
    
    // Test CogExternalLayer directly
    CogReader cogReader = new CogReader(caster, units);
    CogExternalLayer cogLayer = new CogExternalLayer(cogReader);
    RealizedDistribution cogResult = cogLayer.fulfill(request);
    assertNotNull(cogResult);
    assertTrue(cogResult.getSize().isPresent() && cogResult.getSize().get() > 0);
    
    // Test ExternalPathCacheLayer
    ExternalPathCacheLayer cacheLayer = new ExternalPathCacheLayer(cogLayer);
    assertEquals(0, cacheLayer.getCacheSize());
    
    RealizedDistribution cacheResult1 = cacheLayer.fulfill(request);
    assertEquals(1, cacheLayer.getCacheSize());
    
    RealizedDistribution cacheResult2 = cacheLayer.fulfill(request);
    assertEquals(1, cacheLayer.getCacheSize());
    assertEquals(cacheResult1.getSize(), cacheResult2.getSize());
    
    // Test cache clearing
    cacheLayer.clearCache();
    assertEquals(0, cacheLayer.getCacheSize());
    
    // Test PrimingGeometryLayer
    PrimingGeometryLayer primingLayer = new PrimingGeometryLayer(cacheLayer);
    assertFalse(primingLayer.getPrimingGeometry().isPresent());
    
    primingLayer.fulfill(request);
    assertTrue(primingLayer.getPrimingGeometry().isPresent());
    
    // Test with a second geometry
    Geometry testArea2 = createBoxGeometry(-98.0, 39.0, -97.0, 40.0);
    Request request2 = new Request(COG_DEC_2021, Optional.of(testArea2));
    primingLayer.fulfill(request2);
    assertTrue(primingLayer.getPrimingGeometry().isPresent());
  }
  
  @Test
  void testMultipleGeometries() {
    ExternalLayer chain = factory.createCogExternalLayerChain();
    
    // Create requests for different areas and months
    Geometry area1 = createBoxGeometry(-100.0, 40.0, -99.0, 41.0);
    Geometry area2 = createBoxGeometry(-98.0, 39.0, -97.0, 40.0);
    Request request1 = new Request(COG_NOV_2021, Optional.of(area1));
    Request request2 = new Request(COG_DEC_2021, Optional.of(area2));
    
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
}