package org.joshsim.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SimulationExecutionExceptionTest {

  @Test
  void constructor_shouldSetUserMessageAndOriginalCause() {
    // Arrange
    String userMessage = "User-friendly error message";
    RuntimeException originalCause = new RuntimeException("Internal error details");

    // Act
    SimulationExecutionException exception = new SimulationExecutionException(userMessage, 
        originalCause);

    // Assert
    assertEquals(userMessage, exception.getUserMessage());
    assertEquals(userMessage, exception.getMessage());
    assertSame(originalCause, exception.getOriginalCause());
    assertSame(originalCause, exception.getCause());
  }

  @Test
  void constructor_shouldHandleNullUserMessage() {
    // Arrange
    String userMessage = null;
    RuntimeException originalCause = new RuntimeException("Internal error");

    // Act
    SimulationExecutionException exception = new SimulationExecutionException(userMessage, 
        originalCause);

    // Assert
    assertEquals(null, exception.getUserMessage());
    assertEquals(null, exception.getMessage());
    assertSame(originalCause, exception.getOriginalCause());
  }

  @Test
  void constructor_shouldHandleNullOriginalCause() {
    // Arrange
    String userMessage = "Error occurred";
    Throwable originalCause = null;

    // Act
    SimulationExecutionException exception = new SimulationExecutionException(userMessage, 
        originalCause);

    // Assert
    assertEquals(userMessage, exception.getUserMessage());
    assertEquals(userMessage, exception.getMessage());
    assertEquals(null, exception.getOriginalCause());
    assertEquals(null, exception.getCause());
  }

  @Test
  void constructor_shouldHandleBothNull() {
    // Arrange
    String userMessage = null;
    Throwable originalCause = null;

    // Act
    SimulationExecutionException exception = new SimulationExecutionException(userMessage, 
        originalCause);

    // Assert
    assertEquals(null, exception.getUserMessage());
    assertEquals(null, exception.getMessage());
    assertEquals(null, exception.getOriginalCause());
    assertEquals(null, exception.getCause());
  }
}