/**
 * Structures to help find values within a scope or nested scopes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Optional;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Decorator implementation of ValueResolver that times each non-evalDuration resolution.
 *
 * <p>Wraps an inner {@link ValueResolver} and measures the wall-clock duration of each call to
 * {@link #get(Scope)} in milliseconds. When the wrapped path is {@code "evalDuration"}, the
 * resolver intercepts the call and returns the last recorded duration as an {@link EngineValue}
 * with {@link Units#MILLISECONDS} units instead of delegating to the inner resolver. This enables
 * a synthetic {@code attr.evalDuration} attribute in the Josh language without adding overhead to
 * the hot path when timing is not requested.</p>
 */
public class TimedValueResolver implements ValueResolver {

  private static final String EVAL_DURATION_ATTR = "evalDuration";

  private final ValueSupportFactory valueFactory;
  private final ValueResolver inner;
  private final boolean isEvalDuration;
  private long lastDurationMs;

  /**
   * Creates a new TimedValueResolver decorating the given inner resolver.
   *
   * @param valueFactory The factory used to construct the millisecond EngineValue returned
   *     for evalDuration queries.
   * @param inner The inner resolver whose get calls will be timed and whose path will be delegated.
   */
  public TimedValueResolver(ValueSupportFactory valueFactory, ValueResolver inner) {
    this.valueFactory = valueFactory;
    this.inner = inner;
    this.isEvalDuration = EVAL_DURATION_ATTR.equals(inner.getPath());
    this.lastDurationMs = 0L;
  }

  /**
   * Gets a value from the target scope, timing the delegation or returning last duration.
   *
   * <p>If this resolver's path is {@code "evalDuration"}, returns the last recorded duration
   * immediately as a millisecond EngineValue without calling the inner resolver. Otherwise,
   * delegates to the inner resolver, recording the elapsed wall-clock time in milliseconds.
   * The timing is always updated even if the inner resolver throws a runtime exception.</p>
   *
   * @param target The scope to search for the value.
   * @return Optional containing the resolved value if found, or the last duration in milliseconds
   *     if the path is evalDuration.
   */
  @Override
  public Optional<EngineValue> get(Scope target) {
    if (isEvalDuration) {
      return Optional.of(valueFactory.build(lastDurationMs, Units.MILLISECONDS));
    }
    long start = System.currentTimeMillis();
    try {
      return inner.get(target);
    } finally {
      lastDurationMs = System.currentTimeMillis() - start;
    }
  }

  /**
   * Gets the path being resolved, delegating to the inner resolver.
   *
   * @return the dot-separated path string from the inner resolver.
   */
  @Override
  public String getPath() {
    return inner.getPath();
  }

  @Override
  public String toString() {
    return String.format("TimedValueResolver(%s)", inner.getPath());
  }
}
