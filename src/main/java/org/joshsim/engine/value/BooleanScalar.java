package org.joshsim.engine.value;

import java.math.BigDecimal;

public class BooleanScalar extends Scalar {
    private final boolean value;

    /**
     * Creates a new BooleanScalar with the given value.
     *
     * @param value the value of the scalar.
     * @param units the units of the scalar.
     */
    public BooleanScalar(boolean value, String units) {
        super(units);
        this.value = value;
    }

    //<editor-fold desc="Getters">
    /**
     * Returns as a Scalar.
     *
     * @return the scalar value.
     */
    public Scalar getAsScalar() {
        return this;
    }

    /**
     * Returns as a distribution, with just this single value.
     */
    public Distribution getAsDistribution() {
        throw new Error("Not yet implemented");
    }

    /**
     * Gets the value as a boolean.
     *
     * @return the scalar value as a boolean.
     */
    public boolean getAsBoolean() {
        return value;
    }

    /**
     * Gets the value as an integer.
     *
     * @return the scalar value as an int.
     */
    public int getAsInt() {
        return value ? 1 : 0;
    }

    /**
     * Gets the value as a BigDecimal.
     *
     * @return the scalar value as a BigDecimal.
     */
    public BigDecimal getAsDecimal() {
        return value ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    /**
     * Gets the value as a String.
     *
     * @return the scalar value as a String.
     */
    public String getAsString() {
        return Boolean.toString(value);
    }

    /**
     * Gets the type of this scalar value.
     *
     * @return the type as a String.
     */
    public String getType() {
        return "boolean";
    }
    //</editor-fold>

    //<editor-fold desc="Comparison">
    public boolean equals(Scalar obj) {
        return value == obj.getAsBoolean();
    }

    @Override
    public int compareTo(Scalar other) {
        return Boolean.compare(value, other.getAsBoolean());
    }
    //</editor-fold>

    // FIRST DISPATCH
    //<editor-fold desc="First Dispatch">
    @Override
    public EngineValue add(EngineValue other) {
        return other.addBooleanScalar(this);
    }

    @Override
    public EngineValue subtract(EngineValue other) {
        return other.subtractBooleanScalar(this);
    }

    @Override
    public EngineValue multiply(EngineValue other) {
        return other.multiplyBooleanScalar(this);
    }

    @Override
    public EngineValue divide(EngineValue other) {
        return other.divideBooleanScalar(this);
    }

    @Override
    public EngineValue power(EngineValue other) {
        return other.powerBooleanScalar(this);
    }

    @Override
    public EngineValue raiseToPower(EngineValue other) {
        return power(other);
    }
    //</editor-fold>

    // SECOND DISPATCH
    //<editor-fold desc="IntScalar Operations">
    @Override
    public EngineValue addIntScalar(IntScalar other) {
        throw new UnsupportedOperationException("Cannot add integer to boolean");
    }

    @Override
    public EngineValue subtractIntScalar(IntScalar other) {
        throw new UnsupportedOperationException("Cannot subtract integer from boolean");
    }

    @Override
    public EngineValue multiplyIntScalar(IntScalar other) {
        if (value) {
            return new IntScalar(other.getAsInt(), getUnits());
        } else {
            return new IntScalar(0, getUnits());
        }
    }

    @Override
    public EngineValue divideIntScalar(IntScalar other) {
        throw new UnsupportedOperationException("Cannot divide integer by boolean");
    }

    @Override
    public EngineValue powerIntScalar(IntScalar other) {
        throw new UnsupportedOperationException("Cannot raise integer to power of boolean");
    }
    //</editor-fold>

    //<editor-fold desc="DecimalScalar Operations">
    @Override
    public EngineValue addDecimalScalar(DecimalScalar other) {
        throw new UnsupportedOperationException("Cannot add decimal to boolean");
    }

    @Override
    public EngineValue subtractDecimalScalar(DecimalScalar other) {
        throw new UnsupportedOperationException("Cannot subtract decimal from boolean");
    }

    @Override
    public EngineValue multiplyDecimalScalar(DecimalScalar other) {
        if (value) {
            return new DecimalScalar(other.getAsDecimal(), getUnits());
        } else {
            return new DecimalScalar(BigDecimal.ZERO, getUnits());
        }
    }

    @Override
    public EngineValue divideDecimalScalar(DecimalScalar other) {
        throw new UnsupportedOperationException("Cannot divide decimal by boolean");
    }

    @Override
    public EngineValue powerDecimalScalar(DecimalScalar other) {
        throw new UnsupportedOperationException("Cannot raise decimal to power of boolean");
    }
    //</editor-fold>

    //<editor-fold desc="BooleanScalar Operations">
    @Override
    public EngineValue addBooleanScalar(BooleanScalar other) {
        // Implement boolean OR operation
        return new BooleanScalar(value || other.getAsBoolean(), getUnits());
    }

    @Override
    public EngineValue subtractBooleanScalar(BooleanScalar other) {
        throw new UnsupportedOperationException("Cannot subtract boolean from boolean");
    }

    @Override
    public EngineValue multiplyBooleanScalar(BooleanScalar other) {
        // Implement boolean AND operation
        return new BooleanScalar(value && other.getAsBoolean(), getUnits());
    }

    @Override
    public EngineValue divideBooleanScalar(BooleanScalar other) {
        if (!other.getAsBoolean()) {
            throw new ArithmeticException("Division by false (zero)");
        }
        return new BooleanScalar(value, getUnits());
    }

    @Override
    public EngineValue powerBooleanScalar(BooleanScalar other) {
        // If base is false, result is false unless exponent is false (0)
        // If base is true, result is always true
        if (!value && !other.getAsBoolean()) {
            return new BooleanScalar(true, getUnits());
        }
        return new BooleanScalar(value, getUnits());
    }
    //</editor-fold>

    //<editor-fold desc="StringScalar Operations">
    @Override
    public EngineValue addStringScalar(StringScalar other) {
        return new StringScalar(value + other.getAsString(), getUnits());
    }

    @Override
    public EngineValue subtractStringScalar(StringScalar other) {
        throw new UnsupportedOperationException("Cannot subtract string from boolean");
    }

    @Override
    public EngineValue multiplyStringScalar(StringScalar other) {
        if (value) {
            return new StringScalar(other.getAsString(), getUnits());
        } else {
            return new StringScalar("", getUnits());
        }
    }

    @Override
    public EngineValue divideStringScalar(StringScalar other) {
        throw new UnsupportedOperationException("Cannot divide string by boolean");
    }

    @Override
    public EngineValue powerStringScalar(StringScalar other) {
        throw new UnsupportedOperationException("Cannot raise string to power of boolean");
    }
    //</editor-fold>
}