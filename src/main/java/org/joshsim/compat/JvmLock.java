package org.joshsim.compat;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class JvmLock implements CompatibleLock {

  private final Lock inner;

  public JvmLock() {
    inner = new ReentrantLock();
  }

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
