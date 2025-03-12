/**
 * @license BSD-3-Clause
 */
package org.joshsim.engine.func;


/**
 * Represents a compiled selector that can evaluate to a boolean value.
 * Typically used for conditional expressions or filters.
 */
public interface CompiledSelector {
    /**
     * Evaluates this selector using the provided scope.
     *
     * @param scope the execution scope providing context for evaluation
     * @return true if the selector condition is met, false otherwise
     */
    boolean evaluate(Scope scope);
}