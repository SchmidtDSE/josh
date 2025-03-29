package org.joshsim.lang.interpret.fragment;

import java.util.List;
import org.joshsim.engine.value.Conversion;


public class ConversionsFragment extends Fragment {

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
