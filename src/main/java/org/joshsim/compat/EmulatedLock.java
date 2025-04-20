/**
 * WASM-compatible lock.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;


/**
 * A WebAssembly-compatible implementation of CompatibleLock.
 *
 * <p>This implementation provides a simplified locking mechanism for WebAssembly environments
 * where traditional JVM synchronization primitives are not available. The lock operations
 * are currently no-ops since WebAssembly is single-threaded.</p>
 */
public class EmulatedLock implements CompatibleLock {

  @Override
  public void lock() {
    // No-op in WebAssembly environment (single-threaded)
  }

  @Override
  public void unlock() {
    // No-op in WebAssembly environment (single-threaded)
  }

}
