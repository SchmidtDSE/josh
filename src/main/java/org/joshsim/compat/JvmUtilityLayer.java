package org.joshsim.compat;

public class JvmUtilityLayer implements UtilityLayer {

  @Override
  public UtilityStringJoiner buildStringJoiner(String delim) {
    return new JvmStringJoiner(delim);
  }

}
