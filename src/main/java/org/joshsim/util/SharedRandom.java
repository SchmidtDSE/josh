/**
 * Thread-local shared random number generator for reproducible simulations.
 *
 * <p>Provides a centralized random number generator that can be seeded for
 * reproducible simulation results. Uses ThreadLocal storage to ensure thread
 * safety while maintaining consistent random sequences within each thread.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import java.util.Optional;
import java.util.Random;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.RandomGeneratorFactory;


/**
 * Utility class for managing a shared Random instance across simulation components.
 *
 * <p>This class provides thread-local storage for a Random instance that can be
 * seeded at simulation start for reproducible results. All simulation components
 * that need random numbers should use this class instead of creating their own
 * Random instances.</p>
 *
 * <p>Usage pattern:
 * <pre>
 * // At simulation start
 * SharedRandom.initialize(Optional.of(42L));
 *
 * // In simulation code
 * double value = SharedRandom.get().nextDouble();
 *
 * // At simulation end (cleanup)
 * SharedRandom.clear();
 * </pre>
 * </p>
 */
public final class SharedRandom {

  private static final ThreadLocal<Random> THREAD_LOCAL_RANDOM = new ThreadLocal<>();
  private static final ThreadLocal<Optional<Long>> THREAD_LOCAL_SEED = new ThreadLocal<>();

  private SharedRandom() {
    // Utility class - prevent instantiation
  }

  /**
   * Initialize the shared random number generator for the current thread.
   *
   * <p>If a seed is provided, creates a seeded Random for reproducible results.
   * If no seed is provided, creates an unseeded Random with default behavior.</p>
   *
   * @param seed Optional seed value. If present, the Random will be seeded for reproducibility.
   */
  public static void initialize(Optional<Long> seed) {
    if (seed.isPresent()) {
      THREAD_LOCAL_RANDOM.set(new Random(seed.get()));
      THREAD_LOCAL_SEED.set(seed);
    } else {
      THREAD_LOCAL_RANDOM.set(new Random());
      THREAD_LOCAL_SEED.set(Optional.empty());
    }
  }

  /**
   * Initialize the shared random number generator with a specific seed.
   *
   * @param seed The seed value for reproducible random sequences.
   */
  public static void initialize(long seed) {
    initialize(Optional.of(seed));
  }

  /**
   * Get the shared Random instance for the current thread.
   *
   * <p>If initialize() has not been called, creates a default unseeded Random.
   * This ensures backward compatibility with code that doesn't explicitly
   * initialize the shared random.</p>
   *
   * @return The shared Random instance for the current thread.
   */
  public static Random get() {
    Random random = THREAD_LOCAL_RANDOM.get();
    if (random == null) {
      // Lazy initialization with default unseeded Random
      random = new Random();
      THREAD_LOCAL_RANDOM.set(random);
      THREAD_LOCAL_SEED.set(Optional.empty());
    }
    return random;
  }

  /**
   * Check if the shared random has been initialized with a seed.
   *
   * @return true if a seed was provided during initialization, false otherwise.
   */
  public static boolean isSeeded() {
    Optional<Long> seed = THREAD_LOCAL_SEED.get();
    return seed != null && seed.isPresent();
  }

  /**
   * Get the seed used for initialization, if any.
   *
   * @return Optional containing the seed if one was provided, empty otherwise.
   */
  public static Optional<Long> getSeed() {
    Optional<Long> seed = THREAD_LOCAL_SEED.get();
    return seed != null ? seed : Optional.empty();
  }

  /**
   * Clear the shared random instance for the current thread.
   *
   * <p>Should be called at the end of simulation runs to clean up thread-local
   * storage and prevent memory leaks.</p>
   */
  public static void clear() {
    THREAD_LOCAL_RANDOM.remove();
    THREAD_LOCAL_SEED.remove();
  }

  /**
   * Generate a random double using the shared Random.
   *
   * <p>Convenience method equivalent to get().nextDouble().</p>
   *
   * @return A random double between 0.0 (inclusive) and 1.0 (exclusive).
   */
  public static double nextDouble() {
    return get().nextDouble();
  }

  /**
   * Generate a random double in a range using the shared Random.
   *
   * @param min The minimum value (inclusive).
   * @param max The maximum value (exclusive).
   * @return A random double between min and max.
   */
  public static double nextDouble(double min, double max) {
    return get().nextDouble(min, max);
  }

  /**
   * Generate a random integer using the shared Random.
   *
   * @param bound The upper bound (exclusive).
   * @return A random integer between 0 (inclusive) and bound (exclusive).
   */
  public static int nextInt(int bound) {
    return get().nextInt(bound);
  }

  /**
   * Generate a Gaussian-distributed random double using the shared Random.
   *
   * @return A random double from a Gaussian distribution with mean 0 and std 1.
   */
  public static double nextGaussian() {
    return get().nextGaussian();
  }

  /**
   * Generate a Gaussian-distributed random double with specified parameters.
   *
   * @param mean The mean of the distribution.
   * @param stddev The standard deviation of the distribution.
   * @return A random double from the specified Gaussian distribution.
   */
  public static double nextGaussian(double mean, double stddev) {
    return get().nextGaussian(mean, stddev);
  }

  /**
   * Generate a binomial-distributed random integer.
   *
   * <p>Draws a single sample from a Binomial(n, p) distribution using Apache Commons Math,
   * returning the number of successes in n independent Bernoulli(p) trials.</p>
   *
   * @param n The number of trials (must be non-negative).
   * @param p The probability of success on each trial (must be in [0, 1]).
   * @return The number of successes, an integer in [0, n].
   * @throws IllegalArgumentException if n is negative or p is not in [0, 1].
   */
  public static int nextBinomial(int n, double p) {
    if (n < 0) {
      throw new IllegalArgumentException("n must be non-negative, got: " + n);
    }
    if (p < 0.0 || p > 1.0) {
      throw new IllegalArgumentException("p must be in [0, 1], got: " + p);
    }
    BinomialDistribution dist = new BinomialDistribution(
        RandomGeneratorFactory.createRandomGenerator(get()), n, p);
    return dist.sample();
  }

}
