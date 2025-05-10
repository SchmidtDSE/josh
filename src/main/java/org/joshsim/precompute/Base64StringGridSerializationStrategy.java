package org.joshsim.precompute;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * Serialization strategy which reads or writes jshd formatted binary as base64 strings.
 */
public class Base64StringGridSerializationStrategy implements GridSerializationStrategy {

  @Override
  public void serialize(DataGridLayer target, OutputStream outputStream) {

  }

  @Override
  public DataGridLayer deserialize(InputStream inputStream) {
    return null;
  }
}
