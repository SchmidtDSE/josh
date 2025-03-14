package org.joshsim.engine.value;

import java.math.BigDecimal;

public class IntScalar extends Scalar {
    private final int value;

    public IntScalar(int value, String units) {
        super(units);
        this.value = value;
    }

    public int getAsInt() {
        return value;
    }

    public BigDecimal getAsDecimal() {
        return new BigDecimal(value);
    }

    public boolean getAsBoolean() {
        return value != 0;
    }

    public String getAsString() {
        return Integer.toString(value);
    }

    public String getType() {
        return "int";
    }

    public Scalar add(IntScalar other) {
        int sum = value + other.getAsInt();
        return new IntScalar(sum, getUnits());
    }

    public DecimalScalar add(DecimalScalar other) {
        BigDecimal sum = new BigDecimal(value).add(other.getAsDecimal());
        return new DecimalScalar(sum, getUnits());
    }

    public IntScalar subtract(IntScalar other) {
        int difference = value - other.getAsInt();
        return new IntScalar(difference, getUnits());
    }

    public Scalar subtract(DecimalScalar other) {
        BigDecimal difference = new BigDecimal(value).subtract(other.getAsDecimal());
        return new DecimalScalar(difference, getUnits());
    }

    @Override
    public Scalar subtract(Scalar other) {
        if (other instanceof IntScalar) {
            int difference = value - other.getAsInt();
            return new IntScalar(difference, getUnits());
        }
        else if (other instanceof DecimalScalar) {
            BigDecimal difference = new BigDecimal(value).subtract(other.getAsDecimal());
            return new DecimalScalar(difference, getUnits());
        } else {
            // TODO: How to handle BooleanScalar and StringScalar here?
            return null;
        }
    }

    @Override
    public Scalar multiply(Scalar other) {
        if (other instanceof IntScalar) {
            int product = value * other.getAsInt();
            return new IntScalar(product, getUnits());
        }
        else if (other instanceof DecimalScalar) {
            BigDecimal product = new BigDecimal(value).multiply(other.getAsDecimal());
            return new DecimalScalar(product, getUnits());
        } else {
            // TODO: How to handle BooleanScalar and StringScalar here?
            return null;
        }
    }

    @Override
    public EngineValue divide(EngineValue other) {
        return value / other.getAsInt();
    }

    @Override
    public EngineValue raiseToPower(EngineValue other) {
        return Math.pow(value, other.getAsInt());
    }

    @Override
    public String getUnits() {
        return getUnits();
    }

    public boolean equals(Scalar obj) {
        return value == obj.getAsInt();
    }

    @Override
    public int compareTo(EngineValue other) {
        return Integer.compare(value, other.getAsInt());
    }
}
