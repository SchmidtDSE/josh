package org.joshsim.engine.func;


public class CompiledSelectorFromCallable implements CompiledSelector {

  private final CompiledCallable inner;

  public CompiledSelectorFromCallable(CompiledCallable inner) {
    this.inner = inner;
  }

  @Override
  public boolean evaluate(Scope scope) {
    return inner.evaluate(scope).getAsBoolean();
  }
}
