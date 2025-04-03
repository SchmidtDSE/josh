
package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PriorShadowingEntityDecoratorTest {

    @Mock private ShadowingEntity mockInner;
    @Mock private Geometry mockGeometry;
    @Mock private EntityType mockEntityType;
    @Mock private Entity mockFrozenEntity;
    @Mock private GeoKey mockGeoKey;
    @Mock private EngineValue mockEngineValue;

    private PriorShadowingEntityDecorator decorator;

    @BeforeEach
    void setUp() {
        when(mockInner.getName()).thenReturn("TestEntity");
        when(mockInner.getGeometry()).thenReturn(Optional.of(mockGeometry));
        when(mockInner.getEntityType()).thenReturn(mockEntityType);
        when(mockInner.freeze()).thenReturn(mockFrozenEntity);
        when(mockInner.getKey()).thenReturn(Optional.of(mockGeoKey));
        when(mockInner.getAttributeNames()).thenReturn(Arrays.asList("testAttr"));
        when(mockInner.getPriorAttribute("testAttr")).thenReturn(mockEngineValue);
        
        decorator = new PriorShadowingEntityDecorator(mockInner);
    }

    @Test
    void testGetGeometry() {
        Optional<Geometry> result = decorator.getGeometry();
        assertTrue(result.isPresent());
        assertEquals(mockGeometry, result.get());
    }

    @Test
    void testGetName() {
        assertEquals("TestEntity", decorator.getName());
    }

    @Test
    void testGetAttributeValue() {
        Optional<EngineValue> result = decorator.getAttributeValue("testAttr");
        assertTrue(result.isPresent());
        assertEquals(mockEngineValue, result.get());
    }

    @Test
    void testGetAttributeNames() {
        Iterable<String> names = decorator.getAttributeNames();
        assertEquals(Arrays.asList("testAttr"), names);
    }

    @Test
    void testGetEntityType() {
        assertEquals(mockEntityType, decorator.getEntityType());
    }

    @Test
    void testFreeze() {
        assertEquals(mockFrozenEntity, decorator.freeze());
    }

    @Test
    void testGetKey() {
        Optional<GeoKey> result = decorator.getKey();
        assertTrue(result.isPresent());
        assertEquals(mockGeoKey, result.get());
    }
}
