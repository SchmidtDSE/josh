
package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class JvmInputGetterTest {

  private static final String TEST_FILE_PATH = "/tmp/josh-jvm-example.txt";
  private static final String TEST_CONTENT = "test text";
  private File testFile;
  private JvmInputGetter inputGetter;

  @BeforeEach
  void setUp() throws Exception {
    testFile = new File(TEST_FILE_PATH);
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write(TEST_CONTENT);
    }
    inputGetter = new JvmInputGetter();
  }

  @AfterEach
  void tearDown() {
    if (testFile != null && testFile.exists()) {
      testFile.delete();
    }
  }

  @Test
  void testOpenFileFromWorkingDirectory() throws Exception {
    // Act
    InputStream inputStream = inputGetter.open(TEST_FILE_PATH);

    // Assert
    String result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(TEST_CONTENT, result);
  }
}
