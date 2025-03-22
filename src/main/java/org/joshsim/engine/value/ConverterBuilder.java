/**
 * Structures to help build converters.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.value;

/**
 * Builder for Converters which can handle multiple conversions.
 */
public interface ConverterBuilder {
  /**
   * Add a conversion rule to the builder.
   *
   * @param conversion the conversion rule to add
   * @return this builder for method chaining
   */
  ConverterBuilder addConversion(Conversion conversion);

  /**
   * Build and returns a Converter based on the added conversions.
   *
   * @return a new Converter instance
   */
  Converter build();
}
