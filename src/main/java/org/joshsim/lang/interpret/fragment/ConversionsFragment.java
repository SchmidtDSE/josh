package org.joshsim.lang.interpret.fragment;

import org.joshsim.engine.value.Conversion;

import java.util.List;


public class ConversionsFragment extends Fragment{

  private final Iterable<Conversion> conversions;

  public ConversionsFragment(Iterable<Conversion> conversions) {
    this.conversions = conversions;
  }

  public Iterable<Conversion> getConversions() {
    return conversions;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.CONVERSIONS;
  }

}
