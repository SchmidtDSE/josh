/**
 @license BSD-3-Clause
 */

package org.dse.JoshLang;

/**
 * Represents a value in the engine.
 */
public class EngineValue {
    private final double value;

    /**
     * Constructs an EngineValue with the specified value.
     *
     * @param value the value to be set
     */
    public EngineValue(double value) {
        this.value = value;
    }

    /**
     * Adds this value to another value.
     *
     * @param other the other value
     * @return the result of the addition
     */
    public EngineValue add(EngineValue other) {
        return new EngineValue(this.value + other.value);
    }

    /**
     * Subtracts another value from this value.
     *
     * @param other the other value
     * @return the result of the subtraction
     */
    public EngineValue subtract(EngineValue other) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Multiplies this value by another value.
     *
     * @param other the other value
     * @return the result of the multiplication
     */
    public EngineValue multiply(EngineValue other) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Divides this value by another value.
     *
     * @param other the other value
     * @return the result of the division
     */
    public EngineValue divide(EngineValue other) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Raises this value to the power of another value.
     *
     * @param other the other value
     * @return the result of the exponentiation
     */
    public EngineValue raiseToPower(EngineValue other) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Gets the units of this value.
     *
     * @return the units of this value
     */
    public String getUnits() {
        // TODO: Implement this method
        return null;
    }

    /**
     * Compares this EngineValue to the specified object for equality.
     * Two EngineValue objects are considered equal if they have the same numeric value.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EngineValue that = (EngineValue) obj;
        return Double.compare(that.value, value) == 0;
    }
}