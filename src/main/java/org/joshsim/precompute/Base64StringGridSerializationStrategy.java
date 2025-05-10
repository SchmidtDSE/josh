package org.joshsim.precompute;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * Serialization strategy which reads or writes jshd formatted binary as base64 strings.
 *
 * <p>Serialization strategy which decorates another serialization strategy like 
 * BinaryGridSerializationStrategy to read and write base64 encoded binary payloads as opposed to
 * those binary data directly.</p>
 */
public class Base64StringGridSerializationStrategy implements GridSerializationStrategy {

  private final GridSerializationStrategy inner;

  /**
   * Decorate a serialization strategy.
   *
   * @param inner The serialization strategy to be decorated to read and write base64 strings.
   */
  public Base64StringGridSerializationStrategy(GridSerializationStrategy inner) {
    this.inner = inner;
  }
  
  @Override
  public void serialize(DataGridLayer target, OutputStream outputStream) {
    // TODO - serialize with inner and then convert the binary payload produced by inner into a
    // string which, base64 encoded, is sent to output stream.
  }

  @Override
  public DataGridLayer deserialize(InputStream inputStream) {
    // TODO - first parse the input stream as a string and decode it assuming it contains base64
    // encoded data before then deserializing with inner.
  }
}
