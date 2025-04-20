package org.joshsim.compat;

import java.util.StringJoiner;

public class JvmStringJoiner implements CompatibleStringJoiner{

  private final StringJoiner inner;

  public JvmStringJoiner(String delimiter) {
    inner = new StringJoiner(delimiter);
  }

  @Override
  public CompatibleStringJoiner add(CharSequence newPiece) {
    inner.add(newPiece);
    return this;
  }

  public String toString() {
    return inner.toString();
  }

}
