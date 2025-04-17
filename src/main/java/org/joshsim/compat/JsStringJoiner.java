package org.joshsim.compat;


public class JsStringJoiner implements UtilityStringJoiner {

  private final String delim;
  private String value;

  public JsStringJoiner(String delim) {
    this.delim = delim;
    value = "";
  }

  @Override
  public void add(String namePiece) {
    value = value + delim + namePiece;
  }

  @Override
  public String compile() {
    return value;
  }

}
