/**
 * Unit tests for StreamingFormBodyPublisher.
 *
 * <p>Verifies that the streaming form body produces output identical to the original
 * in-memory approach (URLEncoder.encode on concatenated strings), and that large files
 * can be streamed without exceeding memory limits.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for StreamingFormBodyPublisher.
 *
 * <p>Tests wire-format compatibility with the original in-memory serialization approach,
 * correct handling of text and binary files, and large-file streaming.</p>
 */
public class StreamingFormBodyPublisherTest {

  /**
   * Verifies that the streaming output for a binary file matches the original
   * in-memory URLEncoder.encode(Base64.encode(bytes)) approach exactly.
   */
  @Test
  public void testBinaryFileWireFormatCompatibility(@TempDir Path tempDir) throws Exception {
    // Create a binary file with bytes that exercise all Base64 special characters
    byte[] binaryData = {1, 2, 3, 4, 5, (byte) 0xFF, (byte) 0xFE, 0, 127, (byte) 0x80};
    Path binaryFile = tempDir.resolve("test.jshd");
    Files.write(binaryFile, binaryData);

    // === Original approach (in-memory) ===
    String base64 = Base64.getEncoder().encodeToString(binaryData);
    String originalExternalData = "test.jshd\t1\t" + base64 + "\t";
    String originalFormBody = "code=" + URLEncoder.encode("sim code", StandardCharsets.UTF_8)
        + "&name=" + URLEncoder.encode("TestSim", StandardCharsets.UTF_8)
        + "&apiKey=" + URLEncoder.encode("key-123", StandardCharsets.UTF_8)
        + "&favorBigDecimal=" + URLEncoder.encode("true", StandardCharsets.UTF_8)
        + "&externalData=" + URLEncoder.encode(originalExternalData, StandardCharsets.UTF_8);

    // === New streaming approach ===
    Map<String, String> smallFields = new LinkedHashMap<>();
    smallFields.put("code", "sim code");
    smallFields.put("name", "TestSim");
    smallFields.put("apiKey", "key-123");
    smallFields.put("favorBigDecimal", "true");

    List<ExternalFileEntry> files = List.of(
        new ExternalFileEntry("test.jshd", true, binaryFile.toString())
    );

    String streamedFormBody = captureFormBody(smallFields, files, null);

    // Parse both form bodies into field maps and compare
    Map<String, String> originalFields = parseFormData(originalFormBody);
    Map<String, String> streamedFields = parseFormData(streamedFormBody);

    assertEquals(originalFields.get("code"), streamedFields.get("code"));
    assertEquals(originalFields.get("name"), streamedFields.get("name"));
    assertEquals(originalFields.get("apiKey"), streamedFields.get("apiKey"));
    assertEquals(originalFields.get("favorBigDecimal"), streamedFields.get("favorBigDecimal"));
    assertEquals(originalFields.get("externalData"), streamedFields.get("externalData"));
  }

  /**
   * Verifies that the streaming output for a text file matches the original approach.
   */
  @Test
  public void testTextFileWireFormatCompatibility(@TempDir Path tempDir) throws Exception {
    // Create a text file with tabs (which get replaced) and special characters
    String textContent = "Hello\tWorld\nLine 2 with special chars: é & <>";
    Path textFile = tempDir.resolve("config.jshc");
    Files.writeString(textFile, textContent);

    // === Original approach ===
    String processedContent = textContent.replace("\t", "    ");
    String originalExternalData = "config.jshc\t0\t" + processedContent + "\t";
    // === New streaming approach ===
    List<ExternalFileEntry> files = List.of(
        new ExternalFileEntry("config.jshc", false, textFile.toString())
    );

    String streamedFormBody = captureFormBody(Map.of(), files, null);
    // Extract just the externalData value
    Map<String, String> fields = parseFormData(streamedFormBody);
    String streamedExternalData = fields.get("externalData");

    assertEquals(originalExternalData, streamedExternalData);
  }

  /**
   * Verifies wire-format compatibility with mixed text and binary files.
   */
  @Test
  public void testMixedFilesWireFormatCompatibility(@TempDir Path tempDir) throws Exception {
    // Create test files
    byte[] binaryData = {10, 20, 30};
    Path binaryFile = tempDir.resolve("data.jshd");
    Files.write(binaryFile, binaryData);

    String textContent = "key,value\na,1\nb,2";
    Path textFile = tempDir.resolve("params.csv");
    Files.writeString(textFile, textContent);

    // === Original approach ===
    String base64 = Base64.getEncoder().encodeToString(binaryData);
    String originalExternalData = "data.jshd\t1\t" + base64 + "\t"
        + "params.csv\t0\t" + textContent + "\t";

    // === New streaming approach ===
    List<ExternalFileEntry> files = List.of(
        new ExternalFileEntry("data.jshd", true, binaryFile.toString()),
        new ExternalFileEntry("params.csv", false, textFile.toString())
    );

    String streamedFormBody = captureFormBody(Map.of(), files, null);
    Map<String, String> fields = parseFormData(streamedFormBody);

    assertEquals(originalExternalData, fields.get("externalData"));
  }

  /**
   * Verifies that pre-serialized external data string (server-side path) is encoded correctly.
   */
  @Test
  public void testPreSerializedStringFallback() throws Exception {
    String preSerializedData = "config.jshc\t0\tsome content\t";

    Map<String, String> smallFields = new LinkedHashMap<>();
    smallFields.put("code", "test");

    String formBody = captureFormBody(smallFields, List.of(), preSerializedData);
    Map<String, String> fields = parseFormData(formBody);

    assertEquals("test", fields.get("code"));
    assertEquals(preSerializedData, fields.get("externalData"));
  }

  /**
   * Verifies that empty external files produce no externalData field.
   */
  @Test
  public void testEmptyExternalFiles() throws Exception {
    Map<String, String> smallFields = new LinkedHashMap<>();
    smallFields.put("code", "test");

    String formBody = captureFormBody(smallFields, List.of(), null);

    assertEquals("code=test", formBody);
    assertFalse(formBody.contains("externalData"));
  }

  /**
   * Verifies that the Base64UrlEncodingOutputStream correctly encodes all special characters.
   */
  @Test
  public void testBase64UrlEncodingOutputStream() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamingFormBodyPublisher.Base64UrlEncodingOutputStream encoder =
        new StreamingFormBodyPublisher.Base64UrlEncodingOutputStream(baos);

    // Write a mix of safe and special Base64 characters
    // "AB+/=CD" in ASCII bytes
    encoder.write(new byte[]{'A', 'B', '+', '/', '=', 'C', 'D'}, 0, 7);
    encoder.flush();

    assertEquals("AB%2B%2F%3DCD", baos.toString(StandardCharsets.UTF_8));
  }

  /**
   * Verifies single-byte writes through Base64UrlEncodingOutputStream.
   */
  @Test
  public void testBase64UrlEncodingSingleByteWrites() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamingFormBodyPublisher.Base64UrlEncodingOutputStream encoder =
        new StreamingFormBodyPublisher.Base64UrlEncodingOutputStream(baos);

    encoder.write('+');
    encoder.write('A');
    encoder.write('/');
    encoder.write('=');
    encoder.flush();

    assertEquals("%2BA%2F%3D", baos.toString(StandardCharsets.UTF_8));
  }

  /**
   * Stress test: stream a 10MB binary file and verify the output decodes correctly.
   *
   * <p>This verifies that the streaming Base64 + URL-encoding pipeline works end-to-end
   * for non-trivial file sizes without creating intermediate giant Strings.</p>
   */
  @Test
  public void testLargeBinaryFileStreaming(@TempDir Path tempDir) throws Exception {
    // Create a 10MB binary file with random data
    int size = 10 * 1024 * 1024; // 10MB
    byte[] largeData = new byte[size];
    new Random(42).nextBytes(largeData); // Deterministic seed for reproducibility
    Path largeFile = tempDir.resolve("large.jshd");
    Files.write(largeFile, largeData);

    // Stream through the publisher
    List<ExternalFileEntry> files = List.of(
        new ExternalFileEntry("large.jshd", true, largeFile.toString())
    );

    String formBody = captureFormBody(Map.of(), files, null);
    Map<String, String> fields = parseFormData(formBody);
    String externalData = fields.get("externalData");

    // Parse the wire format: filename\tbinary_flag\tcontent\t
    String[] parts = externalData.split("\t", -1);
    assertEquals("large.jshd", parts[0]);
    assertEquals("1", parts[1]);

    // Decode Base64 content and verify it matches original data
    byte[] decodedData = Base64.getDecoder().decode(parts[2]);
    assertArrayEquals(largeData, decodedData);
  }

  /**
   * Verifies that streaming produces identical Base64 output to the in-memory approach
   * for a file whose Base64 contains all three special characters (+, /, =).
   */
  @Test
  public void testBase64SpecialCharacterConsistency(@TempDir Path tempDir) throws Exception {
    // Craft bytes that produce +, /, and = in Base64
    // 0xFB, 0xEF, 0xBE gives "/++=" in Base64 (approximately)
    // Use a range of values to guarantee all three appear
    byte[] data = new byte[256];
    for (int i = 0; i < 256; i++) {
      data[i] = (byte) i;
    }
    Path file = tempDir.resolve("special.jshd");
    Files.write(file, data);

    // In-memory approach
    String expectedBase64 = Base64.getEncoder().encodeToString(data);
    // Streaming approach
    List<ExternalFileEntry> files = List.of(
        new ExternalFileEntry("special.jshd", true, file.toString())
    );
    String formBody = captureFormBody(Map.of(), files, null);
    Map<String, String> fields = parseFormData(formBody);
    String externalData = fields.get("externalData");
    String[] parts = externalData.split("\t", -1);

    // The Base64 content should be identical
    assertEquals(expectedBase64, parts[2]);

    // Verify all three special chars were present (test exercises the substitution paths)
    assertTrue(expectedBase64.contains("+"), "Test data should produce + in Base64");
    assertTrue(expectedBase64.contains("/"), "Test data should produce / in Base64");
    assertTrue(expectedBase64.contains("="), "Test data should produce = in Base64");
  }

  /**
   * Captures the form body output as a String by writing directly to a ByteArrayOutputStream.
   */
  private String captureFormBody(
      Map<String, String> smallFields,
      List<ExternalFileEntry> externalFiles,
      String externalDataString) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamingFormBodyPublisher.writeFormBody(baos, smallFields, externalFiles, externalDataString);
    return baos.toString(StandardCharsets.UTF_8);
  }

  /**
   * Parses a URL-encoded form body into a map of field names to decoded values.
   */
  private Map<String, String> parseFormData(String formData) {
    Map<String, String> fields = new HashMap<>();
    for (String pair : formData.split("&")) {
      String[] kv = pair.split("=", 2);
      if (kv.length == 2) {
        String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
        String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
        fields.put(key, value);
      }
    }
    return fields;
  }
}
