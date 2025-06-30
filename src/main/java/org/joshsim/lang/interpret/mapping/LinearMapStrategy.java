/**
 * Strategy for performing a linear map between a given domain and range.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.mapping;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Map strategy which does a linear mapping between the given domain and range.
 */
public class LinearMapStrategy implements MapStrategy {

  private final EngineValueFactory valueFactory;
  private final MapBounds domain;
  private final MapBounds range;
  private final boolean unbounded;

  /**
   * Create a new map strategy that performs a linear mapping between a given domain and range.
   *
   * @param valueFactory The value factory to use in constructing returned and supporting values.
   * @param domain The domain from which values provided will be mapped linearly.
   * @param range The range to which values provided will be mapped linearly.
   */
  public LinearMapStrategy(EngineValueFactory valueFactory, MapBounds domain, MapBounds range) {
    this(valueFactory, domain, range, false);
  }

  /**
   * Create a new map strategy that performs a linear mapping between a given domain and range.
   *
   * @param valueFactory The value factory to use in constructing returned and supporting values.
   * @param domain The domain from which values provided will be mapped linearly.
   * @param range The range to which values provided will be mapped linearly.
   * @param unbounded Whether to disable domain validation (allows values outside domain)
   */
  public LinearMapStrategy(EngineValueFactory valueFactory, MapBounds domain, MapBounds range, 
      boolean unbounded) {
    this.valueFactory = valueFactory;
    this.domain = domain;
    this.range = range;
    this.unbounded = unbounded;
  }

  @Override
  public EngineValue apply(EngineValue operand) {
    EngineValue fromHigh = domain.getHigh();
    EngineValue fromLow = domain.getLow();
    EngineValue toHigh = range.getHigh();
    EngineValue toLow = range.getLow();

    // Validate domain bounds unless unbounded is specified
    if (!unbounded) {
      boolean belowLowerBound = operand.lessThan(fromLow).getAsBoolean();
      boolean aboveUpperBound = operand.greaterThan(fromHigh).getAsBoolean();
      
      if (belowLowerBound || aboveUpperBound) {
        throw new MapDomainException(operand, fromLow, fromHigh);
      }
    }

    EngineValue zero = valueFactory.buildForNumber(0, Units.EMPTY);
    EngineValue fromSpan = fromHigh.subtract(fromLow);
    EngineValue toSpan = toHigh.subtract(toLow);
    EngineValue operandDiff = operand.subtract(fromLow);
    EngineValue percent = operandDiff.add(zero).divide(fromSpan);
    EngineValue result = toSpan.multiply(percent).add(toLow);

    return result;
  }
}
