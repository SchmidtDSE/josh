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
    //</editor-fold>

    //<editor-fold desc="Comparison">
    public boolean equals(Scalar obj) {
        return value == obj.getAsInt();
    }

    public int compareTo(IntScalar other) {
        return Integer.compare(value, other.getAsInt());
    }

    public int compareTo(DecimalScalar other) {
        return new BigDecimal(value).compareTo(other.getAsDecimal());
    }
    //</editor-fold>

    // FIRST DISPATCH
    // This ensures that the proper method is called, but
    // that we only need to implement each operation once. Each type of scalar needs to know how to perform
    // arithmetic on each other type of scalar as an argument.

    //<editor-fold desc="First Dispatch">

    @Override
    public EngineValue add(EngineValue other) {
        return other.addIntScalar(this);
    }

    @Override
    public EngineValue subtract(EngineValue other) {
        return other.subtractIntScalar(this);
    }

    @Override
    public EngineValue multiply(EngineValue other) {
        return other.multiplyIntScalar(this);
    }

    @Override
    public EngineValue divide(EngineValue other) {
        return other.divideIntScalar(this);
    }

    @Override
    public EngineValue power(EngineValue other) {
        return other.powerIntScalar(this);
    }
    //</editor-fold>

    // SECOND DISPATCH
    // These methods are called by the first dispatch methods, and are implemented in each subclass of Scalar. We
    // need one for each type of scalar that can be passed as an argument.

    //<editor-fold desc="IntScalar Operations">

    @Override
    public EngineValue addIntScalar(IntScalar other) {
        int sum = value + other.getAsInt();
        return new IntScalar(sum, getUnits());
    }

    @Override
    public EngineValue subtractIntScalar(IntScalar other) {
        int difference = value - other.getAsInt();
        return new IntScalar(difference, getUnits());
    }

    @Override
    public EngineValue multiplyIntScalar(IntScalar other) {
        int product = value * other.getAsInt();
        return new IntScalar(product, getUnits());
    }

    @Override
    public EngineValue divideIntScalar(IntScalar other) {
        BigDecimal quotient = new BigDecimal(value).divide(new BigDecimal(other.getAsInt()));
        if (quotient.scale() == 0) {
            return new IntScalar(quotient.intValue(), getUnits());
        } else {
            return new DecimalScalar(quotient, getUnits());
        }
    }

    @Override
    public EngineValue powerIntScalar(IntScalar other) {
        int power = (int) Math.pow(value, other.getAsInt());
        return new IntScalar(power, getUnits());
    }

    //</editor-fold>

    //<editor-fold desc="DecimalScalar Operations">
    @Override
    public EngineValue subtractDecimalScalar(DecimalScalar other) {
        BigDecimal difference = new BigDecimal(value).subtract(other.getAsDecimal());
        return new DecimalScalar(difference, getUnits());
    }

    @Override
    public EngineValue addDecimalScalar(DecimalScalar other) {
        BigDecimal sum = new BigDecimal(value).add(other.getAsDecimal());
        return new DecimalScalar(sum, getUnits());
    }

    @Override
    public EngineValue multiplyDecimalScalar(DecimalScalar other) {
        BigDecimal product = new BigDecimal(value).multiply(other.getAsDecimal());
        return new DecimalScalar(product, getUnits());
    }

    @Override
    public EngineValue divideDecimalScalar(DecimalScalar other) {
        BigDecimal quotient = new BigDecimal(value).divide(other.getAsDecimal());
        return new DecimalScalar(quotient, getUnits());
    }

    @Override
    public EngineValue powerDecimalScalar(DecimalScalar other) {
        BigDecimal power = new BigDecimal(value).pow(other.getAsInt());
        return new DecimalScalar(power, getUnits());
    }
    //</editor-fold>


}
