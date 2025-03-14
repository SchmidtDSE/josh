/**
 * Description of a distribution with undefined size.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;


/**
 * Distribution which, described theoretically, does not have discrete elements.
 *
 * <p>A distribution which is described by parameters to define a certain mathematical shape but
 * which does not have specific individual elements. These distributions can still be sampled and
 * used for generating summary statistics.</p> 
 */
public abstract class VirtualDistribution extends Distribution {
    /**
     * Create a new distribution, declaring the units of the distribution.
     *
     * @param units
     */
    public VirtualDistribution(String units) {
        super(units);
    }
}