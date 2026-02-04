/**
 * Utility class for floating-point precision handling in comparisons.
 *
 * <p>Provides centralized epsilon-based comparison methods for floating-point
 * numbers. Standard floating-point arithmetic can produce small rounding errors
 * (e.g., 0.4 + 0.05 = 0.45000000000000001), making exact comparison unreliable.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;


/**
 * Utility class for floating-point precision handling.
 *
 * <p>This class provides constants and methods for comparing floating-point
 * numbers with appropriate tolerance for rounding errors. All numeric scalar
 * types should use these methods for equality comparisons.</p>
 */
public final class PrecisionUtil {

  /**
   * Epsilon value for floating-point equality comparison.
   *
   * <p>This value is used to determine if two floating-point numbers are "equal"
   * within acceptable precision limits. The value 1e-10 provides sufficient
   * precision for most ecological modeling calculations while accommodating
   * typical floating-point rounding errors.</p>
   */
  public static final double EPSILON = 1e-10;

  private PrecisionUtil() {
    // Prevent instantiation
  }

  /**
   * Checks if two double values are approximately equal within epsilon tolerance.
   *
   * <p>Uses relative epsilon for large values and absolute epsilon for small values
   * to provide robust comparison across different magnitudes.</p>
   *
   * @param a the first value to compare.
   * @param b the second value to compare.
   * @return true if the values are equal within tolerance, false otherwise.
   */
  public static boolean areEqual(double a, double b) {
    double diff = Math.abs(a - b);
    double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
    return diff < EPSILON * scale;
  }

  /**
   * Checks if two double values are not approximately equal within epsilon tolerance.
   *
   * <p>This is the logical negation of {@link #areEqual(double, double)}.</p>
   *
   * @param a the first value to compare.
   * @param b the second value to compare.
   * @return true if the values are not equal within tolerance, false otherwise.
   */
  public static boolean areNotEqual(double a, double b) {
    double diff = Math.abs(a - b);
    double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
    return diff >= EPSILON * scale;
  }

}
