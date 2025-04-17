package org.joshsim.compat;

public class JsUtilityLayer implements UtilityLayer {

  @Override
  public UtilityStringJoiner buildStringJoiner(String delim) {
    return new JsStringJoiner(delim);
  }

}
