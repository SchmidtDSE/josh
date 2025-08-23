package org.joshsim.geo.external;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.readers.JshdExternalDataReader;
import org.joshsim.precompute.DoublePrecomputedGrid;
import org.joshsim.precompute.JshdUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests for the ExternalDataReaderFactory with JSHD files.
 */
@ExtendWith(MockitoExtension.class)
public class ExternalDataReaderFactoryJshdTest {

  private EngineValueFactory valueFactory;

  @TempDir
  Path tempDir;

  /**
   * Sets up the test environment before each test.
   */
  @BeforeEach
  public void setUp() {
    valueFactory = new EngineValueFactory();
  }

  @Test
  public void testCreateReaderForJshdFile() throws Exception {
    // Create a test JSHD file
    Path testJshdFile = createTestJshdFile();
    
    ExternalDataReader reader = ExternalDataReaderFactory.createReader(
        valueFactory, testJshdFile.toString());
    
    assertNotNull(reader);
    assertTrue(reader instanceof JshdExternalDataReader);
    
    reader.close();
  }

  @Test
  public void testCreateReaderForJshdFileCaseInsensitive() throws Exception {
    // Create a test JSHD file with uppercase extension
    Path testJshdFile = createTestJshdFile("TEST.JSHD");
    
    ExternalDataReader reader = ExternalDataReaderFactory.createReader(
        valueFactory, testJshdFile.toString());
    
    assertNotNull(reader);
    assertTrue(reader instanceof JshdExternalDataReader);
    
    reader.close();
  }

  /**
   * Creates a test JSHD file for testing purposes.
   */
  private Path createTestJshdFile() throws Exception {
    return createTestJshdFile("test.jshd");
  }

  /**
   * Creates a test JSHD file with the specified filename for testing purposes.
   */
  private Path createTestJshdFile(String filename) throws Exception {
    // Create mock extents
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.valueOf(0));
    when(extents.getTopLeftY()).thenReturn(BigDecimal.valueOf(0));
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(1));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(1));

    // Create test data
    double[][][] innerValues = new double[1][2][2]; // 1 timestep, 2x2 grid
    innerValues[0][0][0] = 10.5;
    innerValues[0][0][1] = 20.5;
    innerValues[0][1][0] = 30.5;
    innerValues[0][1][1] = 40.5;

    // Create DoublePrecomputedGrid
    DoublePrecomputedGrid grid = new DoublePrecomputedGrid(
        valueFactory,
        extents,
        0, // minTimestep
        0, // maxTimestep
        Units.of("integration_test"),
        innerValues
    );

    // Serialize to bytes
    byte[] jshdData = JshdUtil.serializeToBytes(grid);

    // Write to temp file
    Path jshdFile = tempDir.resolve(filename);
    Files.write(jshdFile, jshdData);

    return jshdFile;
  }
}