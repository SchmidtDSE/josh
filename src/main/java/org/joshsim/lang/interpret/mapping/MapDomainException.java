/**
 * Exception thrown when a map operation receives an input value outside the specified domain.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.mapping;

import org.joshsim.engine.value.type.EngineValue;

/**
 * Exception thrown when a map operation receives an input value that falls outside
 * the specified domain bounds, indicating a potential modeling error.
 */
public class MapDomainException extends IllegalArgumentException {

  private final EngineValue operand;
  private final EngineValue domainLow;
  private final EngineValue domainHigh;

  /**
   * Constructs a MapDomainException with details about the domain violation.
   *
   * @param operand The input value that was outside the domain
   * @param domainLow The lower bound of the valid domain
   * @param domainHigh The upper bound of the valid domain
   */
  public MapDomainException(EngineValue operand, EngineValue domainLow, EngineValue domainHigh) {
    super(buildErrorMessage(operand, domainLow, domainHigh));
    this.operand = operand;
    this.domainLow = domainLow;
    this.domainHigh = domainHigh;
  }

  /**
   * Gets the operand value that caused the domain violation.
   *
   * @return The operand value
   */
  public EngineValue getOperand() {
    return operand;
  }

  /**
   * Gets the lower bound of the domain.
   *
   * @return The domain lower bound
   */
  public EngineValue getDomainLow() {
    return domainLow;
  }

  /**
   * Gets the upper bound of the domain.
   *
   * @return The domain upper bound
   */
  public EngineValue getDomainHigh() {
    return domainHigh;
  }

  /**
   * Builds a descriptive error message for the domain violation.
   *
   * @param operand The input value that was outside the domain
   * @param domainLow The lower bound of the valid domain
   * @param domainHigh The upper bound of the valid domain
   * @return A formatted error message
   */
  private static String buildErrorMessage(EngineValue operand, EngineValue domainLow, 
      EngineValue domainHigh) {
    return String.format(
      "Map domain violation: input value %s %s is outside the valid domain [%s %s, %s %s]. "
      + "This often indicates a modeling error where calculated values exceed expected ranges. "
      + "Suggestions: (1) Check your calculations for errors, (2) Verify your domain bounds "
      + "are correct, (3) Add 'unbounded' to the map operation to disable domain validation "
      + "if this is intentional.",
      operand.getAsDecimal(), operand.getUnits(),
      domainLow.getAsDecimal(), domainLow.getUnits(),
      domainHigh.getAsDecimal(), domainHigh.getUnits()
    );
  }
}