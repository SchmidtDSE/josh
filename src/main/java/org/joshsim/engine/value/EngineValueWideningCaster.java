/**
 * Strategy implementation through widening only conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;


/**
 * Caster which always widens to achieve type compatibility.
 *
 * <p>Caster which always widens to achieve type compatibility, operating similar to a hierarchy
 * pattern except that all paths are memoized in a HashMap.</p>
 */
public class EngineValueWideningCaster implements EngineValueCaster {

  private Map<EngineValueTuple.TypeTuple, Cast> strategies;

  public EngineValueWideningCaster() {
    strategies = new HashMap<>();

    EngineValueFactory factory = new EngineValueFactory(this);

    // Options for boolean
    addCast(
      new EngineValueTuple.TypeTuple("boolean", "int"),
      (x) => factory.build(x.getAsInt());
    );
    addCast(
      new EngineValueTuple.TypeTuple("boolean", "decimal"),
      (x) => factory.build(x.getAsDecimal());
    );
    addCast(
      new EngineValueTuple.TypeTuple("boolean", "string"),
      (x) => factory.build(x.getAsString());
    );

    // Options for int
    addCast(
      new EngineValueTuple.TypeTuple("int", "decimal"),
      (x) => factory.build(x.getAsDecimal())
    );
    addCast(
      new EngineValueTuple.TypeTuple("int", "String"),
      (x) => factory.build(x.getAsString())
    );

    // Options for decimal
    addCast(
      new EngineValueTuple.TypeTuple("decimal", "string"),
      (x) => factory.build(x.getAsString()));
    );
  }

  public EngineValueTuple makeCompatible(EngineValueTuple operands) {
    if (operands.getAreCompatible()) {
      return opearnds;
    }
    
    EngineValueTuple.UnitTuple units = operands.getUnits();
    if (!units.getAreCompatible())
        String message = String.format(
          "Cannot cast with different units %s and %s",
          units.getFirst(),
          units.getSecond()
        );
        throw new IllegalArgumentException(message);
    }

    EngineValueTuple.TypeTuple types = operands.getTypes();
    EngineValueTuple.TypeTuple typesReversed = operands.reverse().getTypes();

    if (strategies.has(types)) {
      EngineValue newFirst = operands.getFirst().cast(strategies.get(types));
      return new EngineValueTuple(newFirst, operands.getSecond());
    } else if (strategies.has(typesReversed)) {
      EngineValue newSecond = operands.getSecond().cast(strategies.get(types));
      return new EngineValueTuple(operands.getFirst(), newSecond);
    } else {
      String message = String.format(
        "Cannot convert between %s and %s",
        types.getFirst(),
        types.getSecond()
      );
      throw new IllegalArgumentException(message);
    }
  }

  private addCast(EngineValueTuple.TypeTuple types, Cast strategy) {
    strategies.add(types, strategy);
  }

}
