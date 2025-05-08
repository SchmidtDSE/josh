
package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;

class PatchKeyConverterTest {
    
    private PatchKeyConverter converter;
    private static final BigDecimal PATCH_WIDTH = new BigDecimal("5000"); // 5km patches

    @BeforeEach
    void setUp() {
        // Using LA and SF coordinates from HaversineUtilTest
        BigDecimal laLong = new BigDecimal("-118.24");
        BigDecimal laLat = new BigDecimal("34.05");
        BigDecimal sfLong = new BigDecimal("-122.45");
        BigDecimal sfLat = new BigDecimal("37.73");
        
        PatchBuilderExtents extents = new PatchBuilderExtents(
            sfLong,  // topLeftX (west)
            sfLat,   // topLeftY (north)
            laLong,  // bottomRightX (east)
            laLat    // bottomRightY (south)
        );
        
        converter = new PatchKeyConverter(extents, PATCH_WIDTH);
    }

    @Test
    void convertShouldCalculateCorrectGridPosition() {
        // Create a GeoKey at LA's position
        GeoKey laKey = new GeoKey(new Entity() {
            @Override
            public Optional<EngineGeometry> getGeometry() {
                return Optional.of(new EngineGeometry() {
                    @Override
                    public BigDecimal getCenterX() {
                        return new BigDecimal("-118.24");
                    }
                    
                    @Override
                    public BigDecimal getCenterY() {
                        return new BigDecimal("34.05");
                    }
                    
                    @Override
                    public BigDecimal getOnGrid() {
                        return BigDecimal.ONE;
                    }
                });
            }
            
            @Override
            public String getName() {
                return "TestPatch";
            }
        });

        ProjectedKey result = converter.convert(laKey);
        
        // LA is approximately 557.787km from SF (from HaversineUtilTest)
        // With 5km patches, expect around 111 patches distance
        BigDecimal expectedX = new BigDecimal("111");
        BigDecimal expectedY = new BigDecimal("111");
        
        assertEquals(0, result.getX().compareTo(expectedX),
            "X coordinate should match expected grid position");
        assertEquals(0, result.getY().compareTo(expectedY),
            "Y coordinate should match expected grid position");
    }
}
