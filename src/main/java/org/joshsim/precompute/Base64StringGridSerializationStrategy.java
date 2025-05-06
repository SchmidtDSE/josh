package org.joshsim.precompute;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * Serialization strategy which reads or writes jshd formatted binary as base64 strings.
 */
public class Base64StringGridSerializationStrategy implements GridSerializationStrategy {

  @Override
  public void serialize(PrecomputedGrid target, OutputStream outputStream) {

  }

  @Override
  public PrecomputedGrid deserialize(InputStream inputStream) {
    return null;
  }
}
