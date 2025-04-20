/**
 * Tests for LocalOutputStreamStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

/**
 * Test a local file output stream.
 */
class LocalOutputStreamStrategyTest {

  @Test
  void testOpenMethodCreatesFileOutputStream() throws IOException {
    // Arrange
    String filePath = "/tmp/josh-open-test.txt";
    File file = new File(filePath);
    if (file.exists()) {
      assertTrue(file.delete());
    }
    LocalOutputStreamStrategy strategy = new LocalOutputStreamStrategy(filePath);

    // Act
    OutputStream outputStream = strategy.open();

    // Assert
    assertNotNull(outputStream);
    assertTrue(file.exists());
    outputStream.close();
    assertTrue(file.delete());
  }

}
