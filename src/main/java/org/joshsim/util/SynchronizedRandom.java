/**
 * Thread-safe wrapper for java.util.Random.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import java.util.Random;

/**
 * A synchronized wrapper around java.util.Random to provide thread-safe random number generation.
 *
 * <p>This class extends Random and synchronizes all methods that access or modify the random
 * number generator's internal state. This ensures that when multiple threads access the same
 * Random instance, they receive sequential values from the random stream without race conditions
 * or non-deterministic behavior.</p>
 *
 * <p>This is particularly important for seeded simulations where deterministic behavior is
 * required even when organisms are processed in parallel.</p>
 *
 * <p>Performance note: Synchronization adds overhead. For unseeded simulations where determinism
 * is not required, consider using ThreadLocalRandom instead.</p>
 */
public class SynchronizedRandom extends Random {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new synchronized random number generator with a seed based on system time.
   */
  public SynchronizedRandom() {
    super();
  }

  /**
   * Creates a new synchronized random number generator with the specified seed.
   *
   * @param seed The initial seed value for deterministic random number generation.
   */
  public SynchronizedRandom(long seed) {
    super(seed);
  }

  /**
   * Generates a random double value between 0.0 (inclusive) and 1.0 (exclusive).
   *
   * @return A random double value in the range [0.0, 1.0).
   */
  @Override
  public synchronized double nextDouble() {
    return super.nextDouble();
  }

  /**
   * Generates a random double value between the specified bounds.
   *
   * @param min The lower bound (inclusive).
   * @param max The upper bound (exclusive).
   * @return A random double value in the range [min, max).
   */
  public synchronized double nextDouble(double min, double max) {
    return super.nextDouble(min, max);
  }

  /**
   * Generates a random value from a standard normal (Gaussian) distribution.
   *
   * @return A random double value from a standard normal distribution (mean=0, stdDev=1).
   */
  @Override
  public synchronized double nextGaussian() {
    return super.nextGaussian();
  }

  /**
   * Generates a random value from a normal (Gaussian) distribution with specified parameters.
   *
   * @param mean The mean of the distribution.
   * @param stdDev The standard deviation of the distribution.
   * @return A random double value from the specified normal distribution.
   */
  public synchronized double nextGaussian(double mean, double stdDev) {
    return super.nextGaussian(mean, stdDev);
  }

  /**
   * Generates a random integer value.
   *
   * @return A random integer value.
   */
  @Override
  public synchronized int nextInt() {
    return super.nextInt();
  }

  /**
   * Generates a random integer value between 0 (inclusive) and the specified bound (exclusive).
   *
   * @param bound The upper bound (exclusive). Must be positive.
   * @return A random integer value in the range [0, bound).
   */
  @Override
  public synchronized int nextInt(int bound) {
    return super.nextInt(bound);
  }

  /**
   * Generates a random long value.
   *
   * @return A random long value.
   */
  @Override
  public synchronized long nextLong() {
    return super.nextLong();
  }

  /**
   * Generates a random boolean value.
   *
   * @return A random boolean value.
   */
  @Override
  public synchronized boolean nextBoolean() {
    return super.nextBoolean();
  }

  /**
   * Generates a random float value between 0.0 (inclusive) and 1.0 (exclusive).
   *
   * @return A random float value in the range [0.0, 1.0).
   */
  @Override
  public synchronized float nextFloat() {
    return super.nextFloat();
  }

  /**
   * Fills the specified byte array with random bytes.
   *
   * @param bytes The byte array to fill with random bytes.
   */
  @Override
  public synchronized void nextBytes(byte[] bytes) {
    super.nextBytes(bytes);
  }

  /**
   * Sets the seed of this random number generator.
   *
   * @param seed The new seed value.
   */
  @Override
  public synchronized void setSeed(long seed) {
    super.setSeed(seed);
  }
}
