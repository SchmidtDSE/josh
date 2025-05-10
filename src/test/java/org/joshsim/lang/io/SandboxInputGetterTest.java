
package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SandboxInputGetterTest {

  private Map<String, VirtualFile> virtualFiles;
  private SandboxInputGetter inputGetter;

  @BeforeEach
  void setUp() {
    virtualFiles = new HashMap<>();
    inputGetter = new SandboxInputGetter(virtualFiles);
  }

  @Test
  void testOpenWithNonExistentFile() {
    String identifier = "nonexistent.txt";
    assertThrows(RuntimeException.class, () -> inputGetter.open(identifier));
  }

  @Test
  void testOpenWithTextFile() throws IOException {
    // Arrange
    String content = "Hello, World!";
    String identifier = "test.txt";
    virtualFiles.put(identifier, new VirtualFile(identifier, content, false));

    // Act
    InputStream inputStream = inputGetter.open(identifier);

    // Assert
    String result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(content, result);
  }

  @Test
  void testOpenWithBinaryFile() throws IOException {
    // Arrange
    byte[] originalBytes = "Binary Content".getBytes(StandardCharsets.UTF_8);
    String base64Content = Base64.getEncoder().encodeToString(originalBytes);
    String identifier = "test.bin";
    virtualFiles.put(identifier, new VirtualFile(identifier, base64Content, true));

    // Act
    InputStream inputStream = inputGetter.open(identifier);

    // Assert
    byte[] result = inputStream.readAllBytes();
    assertEquals(new String(originalBytes, StandardCharsets.UTF_8), 
                new String(result, StandardCharsets.UTF_8));
  }

  @Test
  void testOpenWithEmptyTextFile() throws IOException {
    // Arrange
    String content = "";
    String identifier = "empty.txt";
    virtualFiles.put(identifier, new VirtualFile(identifier, content, false));

    // Act
    InputStream inputStream = inputGetter.open(identifier);

    // Assert
    String result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(content, result);
  }

  @Test
  void testOpenWithEmptyBinaryFile() throws IOException {
    // Arrange
    String base64Content = "";
    String identifier = "empty.bin";
    virtualFiles.put(identifier, new VirtualFile(identifier, base64Content, true));

    // Act
    InputStream inputStream = inputGetter.open(identifier);

    // Assert
    byte[] result = inputStream.readAllBytes();
    assertEquals(0, result.length);
  }

  @Test
  void testOpenWithInvalidBase64Content() {
    // Arrange
    String invalidBase64 = "Invalid Base64!@#$";
    String identifier = "invalid.bin";
    virtualFiles.put(identifier, new VirtualFile(identifier, invalidBase64, true));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> inputGetter.open(identifier));
  }
}
