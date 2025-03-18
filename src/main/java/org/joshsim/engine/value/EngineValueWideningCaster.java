/**
 * Strategy implementation through widening only conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.util.HashMap;
import java.util.Map;


/**
 * Caster which always widens to achieve type compatibility.
 *
 * <p>Caster which always widens to achieve type compatibility, operating similar to a hierarchy
 * pattern except that all paths are memoized in a HashMap.</p>
 */
public class EngineValueWideningCaster implements EngineValueCaster {

  private Map<EngineValueTuple.TypesTuple, Cast> strategies;

  /**
   * Create a new widening caster with default allowed casts.
   */
  public EngineValueWideningCaster() {
    strategies = new HashMap<>();

    EngineValueFactory factory = new EngineValueFactory(this);

    // Options for boolean
    addCast(
      new EngineValueTuple.TypesTuple("boolean", "int"),
      (x) -> factory.build(x.getAsInt())
    );
    addCast(
      new EngineValueTuple.TypesTuple("boolean", "decimal"),
      (x) -> factory.build(x.getAsDecimal())
    );
    addCast(
      new EngineValueTuple.TypesTuple("boolean", "string"),
      (x) -> factory.build(x.getAsString())
    );

    // Options for int
    addCast(
      new EngineValueTuple.TypesTuple("int", "decimal"),
      (x) -> factory.build(x.getAsDecimal())
    );
    addCast(
      new EngineValueTuple.TypesTuple("int", "String"),
      (x) -> factory.build(x.getAsString())
    );

    // Options for decimal
    addCast(
      new EngineValueTuple.TypesTuple("decimal", "string"),
      (x) -> factory.build(x.getAsString())
    );
  }

  /**
   * Convert a tuple of engine values to a tuple where the two values are compatible.
   *
   * <p>Convert a tuple of engine values which are not compatible to a tuple of engine values which
   * can be used in an operation together. This returns a new tuple with identical units and types
   * or throws an exception if the conversion cannot be made.</p>
   *
   * @param operands tuple of engine values to convert and make compatible.
   * @returns tuple of compatible engine values.
   */
  public EngineValueTuple makeCompatible(EngineValueTuple operands) {
    if (operands.getAreCompatible()) {
      return opearnds;
    }
    
    EngineValueTuple.UnitTuple units = operands.getUnits();
    if (!units.getAreCompatible()) {
        String message = String.format(
          "Cannot cast with different units %s and %s",
          units.getFirst(),
          units.getSecond()
        );
        throw new IllegalArgumentException(message);
    }

    EngineValueTuple.TypesTuple types = operands.getTypes();
    EngineValueTuple.TypesTuple typesReversed = operands.reverse().getTypes();

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

  /**
   * Indicate that a cast is valid.
   *
   * @param types directional tuple of types for which this conversion is allowed.
   * @param strategy the strategy to employ to execute the cast.
   */
  private void addCast(EngineValueTuple.TypesTuple types, Cast strategy) {
    strategies.add(types, strategy);
  }

}
