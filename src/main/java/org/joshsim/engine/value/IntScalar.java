package org.joshsim.engine.value;

import java.math.BigDecimal;

public class IntScalar extends Scalar {
    private final int value;

    /**
     * Creates a new IntScalar with the given value.
     *
     * @param value the value of the scalar.
     * @param units the units of the scalar.
     */
    public IntScalar(int value, String units) {
        super(units);
        this.value = value;
    }

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
     * Gets the value as an integer.
     *
     * @return the scalar value as an int.
     */
    public int getAsInt() {
        return value;
    }

    /**
     * Gets the value as a BigDecimal.
     *
     * @return the scalar value as a BigDecimal.
     */
    public BigDecimal getAsDecimal() {
        return new BigDecimal(value);
    }

    /**
     * Gets the value as a boolean, checking if the integer equals 0.
     *
     * @return the scalar value as a boolean.
     */
    public boolean getAsBoolean() {
        return value != 0;
    }

    /**
     * Gets the value as a String.
     *
     * @return the scalar value as a String.
     */
    public String getAsString() {
        return Integer.toString(value);
    }

    /**
     * Gets the type of this scalar value.
     *
     * @return the type as a String.
     */
    public String getType() {
        return "int";
    }


    public EngineValue add(IntScalar other) {
        int sum = value + other.getAsInt();
        return new IntScalar(sum, getUnits());
    }

    public EngineValue add(DecimalScalar other) {
        BigDecimal sum = new BigDecimal(value).add(other.getAsDecimal());
        return new DecimalScalar(sum, getUnits());
    }

    public EngineValue subtract(IntScalar other) {
        int difference = value - other.getAsInt();
        return new IntScalar(difference, getUnits());
    }

    public EngineValue subtract(DecimalScalar other) {
        BigDecimal difference = new BigDecimal(value).subtract(other.getAsDecimal());
        return new DecimalScalar(difference, getUnits());
    }

    public EngineValue multiply(IntScalar other) {
        int product = value * other.getAsInt();
        return new IntScalar(product, getUnits());
    }

    public EngineValue multiply(DecimalScalar other) {
        BigDecimal product = new BigDecimal(value).multiply(other.getAsDecimal());
        return new DecimalScalar(product, getUnits());
    }

    public EngineValue divide(IntScalar other) {
        int quotient = value / other.getAsInt();
        return new IntScalar(quotient, getUnits());
    }

    public EngineValue divide(DecimalScalar other) {
        BigDecimal quotient = new BigDecimal(value).divide(other.getAsDecimal());
        return new DecimalScalar(quotient, getUnits());
    }

    public EngineValue raiseToPower(IntScalar other) {
        int raised = (int)Math.pow(value, other.getAsInt());
        return new IntScalar(raised, getUnits());
    }

    public boolean equals(Scalar obj) {
        return value == obj.getAsInt();
    }

    public int compareTo(IntScalar other) {
        return Integer.compare(value, other.getAsInt());
    }

    public int compareTo(DecimalScalar other) {
        return new BigDecimal(value).compareTo(other.getAsDecimal());
    }
}
