package org.joshsim.engine.value;


public class EmptyConverter implements Converter {

  @Override
  public Conversion getConversion(Units oldUnits, Units newUnits) {
    EngineValueTuple.UnitsTuple tuple = new EngineValueTuple.UnitsTuple(oldUnits, newUnits);

    if (tuple.getAreCompatible()) {
      return new NoopConversion(newUnits);
    } else {
      String message = String.format(
          "No conversion exists between %s and %s.",
          oldUnits,
          newUnits
      );
      throw new IllegalArgumentException(message);
    }
  }

}
