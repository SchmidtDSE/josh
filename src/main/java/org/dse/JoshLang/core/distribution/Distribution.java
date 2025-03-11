/**
 @license BSD-3-Clause
 */
package org.dse.JoshLang.core.distribution;

import org.dse.JoshLang.core.value.EngineValue;
import org.dse.JoshLang.core.value.Scalar;

import java.util.Optional;

/**
 * Represents a distribution of values.
 */
public interface Distribution extends EngineValue {
    /**
     * Samples a single value from this distribution.
     *
     * @return a scalar value sampled from this distribution
     */
    Scalar sample();
    
    /**
     * Gets the size of the distribution if known.
     *
     * @return the number of elements in the distribution, or empty if virtualized
     */
    Optional<Integer> getSize();
    
    /**
     * Gets a specified number of content values from the distribution.
     *
     * @param count number of values to retrieve
     * @param withReplacement whether to sample with replacement
     * @return an iterable of engine values from the distribution
     */
    Iterable<EngineValue> getContents(int count, boolean withReplacement);
}





