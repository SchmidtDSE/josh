package org.joshsim.engine.value;

import java.math.BigDecimal;

public class StringScalar extends Scalar {
  private final String value;

  /**
   * Creates a new StringScalar with the given value.
   *
   * @param value the value of the scalar.
   * @param units the units of the scalar.
   */
  public StringScalar(String value, String units) {
    super(units);
    this.value = value;
  }

  // <editor-fold desc="Getters">
  /**
   * Returns as a Scalar.
   *
   * @return the scalar value.
   */
  public Scalar getAsScalar() {
    return this;
  }

  /** Returns as a distribution, with just this single value. */
  public Distribution getAsDistribution() {
    throw new Error("Not yet implemented");
  }

  /**
   * Gets the value as a String.
   *
   * @return the scalar value as a String.
   */
  public String getAsString() {
    return value;
  }

  /**
   * Gets the value as an integer.
   *
   * @return the scalar value as an int.
   */
  public int getAsInt() {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new UnsupportedOperationException("Cannot convert string to int: " + value);
    }
  }

  /**
   * Gets the value as a BigDecimal.
   *
   * @return the scalar value as a BigDecimal.
   */
  public BigDecimal getAsDecimal() {
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException e) {
      throw new UnsupportedOperationException("Cannot convert string to decimal: " + value);
    }
  }

  /**
   * Gets the value as a boolean.
   *
   * @return the scalar value as a boolean.
   */
  public boolean getAsBoolean() {
    return Boolean.parseBoolean(value);
  }

  /**
   * Gets the type of this scalar value.
   *
   * @return the type as a String.
   */
  public String getType() {
    return "string";
  }

  // </editor-fold>

  // <editor-fold desc="Comparison">
  @Override
  public boolean equals(Scalar obj) {
    return value.equals(obj.getAsString());
  }

  @Override
  public int compareTo(Scalar other) {
    return value.compareTo(other.getAsString());
  }

  // </editor-fold>

  // FIRST DISPATCH
  // <editor-fold desc="First Dispatch">
  @Override
  public EngineValue add(EngineValue other) {
    return other.addStringScalar(this);
  }

  @Override
  public EngineValue subtract(EngineValue other) {
    return other.subtractStringScalar(this);
  }

  @Override
  public EngineValue multiply(EngineValue other) {
    return other.multiplyStringScalar(this);
  }

  @Override
  public EngineValue divide(EngineValue other) {
    return other.divideStringScalar(this);
  }

  @Override
  public EngineValue power(EngineValue other) {
    return other.powerStringScalar(this);
  }

  // </editor-fold>

  // SECOND DISPATCH
  // <editor-fold desc="IntScalar Operations">
  @Override
  public EngineValue addIntScalar(IntScalar other) {
    return new StringScalar(other.getAsString() + value, getUnits());
  }

  @Override
  public EngineValue subtractIntScalar(IntScalar other) {
    throw new UnsupportedOperationException("Cannot subtract integer from string");
  }

  @Override
  public EngineValue multiplyIntScalar(IntScalar other) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < other.getAsInt(); i++) {
      result.append(value);
    }
    return new StringScalar(result.toString(), getUnits());
  }

  @Override
  public EngineValue divideIntScalar(IntScalar other) {
    throw new UnsupportedOperationException("Cannot divide integer by string");
  }

  @Override
  public EngineValue powerIntScalar(IntScalar other) {
    throw new UnsupportedOperationException("Cannot raise integer to power of string");
  }

  // </editor-fold>

  // <editor-fold desc="DecimalScalar Operations">
  @Override
  public EngineValue addDecimalScalar(DecimalScalar other) {
    return new StringScalar(other.getAsString() + value, getUnits());
  }

  @Override
  public EngineValue subtractDecimalScalar(DecimalScalar other) {
    throw new UnsupportedOperationException("Cannot subtract decimal from string");
  }

  @Override
  public EngineValue multiplyDecimalScalar(DecimalScalar other) {
    throw new UnsupportedOperationException("Cannot multiply decimal by string");
  }

  @Override
  public EngineValue divideDecimalScalar(DecimalScalar other) {
    throw new UnsupportedOperationException("Cannot divide decimal by string");
  }

  @Override
  public EngineValue powerDecimalScalar(DecimalScalar other) {
    throw new UnsupportedOperationException("Cannot raise decimal to power of string");
  }

  // </editor-fold>

  // <editor-fold desc="BooleanScalar Operations">
  @Override
  public EngineValue addBooleanScalar(BooleanScalar other) {
    return new StringScalar(other.getAsString() + value, getUnits());
  }

  @Override
  public EngineValue subtractBooleanScalar(BooleanScalar other) {
    throw new UnsupportedOperationException("Cannot subtract boolean from string");
  }

  @Override
  public EngineValue multiplyBooleanScalar(BooleanScalar other) {
    if (other.getAsBoolean()) {
      return new StringScalar(value, getUnits());
    } else {
      return new StringScalar("", getUnits());
    }
  }

  @Override
  public EngineValue divideBooleanScalar(BooleanScalar other) {
    throw new UnsupportedOperationException("Cannot divide boolean by string");
  }

  @Override
  public EngineValue powerBooleanScalar(BooleanScalar other) {
    throw new UnsupportedOperationException("Cannot raise boolean to power of string");
  }

  // </editor-fold>

  // <editor-fold desc="StringScalar Operations">
  @Override
  public EngineValue addStringScalar(StringScalar other) {
    return new StringScalar(value + other.getAsString(), getUnits());
  }

  @Override
  public EngineValue subtractStringScalar(StringScalar other) {
    throw new UnsupportedOperationException("Cannot subtract string from string");
  }

  @Override
  public EngineValue multiplyStringScalar(StringScalar other) {
    throw new UnsupportedOperationException("Cannot multiply string by string");
  }

  @Override
  public EngineValue divideStringScalar(StringScalar other) {
    throw new UnsupportedOperationException("Cannot divide string by string");
  }

  @Override
  public EngineValue powerStringScalar(StringScalar other) {
    throw new UnsupportedOperationException("Cannot raise string to power of string");
  }
  // </editor-fold>
}
