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
   * @param newFirst the first engine value for this tuple, for example the left side operand.
   * @param newSecond the second engine value for this tuple, for example the left side operand.
   */
  public EngineValueTuple(EngineValue newFirst, EngineValue newSecond) {
    first = newFirst;
    second = newSecond;
    types = new TypesTuple(first.getLanguageType(), second.getLanguageType());
    units = new UnitsTuple(first.getUnits(), second.getUnits());
  }

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
   * Tuple describing two identifying values inside of this engine value.
   *
   * <p>Tuple describing two identifying values inside of this engine value such as a pair of units
   * where one unit comes from one engine value and the other unit comes from the other engine
   * value.</p>
   */
  private abstract class InnerTuple {
    private final String first;
    private final String second;

    /**
     * Create a new tuple to represent a pair of identifying names.
     *
     * @param newFirst the first value, for example from the left-side operand.
     * @param newSecond the first value, for example from the right-side operand.
     */
    public InnerTuple(String newFirst, String newSecond) {
      first = newFirst;
      second = newSecond;
    }

    /**
     * Get the first identifying value, for example from the left hand operand.
     *
     * @returns the first identifying value.
     */
    public String getFirst() {
      return first;
    }

    /**
     * Get the second identifying value, for example from the right hand operand.
     *
     * @returns the second identifying value.
     */
    public String getSecond() {
      return second;
    }

    /**
     * Determine if these two identities are compatible without furter casting.
     */
    public boolean getAreCompatible() {
      return getFirst().equals(getSecond());
    }

    /**
     * Get the type of tuple that this represents.
     *
     * @returns tuple type like "units" or "types" which, for example, may correspond to meters or
     *    integer.
     */
    public abstract String getTupleType();

    /**
     * Determine if this tuple equals another tuple.
     *
     * @returns True if the tuples' paired elements have string equality and false otherwise.
     */
    public boolean equals(InnerTuple other) {
      return toString().equals(other.toString());
    }

    @Override
    public String toString() {
      return String.format("%s: %s, %s", getTupleType(), first, second);
    }

    @Override
    public int hashCode() {
      return toString().hashCode();
    }
  }

  /**
   * Typle describing two types that are in this engine value tuple such as int and decimal. 
   */
  public class TypesTuple extends InnerTuple {
    
    /**
     * Create a new types tuple representing a pair of types.
     *
     * <p>This constructor initializes a new pair of types given for the  first and second values. 
     * This might include types, for example, like int or decimal.</p>
     *
     * @param newFirst the first type, representing for example the type of the left-side operand.
     * @param newSecond the second type, representing for example the type of the right-side
     *    operand.
     */
    public TypesTuple(String newFirst, String newSecond) {
      super(newFirst, newSecond);
    }

    @Override
    public String getTupleType() {
      return "types";
    }
    
  }

  /**
   * Typle describing two units that are in this engine value tuple such as meters and centimeters.
   */
  public class UnitsTuple extends InnerTuple {
    
    /**
     * Create a new units tuple representing a pair of units.
     *
     * <p>This constructor initializes a new pair of units given for the first and second values. 
     * This might include units, for example, like meters or centimeters.</p>
     *
     * @param newFirst the first unit, representing for example the unit of the left-side operand.
     * @param newSecond the second unit, representing for example the unit of the right-side
     *    operand.
     */
    public UnitsTuple(String newFirst, String newSecond) {
      super(newFirst, newSecond);
    }

    @Override
    public String getTupleType() {
      return "units";
    }

  }

}
