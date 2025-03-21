/**
 * Structures describing pairs of interacting engine values.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;


/**
 * Pair of engine values interacting in an operation.
 */
public class EngineValueTuple {

  private final EngineValue first;
  private final EngineValue second;
  private final TypesTuple types;
  private final UnitsTuple units;

  /**
   * Create a new tuple of engine values.
   *
   * @param first the first engine value for this tuple, for example the left side operand.
   * @param second the second engine value for this tuple, for example the left side operand.
   */
  public EngineValueTuple(EngineValue first, EngineValue second) {
    this.first = first;
    this.second = second;
    types = new TypesTuple(first.getLanguageType(), second.getLanguageType());
    units = new UnitsTuple(first.getUnits(), second.getUnits());
  }

  /**
   * Reverse the order of the values within the tuple.
   *
   * @returns copy of this tuple with order of operands reversed.
   */
  public EngineValueTuple reverse() {
    return new EngineValueTuple(getSecond(), getFirst());
  }

  /**
   * Determine if the two values in this tuple are compatable without further casting.
   */ 
  public boolean getAreCompatible() {
    return types.getAreCompatible() && units.getAreCompatible();
  }

  /**
   * Get the first engine value for this tuple.
   *
   * @return the first engine value, for example the left operand.
   */
  public EngineValue getFirst() {
    return first;
  }

  /**
   * Get the second engine value for this tuple.
   *
   * @return the second engine value, for example the right operand.
   */
  public EngineValue getSecond() {
    return second;
  }

  /**
   * Get the types tuple for this engine value tuple.
   *
   * @return the types tuple like for int and decimal.
   */
  public TypesTuple getTypes() {
    return types;
  }

  /**
   * Get the units tuple for this engine value tuple.
   *
   * @return the units tuple like for meters and cenitmeters.
   */
  public UnitsTuple getUnits() {
    return units;
  }

  /**
   * Typle describing two types that are in this engine value tuple such as int and decimal. 
   */
  public static class TypesTuple {

    private final LanguageType first;
    private final LanguageType second;
    
    /**
     * Create a new types tuple representing a pair of types.
     *
     * <p>This constructor initializes a new pair of types given for the first and second values. 
     * This might include types, for example, like int or decimal.</p>
     *
     * @param first the first type, representing for example the type of the left-side operand.
     * @param second the second type, representing for example the type of the right-side
     *     operand.
     */
    public TypesTuple(LanguageType first, LanguageType second) {
      this.first = first;
      this.second = second;
    }

    /**
     * Get the first identifying value, for example from the left hand operand.
     *
     * @returns the first identifying value.
     */
    public LanguageType getFirst() {
      return first;
    }

    /**
     * Get the second identifying value, for example from the right hand operand.
     *
     * @returns the second identifying value.
     */
    public LanguageType getSecond() {
      return second;
    }

    /**
     * Determine if the two language types in this tuple are compatable for use in operations.
     *
     * @return true if compatiable and false otherwise.
     */
    public boolean getAreCompatible() {
      return first.getRootType().equals(second.getRootType());
    }

    /**
     * Convert to a string representation using the roots of both types.
     *
     * @returns string representation using root types.
     */
    public String toRootString() {
      return String.format("types: %s, %s", first.getRootType(), second.getRootType());
    }

    /**
     * Determine equality by if two language types tuples are compatible.
     *
     * @param other operand
     * @return true if compatible (equivalent for purposes of operations) and false otherwise.
     */
    public boolean equals(TypesTuple other) {
      return toRootString().equals(other.toRootString());
    }

    @Override
    public boolean equals(Object other) {
      return equals((TypesTuple) other);
    }

    @Override
    public int hashCode() {
      return toRootString().hashCode();
    }

  }

  /**
   * Typle describing two units that are in this engine value tuple such as meters and centimeters.
   */
  public static class UnitsTuple {

    private final Units first;
    private final Units second;

    /**
     * Create a new tuple to represent a pair of identifying names.
     *
     * @param first the first value, for example from the left-side operand.
     * @param second the second value, for example from the right-side operand.
     */
    public UnitsTuple(Units first, Units second) {
      this.first = first;
      this.second = second;
    }

    /**
     * Get the first identifying value, for example from the left hand operand.
     *
     * @returns the first identifying value.
     */
    public Units getFirst() {
      return first;
    }

    /**
     * Get the second identifying value, for example from the right hand operand.
     *
     * @returns the second identifying value.
     */
    public Units getSecond() {
      return second;
    }

    /**
     * Determine if these two identities are compatible without furter casting.
     */
    public boolean getAreCompatible() {
      return getFirst().equals(getSecond());
    }

    @Override
    public boolean equals(Object other) {
      return toString().equals(other.toString());
    }

    @Override
    public String toString() {
      return String.format("units: %s, %s", first, second);
    }

    @Override
    public int hashCode() {
      return toString().hashCode();
    }
    
  }

}
