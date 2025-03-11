/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.core.compiled;

/**
 * Represents a compiled callable that can be evaluated to produce a value.
 * This is the foundation for functions, operators, and expressions in the language.
 */
public interface CompiledCallable {
    /**
     * Evaluates this callable using the provided scope.
     *
     * @param scope the execution scope providing context for evaluation
     * @return the resulting value from evaluating this callable
     */
    EngineValue evaluate(Scope scope);
}