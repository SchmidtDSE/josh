/**
 * JoshFragment containing an entity prototype.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.josh;

import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * JoshFragment representing an entity prototype.
 *
 * <p>This class wraps an EntityPrototype that defines the structure and behavior of
 * an entity in the simulation.</p>
 */
public class EntityFragment extends JoshFragment {

  private final EntityPrototype prototype;

  /**
   * Creates a new fragment around an entity prototype.
   *
   * @param prototype The entity prototype to wrap
   */
  public EntityFragment(EntityPrototype prototype) {
    this.prototype = prototype;
  }

  public EntityPrototype getEntity() {
    return prototype;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.ENTITY;
  }

}