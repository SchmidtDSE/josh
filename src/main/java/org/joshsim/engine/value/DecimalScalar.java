package org.joshsim.engine.value;

import java.math.BigDecimal;

public class DecimalScalar extends Scalar {
    private final BigDecimal value;

    /**
     * Creates a new DecimalScalar with the given value.
     *
     * @param value the value of the scalar.
     * @param units the units of the scalar.
     */
    public DecimalScalar(BigDecimal value, String units) {
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
     * Gets the value as a BigDecimal.
     *
     * @return the scalar value as a BigDecimal.
     */
    public BigDecimal getAsDecimal() {
        return value;
    }

    /**
     * Gets the value as an integer.
     *
     * @return the scalar value as an int.
     */
    public int getAsInt() {
        return value.intValue();
    }

    /**
     * Gets the value as a boolean, checking if the decimal equals 0.
     *
     * @return the scalar value as a boolean.
     */
    public boolean getAsBoolean() {
        return value.compareTo(BigDecimal.ZERO) != 0;
    }

    /**
     * Gets the value as a String.
     *
     * @return the scalar value as a String.
     */
    public String getAsString() {
        return value.toString();
    }

    /**
     * Gets the type of this scalar value.
     *
     * @return the type as a String.
     */
    public String getType() {
        return "decimal";
    }
    //</editor-fold>

    //<editor-fold desc="Comparison">
    public boolean equals(Scalar obj) {
        return value.compareTo(obj.getAsDecimal()) == 0;
    }

    public int compareTo(Scalar other) {
        return value.compareTo(other.getAsDecimal());
    }
    //</editor-fold>

    // FIRST DISPATCH
    //<editor-fold desc="First Dispatch">
    @Override
    public EngineValue add(EngineValue other) {
        return other.addDecimalScalar(this);
    }

    @Override
    public EngineValue subtract(EngineValue other) {
        return other.subtractDecimalScalar(this);
    }

    @Override
    public EngineValue multiply(EngineValue other) {
        return other.multiplyDecimalScalar(this);
    }

    @Override
    public EngineValue divide(EngineValue other) {
        return other.divideDecimalScalar(this);
    }

    @Override
    public EngineValue power(EngineValue other) {
        return other.powerDecimalScalar(this);
    }
    //</editor-fold>

    // SECOND DISPATCH
    //<editor-fold desc="IntScalar Operations">
    @Override
    public EngineValue addIntScalar(IntScalar other) {
        BigDecimal sum = value.add(new BigDecimal(other.getAsInt()));
        return new DecimalScalar(sum, getUnits());
    }

    @Override
    public EngineValue subtractIntScalar(IntScalar other) {
        BigDecimal difference = value.subtract(new BigDecimal(other.getAsInt()));
        return new DecimalScalar(difference, getUnits());
    }

    @Override
    public EngineValue multiplyIntScalar(IntScalar other) {
        BigDecimal product = value.multiply(new BigDecimal(other.getAsInt()));
        return new DecimalScalar(product, getUnits());
    }

    @Override
    public EngineValue divideIntScalar(IntScalar other) {
        BigDecimal quotient = value.divide(new BigDecimal(other.getAsInt()));
        return new DecimalScalar(quotient, getUnits());
    }

    @Override
    public EngineValue powerIntScalar(IntScalar other) {
        BigDecimal raised = value.pow(other.getAsInt());
        return new DecimalScalar(raised, getUnits());
    }
    //</editor-fold>

    //<editor-fold desc="DecimalScalar Operations">
    @Override
    public EngineValue addDecimalScalar(DecimalScalar other) {
        BigDecimal sum = value.add(other.getAsDecimal());
        return new DecimalScalar(sum, getUnits());
    }

    @Override
    public EngineValue subtractDecimalScalar(DecimalScalar other) {
        BigDecimal difference = value.subtract(other.getAsDecimal());
        return new DecimalScalar(difference, getUnits());
    }

    @Override
    public EngineValue multiplyDecimalScalar(DecimalScalar other) {
        BigDecimal product = value.multiply(other.getAsDecimal());
        return new DecimalScalar(product, getUnits());
    }

    @Override
    public EngineValue divideDecimalScalar(DecimalScalar other) {
        BigDecimal quotient = value.divide(other.getAsDecimal());
        return new DecimalScalar(quotient, getUnits());
    }

    @Override
    public EngineValue powerDecimalScalar(DecimalScalar other) {
        BigDecimal raised = value.pow(other.getAsInt());
        return new DecimalScalar(raised, getUnits());
    }
    //</editor-fold>

    //<editor-fold desc="BooleanScalar Operations">
    @Override
    public EngineValue addBooleanScalar(BooleanScalar other) {
        throw new UnsupportedOperationException("Cannot add boolean to decimal");
    }

    @Override
    public EngineValue subtractBooleanScalar(BooleanScalar other) {
        throw new UnsupportedOperationException("Cannot subtract boolean from decimal");
    }

    @Override
    public EngineValue multiplyBooleanScalar(BooleanScalar other) {
        if (other.getAsBoolean()) {
            return new DecimalScalar(value, getUnits());
        } else {
            return new DecimalScalar(BigDecimal.ZERO, getUnits());
        }
    }

    @Override
    public EngineValue divideBooleanScalar(BooleanScalar other) {
        throw new UnsupportedOperationException("Cannot divide decimal by boolean");
    }

    @Override
    public EngineValue powerBooleanScalar(BooleanScalar other) {
        throw new UnsupportedOperationException("Cannot raise decimal to power of boolean");
    }
    //</editor-fold>

    //<editor-fold desc="StringScalar Operations">
    @Override
    public EngineValue addStringScalar(StringScalar other) {
        return new StringScalar(value.toString() + other.getAsString(), getUnits());
    }

    @Override
    public EngineValue subtractStringScalar(StringScalar other) {
        throw new UnsupportedOperationException("Cannot subtract string from decimal");
    }

    @Override
    public EngineValue multiplyStringScalar(StringScalar other) {
        throw new UnsupportedOperationException("Cannot multiply decimal by string");
    }

    @Override
    public EngineValue divideStringScalar(StringScalar other) {
        throw new UnsupportedOperationException("Cannot divide decimal by string");
    }

    @Override
    public EngineValue powerStringScalar(StringScalar other) {
        throw new UnsupportedOperationException("Cannot raise decimal to power of string");
    }
    //</editor-fold>
}
