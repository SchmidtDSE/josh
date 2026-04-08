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
 * {@link #get(Scope)} in milliseconds. Three cases are handled:</p>
 * <ul>
 *   <li>When the wrapped path is {@code "evalDuration"} (bare), returns the last recorded duration
 *       as an {@link EngineValue} with {@link Units#MILLISECONDS} units.</li>
 *   <li>When the wrapped path ends with {@code ".evalDuration"} (e.g.
 *       {@code "height.evalDuration"}), times the resolution of the prefix attribute (e.g.
 *       {@code "height"}) via a separately
 *       constructed {@link RecursiveValueResolver} and returns that elapsed time. This enables
 *       per-entity profiling of dotted attributes without distribution awareness.</li>
 *   <li>For all other paths, delegates to the inner resolver and records the elapsed time.</li>
 * </ul>
 */
public class TimedValueResolver implements ValueResolver {

  private static final String EVAL_DURATION_ATTR = "evalDuration";

  private final ValueSupportFactory valueFactory;
  private final ValueResolver inner;
  private final boolean isEvalDuration;
  private final boolean endsWithEvalDuration;
  private final String evalAttributePrefix;
  private final ValueResolver prefixResolver;
  private long lastDurationMs;

  /**
   * Creates a new TimedValueResolver decorating the given inner resolver.
   *
   * <p>At construction time the path of {@code inner} is examined:
   * <ul>
   *   <li>{@code isEvalDuration} is set when the path is exactly {@code "evalDuration"}.</li>
   *   <li>{@code endsWithEvalDuration} is set when the path ends with {@code ".evalDuration"}
   *       (and is not the bare {@code "evalDuration"} case). In this situation
   *       {@code evalAttributePrefix} holds the prefix (e.g. {@code "height"} for
   *       {@code "height.evalDuration"}) and {@code prefixResolver} holds a
   *       {@link RecursiveValueResolver} for that prefix. Otherwise both are {@code null}.</li>
   * </ul>
   * </p>
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
    String innerPath = inner.getPath();
    this.endsWithEvalDuration = !isEvalDuration
        && innerPath != null
        && innerPath.endsWith("." + EVAL_DURATION_ATTR);
    if (this.endsWithEvalDuration) {
      this.evalAttributePrefix = innerPath.substring(
          0, innerPath.length() - ("." + EVAL_DURATION_ATTR).length()
      );
      this.prefixResolver = new RecursiveValueResolver(valueFactory, this.evalAttributePrefix);
    } else {
      this.evalAttributePrefix = null;
      this.prefixResolver = null;
    }
  }

  /**
   * Gets a value from the target scope, timing the delegation or returning last duration.
   *
   * @param target The scope to search for the value.
   * @return Optional containing the resolved value if found, or the elapsed duration in
   *     milliseconds for evalDuration paths.
   */
  @Override
  public Optional<EngineValue> get(Scope target) {
    return resolveValue(target);
  }

  /**
   * Resolves a value from the target scope, applying timing instrumentation as appropriate.
   *
   * <p>Three branches exist:
   * <ul>
   *   <li>Bare {@code evalDuration}: returns the last recorded duration in milliseconds without
   *       calling the inner resolver.</li>
   *   <li>Dotted {@code attr.evalDuration}: times the resolution of {@code attr} on the target
   *       scope via the pre-built {@code prefixResolver}, discards the resolved value, and returns
   *       the elapsed wall-clock time in milliseconds. The inner resolver is bypassed.</li>
   *   <li>Normal paths: delegates to the inner resolver and records elapsed time. The timing is
   *       always updated even if the inner resolver throws a runtime exception.</li>
   * </ul>
   * </p>
   *
   * @param target The scope to search for the value.
   * @return Optional containing the resolved value if found, or the elapsed duration in
   *     milliseconds for evalDuration paths.
   */
  private Optional<EngineValue> resolveValue(Scope target) {
    if (isEvalDuration) {
      return Optional.of(valueFactory.build(lastDurationMs, Units.MILLISECONDS));
    }
    long start = System.currentTimeMillis();
    if (endsWithEvalDuration) {
      try {
        prefixResolver.get(target);
      } finally {
        lastDurationMs = System.currentTimeMillis() - start;
      }
      return Optional.of(valueFactory.build(lastDurationMs, Units.MILLISECONDS));
    } else {
      try {
        return inner.get(target);
      } finally {
        lastDurationMs = System.currentTimeMillis() - start;
      }
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
