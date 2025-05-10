package org.joshsim.precompute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;


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
    try {
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      inner.serialize(target, byteStream);
      byte[] bytes = byteStream.toByteArray();
      String base64String = Base64.getEncoder().encodeToString(bytes);
      outputStream.write(base64String.getBytes());
      outputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize grid to base64", e);
    }
  }

  @Override
  public DataGridLayer deserialize(InputStream inputStream) {
    try {
      byte[] bytes = inputStream.readAllBytes();
      String base64String = new String(bytes);
      byte[] decodedBytes = Base64.getDecoder().decode(base64String);
      ByteArrayInputStream byteStream = new ByteArrayInputStream(decodedBytes);
      return inner.deserialize(byteStream);
    } catch (IOException e) {
      throw new RuntimeException("Failed to deserialize grid from base64", e);
    }
  }
}
