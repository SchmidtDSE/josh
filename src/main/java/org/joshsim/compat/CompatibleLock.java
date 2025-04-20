/**
 * Interface for a cross-VM compatability lock.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;


/**
 * Platform-independent interface for thread synchronization locks.
 *
 * <p>This interface provides a common abstraction for locking mechanisms across different platforms
 * (JVM and WebAssembly). It supports basic lock/unlock operations for thread synchronization and
 * is implemented by both {@link JvmLock} for standard JVM environments and {@link EmulatedLock}
 * for WebAssembly environments.</p>
 */
public interface CompatibleLock {

  /**
   * Acquires the lock.
   *
   * <p>If the lock is not available then the current thread becomes disabled for thread scheduling
   * purposes and lies dormant until the lock has been acquired.</p>
   */
  void lock();

  /**
   * Releases the lock.
   *
   * <p>Releases the lock which was previously acquired by the same thread through a call to
   * {@link #lock()}.</p>
   */
  void unlock();

}
