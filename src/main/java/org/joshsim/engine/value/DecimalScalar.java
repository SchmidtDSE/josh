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

    public EngineValue add(IntScalar other) {
        BigDecimal sum = value.add(new BigDecimal(other.getAsInt()));
        return new DecimalScalar(sum, getUnits());
    }

    public EngineValue add(DecimalScalar other) {
        BigDecimal sum = value.add(other.getAsDecimal());
        return new DecimalScalar(sum, getUnits());
    }

    public EngineValue subtract(IntScalar other) {
        BigDecimal difference = value.subtract(new BigDecimal(other.getAsInt()));
        return new DecimalScalar(difference, getUnits());
    }

    public EngineValue subtract(DecimalScalar other) {
        BigDecimal difference = value.subtract(other.getAsDecimal());
        return new DecimalScalar(difference, getUnits());
    }

    public EngineValue multiply(IntScalar other) {
        BigDecimal product = value.multiply(new BigDecimal(other.getAsInt()));
        return new DecimalScalar(product, getUnits());
    }

    public EngineValue multiply(DecimalScalar other) {
        BigDecimal product = value.multiply(other.getAsDecimal());
        return new DecimalScalar(product, getUnits());
    }

    public EngineValue divide(IntScalar other) {
        BigDecimal quotient = value.divide(new BigDecimal(other.getAsInt()));
        return new DecimalScalar(quotient, getUnits());
    }

    public EngineValue divide(DecimalScalar other) {
        BigDecimal quotient = value.divide(other.getAsDecimal());
        return new DecimalScalar(quotient, getUnits());
    }

    public EngineValue raiseToPower(IntScalar other) {
        BigDecimal raised = value.pow(other.getAsInt());
        return new DecimalScalar(raised, getUnits());
    }

    public EngineValue raiseToPower(DecimalScalar other) {
        BigDecimal raised = value.pow(other.getAsDecimal().intValue());
        return new DecimalScalar(raised, getUnits());
    }

    public boolean equals(EngineValue obj) {
        return value.compareTo(obj.getAsDecimal()) == 0;
    }

    public int compareTo(Scalar other) {
        if (other instanceof IntScalar) {
            return value.compareTo(new BigDecimal(other.getAsInt()));
        } else if (other instanceof DecimalScalar) {
            return value.compareTo(other.getAsDecimal());
        } else {
            throw new IllegalArgumentException("Cannot compare DecimalScalar to " + other.getType());
        }
    }
}