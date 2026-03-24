/**
 * Utility for streaming form-encoded HTTP request bodies from disk.
 *
 * <p>Writes an {@code application/x-www-form-urlencoded} body to a piped stream, streaming
 * the {@code externalData} field from disk files rather than loading everything into a single
 * Java String. This avoids hitting Java's ~2.1GB String length limit when large JSHD files
 * are Base64-encoded and URL-encoded.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Streams a form-encoded HTTP body from disk files, avoiding in-memory String construction.
 *
 * <p>Small fields (code, name, apiKey, etc.) are URL-encoded normally. The {@code externalData}
 * field is streamed from disk: binary files are read in chunks, Base64-encoded via a wrapping
 * OutputStream, and URL-encoded via simple character substitution. Text files are read as strings,
 * tab-replaced, and URL-encoded directly.</p>
 */
public class StreamingFormBodyPublisher {

  private static final int BUFFER_SIZE = 8192;

  /**
   * Creates a streaming body publisher for the given form fields and external files.
   *
   * @param smallFields Map of small field name-value pairs to URL-encode normally
   * @param externalFiles List of external file entries to stream as the externalData field
   * @return An HttpRequest.BodyPublisher that streams the body from disk
   */
  public static HttpRequest.BodyPublisher create(
      Map<String, String> smallFields,
      List<ExternalFileEntry> externalFiles) {
    return create(smallFields, externalFiles, null);
  }

  /**
   * Creates a streaming body publisher for the given form fields and external data.
   *
   * <p>If {@code externalFiles} is non-empty, the externalData field is streamed from
   * disk files. Otherwise, if {@code externalDataString} is non-null, it is used as a
   * pre-serialized external data value (server-side pass-through).</p>
   *
   * @param smallFields Map of small field name-value pairs to URL-encode normally
   * @param externalFiles List of external file entries to stream as the externalData field
   * @param externalDataString Pre-serialized external data string (used when files are empty)
   * @return An HttpRequest.BodyPublisher that streams the body
   */
  public static HttpRequest.BodyPublisher create(
      Map<String, String> smallFields,
      List<ExternalFileEntry> externalFiles,
      String externalDataString) {

    return HttpRequest.BodyPublishers.ofInputStream(() -> {
      PipedInputStream pipedIn = new PipedInputStream(65536);
      PipedOutputStream pipedOut;
      try {
        pipedOut = new PipedOutputStream(pipedIn);
      } catch (IOException e) {
        throw new RuntimeException("Failed to create piped stream", e);
      }

      Thread writerThread = new Thread(() -> {
        try {
          writeFormBody(pipedOut, smallFields, externalFiles, externalDataString);
        } catch (IOException e) {
          throw new RuntimeException("Failed to write streaming form body", e);
        } finally {
          try {
            pipedOut.close();
          } catch (IOException ignored) {
            // Closing the pipe is best-effort
          }
        }
      }, "streaming-form-body-writer");
      writerThread.setDaemon(true);
      writerThread.start();

      return pipedIn;
    });
  }

  /**
   * Writes the complete form body to the output stream.
   *
   * @param out The output stream to write to
   * @param smallFields Map of small field name-value pairs
   * @param externalFiles List of external file entries
   * @param externalDataString Pre-serialized external data string (fallback when files empty)
   * @throws IOException if writing fails
   */
  static void writeFormBody(
      OutputStream out,
      Map<String, String> smallFields,
      List<ExternalFileEntry> externalFiles,
      String externalDataString) throws IOException {

    boolean first = true;

    // Write small fields as normal URL-encoded key=value pairs
    for (Map.Entry<String, String> entry : smallFields.entrySet()) {
      if (!first) {
        out.write('&');
      }
      first = false;

      String encoded = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
          + "="
          + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
      out.write(encoded.getBytes(StandardCharsets.UTF_8));
    }

    // Write externalData field: stream from files if available, else use pre-serialized string
    if (!externalFiles.isEmpty()) {
      if (!first) {
        out.write('&');
      }
      out.write("externalData=".getBytes(StandardCharsets.UTF_8));
      writeExternalData(out, externalFiles);
    } else if (externalDataString != null) {
      if (!first) {
        out.write('&');
      }
      out.write("externalData=".getBytes(StandardCharsets.UTF_8));
      writeUrlEncoded(out, externalDataString);
    }

    out.flush();
  }

  /**
   * Streams the externalData value from disk files in the wire format.
   *
   * <p>Wire format: {@code filename\tbinary_flag\tcontent\t} for each file,
   * all URL-encoded. Tab delimiters are encoded as {@code %09}.</p>
   *
   * @param out The output stream to write to
   * @param externalFiles The external file entries to stream
   * @throws IOException if reading or writing fails
   */
  private static void writeExternalData(
      OutputStream out,
      List<ExternalFileEntry> externalFiles) throws IOException {

    for (ExternalFileEntry entry : externalFiles) {
      // Write: URL-encoded filename
      writeUrlEncoded(out, entry.filename());
      // Write: URL-encoded tab delimiter
      out.write("%09".getBytes(StandardCharsets.UTF_8));

      // Write: URL-encoded binary flag
      writeUrlEncoded(out, entry.isBinary() ? "1" : "0");
      // Write: URL-encoded tab delimiter
      out.write("%09".getBytes(StandardCharsets.UTF_8));

      // Write: URL-encoded content (streamed from disk)
      if (entry.isBinary()) {
        streamBinaryFileContent(out, entry.filePath());
      } else {
        streamTextFileContent(out, entry.filePath());
      }

      // Write: URL-encoded tab delimiter
      out.write("%09".getBytes(StandardCharsets.UTF_8));
    }
  }

  /**
   * Streams a binary file as Base64-encoded, URL-encoded content without intermediate Strings.
   *
   * <p>Uses {@code Base64.getEncoder().wrap()} to stream Base64 encoding directly through
   * a URL-encoding filter OutputStream. The file is read in chunks, Base64-encoded on the fly,
   * and each Base64 byte is URL-encoded via simple character substitution. This avoids creating
   * any intermediate String that could exceed Java's ~2.1GB limit.</p>
   *
   * @param out The output stream to write to
   * @param filePath Path to the binary file
   * @throws IOException if reading or writing fails
   */
  private static void streamBinaryFileContent(OutputStream out, String filePath)
      throws IOException {
    // Chain: file bytes → Base64 encoder → URL-encoding filter → output stream
    // No intermediate Strings are created at any point.
    Base64UrlEncodingOutputStream urlEncodingOut = new Base64UrlEncodingOutputStream(out);
    try (InputStream fileIn = Files.newInputStream(Paths.get(filePath));
         OutputStream b64Out = Base64.getEncoder().wrap(urlEncodingOut)) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = fileIn.read(buffer)) != -1) {
        b64Out.write(buffer, 0, bytesRead);
      }
    }
    // Closing b64Out flushes Base64 padding; urlEncodingOut does not close the delegate
  }

  /**
   * Streams a text file as URL-encoded content with tab replacement.
   *
   * <p>Text files are small enough to read into memory. Tabs are replaced with
   * spaces for safety (matching the original serialization behavior), then the
   * content is URL-encoded.</p>
   *
   * @param out The output stream to write to
   * @param filePath Path to the text file
   * @throws IOException if reading or writing fails
   */
  private static void streamTextFileContent(OutputStream out, String filePath)
      throws IOException {
    String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    content = content.replace("\t", "    "); // Tab replacement for safety
    writeUrlEncoded(out, content);
  }

  /**
   * URL-encodes a string and writes it to the output stream.
   *
   * @param out The output stream to write to
   * @param value The string value to URL-encode and write
   * @throws IOException if writing fails
   */
  private static void writeUrlEncoded(OutputStream out, String value) throws IOException {
    String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
    out.write(encoded.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * OutputStream filter that URL-encodes Base64 output bytes via character substitution.
   *
   * <p>The Base64 alphabet (A-Z, a-z, 0-9, +, /, =) only requires percent-encoding
   * of three characters: {@code +} → {@code %2B}, {@code /} → {@code %2F},
   * {@code =} → {@code %3D}. All other characters pass through unchanged.</p>
   *
   * <p>Closing this stream does NOT close the delegate, since the caller manages
   * the delegate's lifecycle.</p>
   */
  static class Base64UrlEncodingOutputStream extends OutputStream {
    private final OutputStream delegate;

    Base64UrlEncodingOutputStream(OutputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
      switch (b) {
        case '+' -> delegate.write(PERCENT_2B, 0, 3);
        case '/' -> delegate.write(PERCENT_2F, 0, 3);
        case '=' -> delegate.write(PERCENT_3D, 0, 3);
        default -> delegate.write(b);
      }
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
      // Bulk-process to avoid per-byte overhead. Worst case: every byte expands to 3.
      byte[] buf = new byte[len * 3];
      int pos = 0;

      for (int i = off; i < off + len; i++) {
        switch (bytes[i]) {
          case '+' -> {
            buf[pos++] = '%';
            buf[pos++] = '2';
            buf[pos++] = 'B';
          }
          case '/' -> {
            buf[pos++] = '%';
            buf[pos++] = '2';
            buf[pos++] = 'F';
          }
          case '=' -> {
            buf[pos++] = '%';
            buf[pos++] = '3';
            buf[pos++] = 'D';
          }
          default -> buf[pos++] = bytes[i];
        }
      }

      delegate.write(buf, 0, pos);
    }

    @Override
    public void flush() throws IOException {
      delegate.flush();
    }

    @Override
    public void close() throws IOException {
      // Intentionally do NOT close delegate — caller manages it
      flush();
    }

    private static final byte[] PERCENT_2B = {'%', '2', 'B'};
    private static final byte[] PERCENT_2F = {'%', '2', 'F'};
    private static final byte[] PERCENT_3D = {'%', '3', 'D'};
  }
}
