
/**
 * Strategy which reads and writes files using the jshd format.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.joshsim.engine.value.engine.EngineValueFactory;


/**
 * Strategy which reads or writes a grid serialization with binary values.
 *
 * <p>Strategy which reads or writes a grid serialization through a binary format. This uses the
 * jshd binary format.</p>
 */
public class BinaryGridSerializationStrategy implements GridSerializationStrategy {

  private final EngineValueFactory engineValueFactory;

  /**
   * Construct a serialization strategy using the given engine factory.
   *
   * @param engineValueFactory the factory used to create values during the deserialization process
   */
  public BinaryGridSerializationStrategy(EngineValueFactory engineValueFactory) {
    this.engineValueFactory = engineValueFactory;
  }

  @Override
  public void serialize(PrecomputedGrid target, OutputStream outputStream) {
    if (!(target instanceof DoublePrecomputedGrid)) {
      throw new IllegalArgumentException(
          "Binary serialization currently only supports DoublePrecomputedGrid"
      );
    }
    try {
      byte[] serialized = JshdUtil.serializeToBytes((DoublePrecomputedGrid) target);
      outputStream.write(serialized);
      outputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize grid to output stream", e);
    }
  }

  @Override
  public PrecomputedGrid deserialize(InputStream inputStream) {
    try {
      byte[] bytes = inputStream.readAllBytes();
      return JshdUtil.loadFromBytes(engineValueFactory, bytes);
    } catch (IOException e) {
      throw new RuntimeException("Failed to deserialize grid from input stream", e);
    }
  }
}
