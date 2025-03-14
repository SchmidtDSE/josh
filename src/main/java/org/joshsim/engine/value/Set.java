/**
 * Structures describing distributions with only unique values.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;


/**
 * Distribution in which each unique value can only zero or one times.
 */
public abstract class Set extends Distribution {
    /**
     * Create a new distribution, declaring the units of the distribution.
     *
     * @param units
     */
    public Set(String units) {
        super(units);
    }
}
