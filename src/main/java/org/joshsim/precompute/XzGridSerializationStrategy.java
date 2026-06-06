/**
 * Strategy which reads and writes files using XZ compression over an inner strategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;


/**
 * Serialization strategy decorator that applies XZ (LZMA2) compression.
 *
 * <p>Wraps another {@link GridSerializationStrategy} and applies XZ compression on top. This
 * is the format used for {@code .jshdz} files, which are compressed versions of {@code .jshd}
 * binary grids. XZ compression is only available on the JVM; the browser editor continues to
 * use uncompressed {@code .jshd} files.</p>
 */
public class XzGridSerializationStrategy implements GridSerializationStrategy {

  private final GridSerializationStrategy inner;

  /**
   * Decorate a serialization strategy with XZ compression.
   *
   * @param inner The serialization strategy whose output will be XZ-compressed.
   */
  public XzGridSerializationStrategy(GridSerializationStrategy inner) {
    this.inner = inner;
  }

  @Override
  public void serialize(DataGridLayer target, OutputStream outputStream) {
    try (XZCompressorOutputStream xzOut = new XZCompressorOutputStream(outputStream)) {
      inner.serialize(target, xzOut);
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize grid with XZ compression", e);
    }
  }

  @Override
  public DataGridLayer deserialize(InputStream inputStream) {
    try (XZCompressorInputStream xzIn = new XZCompressorInputStream(inputStream)) {
      return inner.deserialize(xzIn);
    } catch (IOException e) {
      throw new RuntimeException("Failed to deserialize grid with XZ decompression", e);
    }
  }

}
