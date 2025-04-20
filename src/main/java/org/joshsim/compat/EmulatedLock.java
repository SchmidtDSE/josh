
/**
 * A WebAssembly-compatible implementation of CompatibleLock.
 *
 * <p>This implementation provides a simplified locking mechanism for WebAssembly environments
 * where traditional JVM synchronization primitives are not available. The lock operations
 * are currently no-ops since WebAssembly is single-threaded.</p>
 */
package org.joshsim.compat;


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
