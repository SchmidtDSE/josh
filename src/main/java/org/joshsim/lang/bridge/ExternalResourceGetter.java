/**
 * Description of utilities to get external resources.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import org.joshsim.precompute.DataGridLayer;


/**
 * Description of strategy for loading resources from outside the simulation.
 *
 * <p>Description of strategy for accessing data or other resources that, while outside the
 * simulation may be used as inputs into the simulation through time.</p>
 */
public interface ExternalResourceGetter {

  /**
   * Load an external resource.
   *
   * @param name The name of the resource to be loaded.
   * @return The resource data.
   */
  DataGridLayer getResource(String name);

}
