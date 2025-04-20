package org.joshsim.compat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.locks.Lock;

import static org.mockito.Mockito.verify;

class JvmLockTest {

  @Test
  void lock_shouldCallInnerLock() {
    // Arrange
    Lock mockLock = Mockito.mock(Lock.class);
    JvmLock jvmLock = new JvmLock(mockLock);

    // Act
    jvmLock.lock();

    // Assert
    verify(mockLock).lock();
  }

  @Test
  void unlock_shouldCallInnerUnlock() {
    // Arrange
    Lock mockLock = Mockito.mock(Lock.class);
    JvmLock jvmLock = new JvmLock(mockLock);

    // Act
    jvmLock.unlock();

    // Assert
    verify(mockLock).unlock();
  }

}