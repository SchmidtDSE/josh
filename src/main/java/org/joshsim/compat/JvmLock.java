/**
 * JVM-compatible lock.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A JVM-compatible implementation of CompatibleLock.
 *
 * <p>This implementation provides standard JVM locking mechanisms for thread synchronization using
 * ReentrantLock as the underlying implementation.</p>
 */
public class JvmLock implements CompatibleLock {

  private final Lock inner;

  /**
   * Creates a new JvmLock with a default ReentrantLock implementation.
   */
  public JvmLock() {
    inner = new ReentrantLock();
  }

  /**
   * Creates a new JvmLock with a specified Lock implementation.
   *
   * @param inner The Lock implementation to use
   */
  public JvmLock(Lock inner) {
    this.inner = inner;
  }

  @Override
  public void lock() {
    inner.lock();
  }

  @Override
  public void unlock() {
    inner.unlock();
  }
}
