/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.conversion;

/**
 * Interface for converting between different units.
 */
public interface Converter {
    /**
     * Gets a conversion between two unit types.
     *
     * @param oldUnits the source units
     * @param newUnits the destination units
     * @return a Conversion that can convert between the specified units
     * @throws IllegalArgumentException if no conversion exists between the units
     */
    Conversion getConversion(String oldUnits, String newUnits);
}