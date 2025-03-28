package org.joshsim.lang.interpret.fragment;

import org.joshsim.engine.value.Conversion;

public class ConversionFragment extends Fragment {

  private final Conversion conversion;

  public ConversionFragment(Conversion conversion) {
    this.conversion = conversion;
  }

  public Conversion getConversion() {
    return conversion;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.CONVERSION;
  }

}
