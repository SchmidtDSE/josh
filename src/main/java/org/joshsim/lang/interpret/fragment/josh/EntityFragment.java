/**
 * JoshFragment containing an entity prototype.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.josh;

import org.joshsim.engine.entity.prototype.EntityOverwriteBehavior;
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
  private final EntityOverwriteBehavior overwriteBehavior;

  /**
   * Creates a new fragment around an entity prototype.
   *
   * @param prototype The entity prototype to wrap
   * @param overwriteBehavior How this declaration relates to a prior same-named entity, per the
   *     stanza's opening keyword ({@code start} / {@code replace} / {@code update}).
   */
  public EntityFragment(EntityPrototype prototype, EntityOverwriteBehavior overwriteBehavior) {
    this.prototype = prototype;
    this.overwriteBehavior = overwriteBehavior;
  }

  public EntityPrototype getEntity() {
    return prototype;
  }

  @Override
  public EntityOverwriteBehavior getOverwriteBehavior() {
    return overwriteBehavior;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.ENTITY;
  }

}
