package org.joshsim.lang.interpret.fragment;

import org.joshsim.engine.entity.prototype.EntityPrototype;


public class EntityFragment extends Fragment {

  private final EntityPrototype prototype;

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
