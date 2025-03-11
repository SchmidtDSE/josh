/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.conversion;

/**
 * Builder interface for creating Converter instances.
 */
public interface ConverterBuilder {
    /**
     * Adds a conversion rule to the builder.
     *
     * @param conversion the conversion rule to add
     * @return this builder for method chaining
     */
    ConverterBuilder addConversion(Conversion conversion);
    
    /**
     * Builds and returns a Converter based on the added conversions.
     *
     * @return a new Converter instance
     */
    Converter build();
}