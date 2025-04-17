package org.joshsim.compat;

import java.util.StringJoiner;


public class JvmStringJoiner implements UtilityStringJoiner {

  private final StringJoiner jvmStringJoiner;

  public JvmStringJoiner(String delim) {
    jvmStringJoiner = new StringJoiner(delim);
  }

  @Override
  public void add(String namePiece) {
    jvmStringJoiner.add(namePiece);
  }

  @Override
  public String compile() {
    return jvmStringJoiner.toString();
  }
}
