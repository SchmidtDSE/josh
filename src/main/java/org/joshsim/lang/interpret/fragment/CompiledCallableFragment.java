package org.joshsim.lang.interpret.fragment;

import java.util.Optional;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;

public class CompiledCallableFragment extends Fragment {

  private final CompiledCallable callable;
  private final Optional<CompiledSelector> selector;

  public CompiledCallableFragment(CompiledCallable callable) {
    this.callable = callable;
    this.selector = Optional.empty();
  }

  public CompiledCallableFragment(CompiledCallable callable, CompiledSelector selector) {
    this.callable = callable;
    this.selector = Optional.of(selector);
  }

  @Override
  public CompiledCallable getCompiledCallable() {
    return callable;
  }

  @Override
  public Optional<CompiledSelector> getCompiledSelector() {
    return selector;
  }

  @Override
  public FragmentType getFragmentType() {
    return FragmentType.COMPILED_CALLABLE;
  }

}
