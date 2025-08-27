/**
 * Tests for the JvmInputOutputLayerBuilder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;
import org.joshsim.pipeline.job.config.TemplateStringRenderer;
import org.junit.jupiter.api.Test;

/**
 * Tests for JvmInputOutputLayerBuilder.
 */
public class JvmInputOutputLayerBuilderTest {

  @Test
  public void testDefaultBuilder() {
    JvmInputOutputLayer layer = new JvmInputOutputLayerBuilder().build();

    assertNotNull(layer);
    assertNotNull(layer.getExportFacadeFactory());
    assertNotNull(layer.getInputStrategy());
  }

  @Test
  public void testBuilderWithReplicate() {
    JvmInputOutputLayer layer = new JvmInputOutputLayerBuilder()
        .withReplicate(5)
        .build();

    assertNotNull(layer);
    assertNotNull(layer.getExportFacadeFactory());
    assertNotNull(layer.getInputStrategy());
  }

  @Test
  public void testBuilderWithEarthSpace() {
    PatchBuilderExtents extents = new PatchBuilderExtents(
        new BigDecimal("-116.0"), // topLeftX (longitude)
        new BigDecimal("34.0"),   // topLeftY (latitude)
        new BigDecimal("-115.0"), // bottomRightX (longitude)
        new BigDecimal("33.0")    // bottomRightY (latitude)
    );
    BigDecimal width = new BigDecimal("100.0");

    JvmInputOutputLayer layer = new JvmInputOutputLayerBuilder()
        .withReplicate(2)
        .withEarthSpace(extents, width)
        .build();

    assertNotNull(layer);
    assertNotNull(layer.getExportFacadeFactory());
    assertNotNull(layer.getInputStrategy());
  }

  @Test
  public void testBuilderWithCustomInputStrategy() {
    InputGetterStrategy mockStrategy = mock(InputGetterStrategy.class);

    JvmInputOutputLayer layer = new JvmInputOutputLayerBuilder()
        .withReplicate(3)
        .withInputStrategy(mockStrategy)
        .build();

    assertNotNull(layer);
    assertNotNull(layer.getExportFacadeFactory());
    assertEquals(mockStrategy, layer.getInputStrategy());
  }

  @Test
  public void testBuilderChaining() {
    PatchBuilderExtents extents = new PatchBuilderExtents(
        new BigDecimal("-116.0"), // topLeftX (longitude)
        new BigDecimal("34.0"),   // topLeftY (latitude)
        new BigDecimal("-115.0"), // bottomRightX (longitude)
        new BigDecimal("33.0")    // bottomRightY (latitude)
    );
    BigDecimal width = new BigDecimal("50.0");
    InputGetterStrategy mockStrategy = mock(InputGetterStrategy.class);

    JvmInputOutputLayer layer = new JvmInputOutputLayerBuilder()
        .withReplicate(1)
        .withEarthSpace(extents, width)
        .withInputStrategy(mockStrategy)
        .build();

    assertNotNull(layer);
    assertNotNull(layer.getExportFacadeFactory());
    assertEquals(mockStrategy, layer.getInputStrategy());
  }

  @Test
  public void testBuilderWithTemplateRenderer() {
    JoshJob job = new JoshJobBuilder()
        .setFileInfo("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"))
        .setReplicates(1)
        .build();
    TemplateStringRenderer templateRenderer = new TemplateStringRenderer(job, 0);

    JvmInputOutputLayer layer = new JvmInputOutputLayerBuilder()
        .withReplicate(1)
        .withTemplateRenderer(templateRenderer)
        .build();

    assertNotNull(layer);
    assertNotNull(layer.getExportFacadeFactory());
    assertNotNull(layer.getInputStrategy());
  }

  @Test
  public void testBuilderChainWithTemplateRenderer() {
    PatchBuilderExtents extents = new PatchBuilderExtents(
        new BigDecimal("-116.0"), // topLeftX (longitude)
        new BigDecimal("34.0"),   // topLeftY (latitude)
        new BigDecimal("-115.0"), // bottomRightX (longitude)
        new BigDecimal("33.0")    // bottomRightY (latitude)
    );
    BigDecimal width = new BigDecimal("75.0");
    InputGetterStrategy mockStrategy = mock(InputGetterStrategy.class);

    JoshJob job = new JoshJobBuilder()
        .setFileInfo("config.jshc", new JoshJobFileInfo("config_v1", "data/config_v1.jshc"))
        .setFileInfo("weather.jshd", new JoshJobFileInfo("weather_2023", "data/weather_2023.jshd"))
        .setReplicates(1)
        .build();
    TemplateStringRenderer templateRenderer = new TemplateStringRenderer(job, 2);

    JvmInputOutputLayer layer = new JvmInputOutputLayerBuilder()
        .withReplicate(2)
        .withEarthSpace(extents, width)
        .withInputStrategy(mockStrategy)
        .withTemplateRenderer(templateRenderer)
        .build();

    assertNotNull(layer);
    assertNotNull(layer.getExportFacadeFactory());
    assertEquals(mockStrategy, layer.getInputStrategy());
  }

  @Test
  public void testBuilderWithNullTemplateRenderer() {
    JvmInputOutputLayer layer = new JvmInputOutputLayerBuilder()
        .withReplicate(4)
        .withTemplateRenderer(null)
        .build();

    assertNotNull(layer);
    assertNotNull(layer.getExportFacadeFactory());
    assertNotNull(layer.getInputStrategy());
  }
}
