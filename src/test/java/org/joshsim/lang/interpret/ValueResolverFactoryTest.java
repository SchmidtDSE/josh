/**
 * Tests for ValueResolverFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.joshsim.engine.value.engine.EngineValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Tests for ValueResolverFactory and RecursiveValueResolverFactory.
 */
@ExtendWith(MockitoExtension.class)
public class ValueResolverFactoryTest {

  private EngineValueFactory valueFactory;
  private ValueResolverFactory factory;

  /**
   * Set up shared instances before each test.
   */
  @BeforeEach
  void setUp() {
    valueFactory = new EngineValueFactory();
    factory = new RecursiveValueResolverFactory();
  }

  @Test
  void testBuildReturnsValueResolver() {
    ValueResolver resolver = factory.build(valueFactory, "someAttr");
    assertNotNull(resolver, "build should return a non-null ValueResolver");
    assertInstanceOf(ValueResolver.class, resolver, "result should implement ValueResolver");
  }

  @Test
  void testBuildReturnsRecursiveValueResolver() {
    ValueResolver resolver = factory.build(valueFactory, "someAttr");
    assertInstanceOf(
        RecursiveValueResolver.class,
        resolver,
        "result should be a RecursiveValueResolver"
    );
  }

  @Test
  void testBuiltResolverHasCorrectPath() {
    ValueResolver resolver = factory.build(valueFactory, "entity.height");
    assertEquals(
        "entity.height",
        resolver.getPath(),
        "resolver should have the path it was built with"
    );
  }

  @Test
  void testBuildWithDifferentPathsReturnsDifferentResolvers() {
    ValueResolver resolverA = factory.build(valueFactory, "entity.height");
    ValueResolver resolverB = factory.build(valueFactory, "entity.age");
    assertNotSame(
        resolverA,
        resolverB,
        "different paths should produce distinct resolver instances"
    );
  }

}
