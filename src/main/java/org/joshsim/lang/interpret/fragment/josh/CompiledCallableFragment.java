
/**
 * JoshFragment containing a compiled callable with optional selector.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment.josh;

import java.util.Optional;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.lang.interpret.fragment.FragmentType;

/**
 * JoshFragment representing a compiled callable with an optional selector.
 *
 * <p>This class maintains a compiled callable and optionally a compiled selector
 * that can be used with the callable.</p>
 */
public class CompiledCallableFragment extends JoshFragment {

  private final CompiledCallable callable;
  private final Optional<CompiledSelector> selector;

  /**
   * Creates a new fragment with just a compiled callable.
   *
   * @param callable The compiled callable to store
   */
  public CompiledCallableFragment(CompiledCallable callable) {
    this.callable = callable;
    this.selector = Optional.empty();
  }

  /**
   * Creates a new fragment with both a compiled callable and selector.
   *
   * @param callable The compiled callable to store
   * @param selector The compiled selector to associate with the callable
   */
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
