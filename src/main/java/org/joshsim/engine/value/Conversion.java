/**
 * @license BSD-3-Clause
 */
package org.joshsim.engine.value;

import org.joshsim.engine.func.CompiledCallable;


/**
 * Represents a conversion rule between two unit types.
 */
public interface Conversion {
    /**
     * Gets the source units for this conversion.
     *
     * @return the source units as a string
     */
    String getSourceUnits();
    
    /**
     * Gets the destination units for this conversion.
     *
     * @return the destination units as a string
     */
    String getDestinationUnits();
    
    /**
     * Gets the callable that performs the actual conversion.
     *
     * @return a compiled callable that performs the conversion
     */
    CompiledCallable getConversionCallable();
}