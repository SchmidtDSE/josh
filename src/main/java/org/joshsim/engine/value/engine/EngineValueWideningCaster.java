/**
 * Strategy implementation through widening only conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.engine;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.engine.value.type.Cast;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.LanguageType;


/**
 * Caster which always widens to achieve type compatibility.
 *
 * <p>Caster which always widens to achieve type compatibility, operating similar to a hierarchy
 * pattern except that all paths are memoized in a HashMap.</p>
 */
public class EngineValueWideningCaster implements EngineValueCaster {

  private final Map<EngineValueTuple.TypesTuple, Cast> strategies;

  /**
   * Create a new widening caster with default allowed casts.
   */
  public EngineValueWideningCaster() {
    strategies = new HashMap<>();

    EngineValueFactory factory = new EngineValueFactory(this);

    // Options for boolean
    addCast(
        "boolean",
        "int",
        x -> factory.build(x.getAsInt(), x.getUnits())
    );
    addCast(
        "boolean",
        "decimal",
        x -> factory.build(x.getAsDecimal(), x.getUnits())
    );
    addCast(
        "boolean",
        "string",
        x -> factory.build(x.getAsString(), x.getUnits())
    );

    // Options for int
    addCast(
        "int",
        "decimal",
        x -> factory.build(x.getAsDecimal(), x.getUnits())
    );
    addCast(
        "int",
        "string",
        x -> factory.build(x.getAsString(), x.getUnits())
    );

    // Options for decimal
    addCast(
        "decimal",
        "string",
        x -> factory.build(x.getAsString(), x.getUnits())
    );
  }

  @Override
  public EngineValueTuple makeCompatible(EngineValueTuple operands, boolean requireSameUnits) {
    EngineValueTuple.TypesTuple types = operands.getTypes();
    if (operands.getAreCompatible() || (!requireSameUnits && types.getAreCompatible())) {
      return operands;
    }

    EngineValueTuple.UnitsTuple units = operands.getUnits();
    if (requireSameUnits && !units.getAreCompatible()) {
      String message = String.format(
          "Cannot cast with different units %s and %s",
          units.getFirst(),
          units.getSecond()
      );
      throw new IllegalArgumentException(message);
    }

    EngineValueTuple.TypesTuple typesReversed = operands.reverse().getTypes();

    if (strategies.containsKey(types)) {
      EngineValue newFirst = operands.getFirst().cast(strategies.get(types));
      return new EngineValueTuple(newFirst, operands.getSecond());
    } else if (strategies.containsKey(typesReversed)) {
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
   * @param firstRoot the starting type of the cast in terms of root type. Will be broadcast if
   *     given to a distribution.
   * @param secondRoot the ending type of the cast in terms of root type. Will be broadcast if
   *     given to a distribution.
   * @param strategy the strategy to employ to execute the cast.
   */
  private void addCast(String firstRoot, String secondRoot, Cast strategy) {
    EngineValueTuple.TypesTuple types = new EngineValueTuple.TypesTuple(
        new LanguageType(firstRoot),
        new LanguageType(secondRoot)
    );
    strategies.put(types, strategy);
  }

}
