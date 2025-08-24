package org.joshsim.geo.external.readers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.ExternalDataReader;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.precompute.JshdUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the {@link JshdExternalDataReaderFactory} class.
 */
@ExtendWith(MockitoExtension.class)
public class JshdExternalDataReaderFactoryTest {

  private JshdExternalDataReaderFactory factory;
  private EngineValueFactory valueFactory;

  @TempDir
  Path tempDir;

  /**
   * Sets up the test environment before each test.
   */
  @BeforeEach
  public void setUp() {
    valueFactory = new EngineValueFactory();
    factory = new JshdExternalDataReaderFactory(valueFactory);
  }

  @Test
  public void testGetValueFactory() {
    assertSame(valueFactory, factory.getValueFactory());
  }

  @Test
  public void testCreateReader() {
    ExternalDataReader reader = factory.createReader();
    
    assertNotNull(reader);
    assertTrue(reader instanceof JshdExternalDataReader);
  }

  @Test
  public void testCreateAndOpenWithValidFile() throws Exception {
    // Create a test JSHD file
    Path testJshdFile = createTestJshdFile();
    
    ExternalDataReader reader = factory.createAndOpen(testJshdFile.toString());
    
    assertNotNull(reader);
    assertTrue(reader instanceof JshdExternalDataReader);
    
    // Verify the file was opened successfully by checking that we can get variable names
    assertNotNull(reader.getVariableNames());
    
    reader.close();
  }

  @Test
  public void testCreateAndOpenWithInvalidFile() {
    Path nonExistentFile = tempDir.resolve("nonexistent.jshd");
    
    RuntimeException exception = assertThrows(RuntimeException.class, () -> 
        factory.createAndOpen(nonExistentFile.toString())
    );
    
    assertTrue(exception.getMessage().contains("Failed to open JSHD file"));
  }

  /**
   * Creates a test JSHD file for testing purposes.
   */
  private Path createTestJshdFile() throws Exception {
    // Create mock extents
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.valueOf(0));
    when(extents.getTopLeftY()).thenReturn(BigDecimal.valueOf(0));
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(1));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(1));

    // Create test data
    double[][][] innerValues = new double[1][2][2]; // 1 timestep, 2x2 grid
    innerValues[0][0][0] = 1.0;
    innerValues[0][0][1] = 2.0;
    innerValues[0][1][0] = 3.0;
    innerValues[0][1][1] = 4.0;

    // Create DoublePrecomputedGrid
    DoublePrecomputedGrid grid = new DoublePrecomputedGrid(
        valueFactory,
        extents,
        0, // minTimestep
        0, // maxTimestep
        Units.of("test"),
        innerValues
    );

    // Serialize to bytes
    byte[] jshdData = JshdUtil.serializeToBytes(grid);

    // Write to temp file
    Path jshdFile = tempDir.resolve("test_factory.jshd");
    Files.write(jshdFile, jshdData);

    return jshdFile;
  }
}