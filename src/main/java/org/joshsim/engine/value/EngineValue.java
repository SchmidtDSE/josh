/**
 @license BSD-3-Clause
 */
package org.joshsim.engine.value;


/**
 * Represents a value in the engine.
 */
public interface EngineValue {

    /**
     * Adds this value to another value.
     *
     * @param other the other value
     * @return the result of the addition
     * @throws IllegalArgumentException if units are incompatible
     */
    EngineValue add(EngineValue other);

    /**
     * Subtracts another value from this value.
     *
     * @param other the other value
     * @return the result of the subtraction
     * @throws IllegalArgumentException if units are incompatible
     */
    EngineValue subtract(EngineValue other);

    /**
     * Multiplies this value by another value.
     *
     * @param other the other value
     * @return the result of the multiplication
     * @throws IllegalArgumentException if units are incompatible
     */
    EngineValue multiply(EngineValue other);

    /**
     * Divides this value by another value.
     *
     * @param other the other value
     * @return the result of the division
     * @throws IllegalArgumentException if units are incompatible
     * @throws ArithmeticException if division by zero is attempted
     */
    EngineValue divide(EngineValue other);

    /**
     * Raises this value to the power of another value.
     *
     * @param other the other value
     * @return the result of the exponentiation
     * @throws IllegalArgumentException if units are incompatible
     * @throws ArithmeticException if division by zero is attempted
     */
    EngineValue raiseToPower(EngineValue other);

    /**
     * Gets the units of this value.
     *
     * @return the units of this value
     */
    String getUnits();

    /**
     * Gets the numerical value.
     * 
     * @return the numerical value
     */
    double getValue();
    
    /**
     * Compares this EngineValue to the specified object for equality.
     * Two EngineValue objects are considered equal if they have the same numeric value.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    boolean equals(Object obj);

}