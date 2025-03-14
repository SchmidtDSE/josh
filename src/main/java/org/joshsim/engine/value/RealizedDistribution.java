/**
 * Description of a distribution with finite size.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;


/**
 * Distribution with a finite number of elements.
 *
 * <p>Distribution which contains a finite number of EngineValue elements which can be enumerated in
 * memory as distinct to a VirtualizedDistribution which describes a collection of an indeterminate
 * number of elements for which summary statistics like mean can be derived but individual elements
 * cannot be iterated through.
 * </p>
 */
public abstract class RealizedDistribution extends Distribution {
    /**
     * Create a new distribution, declaring the units of the distribution.
     *
     * @param units
     */
    public RealizedDistribution(String units) {
        super(units);
    }
}