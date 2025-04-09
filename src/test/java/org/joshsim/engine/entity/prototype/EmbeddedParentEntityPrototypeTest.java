package org.joshsim.engine.entity.prototype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.entity.base.MutableEntity;
import org.junit.jupiter.api.Test;


class EmbeddedParentEntityPrototypeTest {

  @Test
  void testBuildWhenInnerPrototypeRequiresParent() {
    EntityPrototype innerPrototype = mock(EntityPrototype.class);
    Entity parentEntity = mock(Entity.class);
    EmbeddedParentEntityPrototype prototype = new EmbeddedParentEntityPrototype(
        innerPrototype,
        parentEntity
    );
    MutableEntity expectedEntity = mock(MutableEntity.class);

    when(innerPrototype.requiresParent()).thenReturn(true);
    when(innerPrototype.buildSpatial(parentEntity)).thenReturn(expectedEntity);

    Entity result = prototype.build();

    assertEquals(expectedEntity, result);
    verify(innerPrototype).buildSpatial(parentEntity);
    verify(innerPrototype, never()).build();
    verify(innerPrototype, never()).buildSpatial(any(EngineGeometry.class));
  }

  @Test
  void testBuildWhenInnerPrototypeRequiresGeometry() {
    EntityPrototype innerPrototype = mock(EntityPrototype.class);
    EngineGeometry parentGeometry = mock(EngineGeometry.class);
    Entity parentEntity = mock(Entity.class);
    when(parentEntity.getGeometry()).thenReturn(Optional.of(parentGeometry));

    EmbeddedParentEntityPrototype prototype = new EmbeddedParentEntityPrototype(
        innerPrototype,
        parentEntity
    );
    MutableEntity expectedEntity = mock(MutableEntity.class);

    when(innerPrototype.requiresParent()).thenReturn(false);
    when(innerPrototype.requiresGeometry()).thenReturn(true);
    when(innerPrototype.buildSpatial(parentGeometry)).thenReturn(expectedEntity);

    Entity result = prototype.build();

    assertEquals(expectedEntity, result);
    verify(innerPrototype).buildSpatial(parentGeometry);
    verify(innerPrototype, never()).build();
    verify(innerPrototype, never()).buildSpatial(parentEntity);
  }

  @Test
  void testBuildWhenInnerPrototypeDoesNotRequireParentOrGeometry() {
    EntityPrototype innerPrototype = mock(EntityPrototype.class);
    Entity parentEntity = mock(Entity.class);
    EmbeddedParentEntityPrototype prototype = new EmbeddedParentEntityPrototype(
        innerPrototype,
        parentEntity
    );
    MutableEntity expectedEntity = mock(MutableEntity.class);

    when(innerPrototype.requiresParent()).thenReturn(false);
    when(innerPrototype.requiresGeometry()).thenReturn(false);
    when(innerPrototype.build()).thenReturn(expectedEntity);

    Entity result = prototype.build();

    assertEquals(expectedEntity, result);
    verify(innerPrototype).build();
    verify(innerPrototype, never()).buildSpatial(any(Entity.class));
    verify(innerPrototype, never()).buildSpatial(any(EngineGeometry.class));
  }

  @Test
  void testBuildWhenInnerPrototypeRequiresGeometryButParentHasNoGeometry() {
    EntityPrototype innerPrototype = mock(EntityPrototype.class);
    Entity parentEntity = mock(Entity.class);
    when(parentEntity.getGeometry()).thenReturn(Optional.empty());

    EmbeddedParentEntityPrototype prototype = new EmbeddedParentEntityPrototype(
        innerPrototype,
        parentEntity
    );

    when(innerPrototype.requiresParent()).thenReturn(false);
    when(innerPrototype.requiresGeometry()).thenReturn(true);

    assertThrows(NoSuchElementException.class, prototype::build);
    verify(innerPrototype, never()).build();
    verify(innerPrototype, never()).buildSpatial(any(Entity.class));
    verify(innerPrototype, never()).buildSpatial(any(EngineGeometry.class));
  }
}
