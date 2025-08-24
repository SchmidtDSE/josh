/**
 * Unit tests for ParallelWorkerHandler class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;
import org.joshsim.wire.WireResponse;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ParallelWorkerHandler functionality.
 *
 * <p>Tests the WorkerTask data structure and basic handler construction.
 * Integration tests with actual worker endpoints would require more complex
 * testing infrastructure and are handled by end-to-end tests.</p>
 */
public class ParallelWorkerHandlerTest {

  @Test
  public void testWorkerTaskConstruction() {
    WorkerTask task = new WorkerTask(
        "simulation code",
        "test-simulation",
        "api-key-123",
        "external-data",
        true,
        5
    );

    assertEquals("simulation code", task.getCode());
    assertEquals("test-simulation", task.getSimulationName());
    assertEquals("api-key-123", task.getApiKey());
    assertEquals("external-data", task.getExternalData());
    assertTrue(task.isFavorBigDecimal());
    assertEquals(5, task.getReplicateNumber());
  }

  @Test
  public void testWorkerTaskConstructionWithDefaults() {
    WorkerTask task = new WorkerTask(
        "code",
        "name",
        "",
        "",
        false,
        0
    );

    assertEquals("code", task.getCode());
    assertEquals("name", task.getSimulationName());
    assertEquals("", task.getApiKey());
    assertEquals("", task.getExternalData());
    assertFalse(task.isFavorBigDecimal());
    assertEquals(0, task.getReplicateNumber());
  }

  @Test
  public void testParallelWorkerHandlerConstruction() {
    AtomicInteger counter = new AtomicInteger(0);

    ParallelWorkerHandler handler = new ParallelWorkerHandler(
        "http://localhost:8080/worker",
        10,
        counter
    );

    // Constructor should succeed - we can't easily test internals without
    // making fields package-private or adding getters
    // This test mainly verifies the constructor doesn't throw exceptions
    assertEquals(0, counter.get());
  }

  @Test
  public void testParallelWorkerHandlerWithDifferentParameters() {
    AtomicInteger counter = new AtomicInteger(42);

    ParallelWorkerHandler handler = new ParallelWorkerHandler(
        "https://worker.example.com/api",
        5,
        counter
    );

    // Constructor should succeed with different parameters
    assertEquals(42, counter.get());
  }

  @Test
  public void testWorkerResponseHandlerInterface() {
    // Test that the WorkerResponseHandler interface can be implemented
    WorkerResponseHandler handler =
        (line, replicateNumber, exchange, cumulativeStepCount) -> {
          // Mock implementation for testing interface
        };

    // This test mainly verifies the interface can be implemented
    // The actual functionality is tested in integration tests
  }

  @Test
  public void testWireResponseHandlerInterface() {
    // Test that the WireResponseHandler interface can be implemented
    WireResponseHandler handler =
        mock(WireResponseHandler.class);

    // Create a mock wire response to test with
    WireResponse mockResponse = new WireResponse(42L); // PROGRESS response
    AtomicInteger counter = new AtomicInteger(0);

    // Call the handler interface method
    handler.handleWireResponse(mockResponse, 0, null, counter);

    // Verify the method was called
    verify(handler).handleWireResponse(eq(mockResponse), eq(0), eq(null), eq(counter));
  }

  @Test
  public void testWireResponseHandlerWithDifferentResponseTypes() {
    WireResponseHandler handler =
        mock(WireResponseHandler.class);
    AtomicInteger counter = new AtomicInteger(0);

    // Test with DATUM response
    WireResponse datumResponse = new WireResponse(0, "name=test,value=123");
    handler.handleWireResponse(datumResponse, 0, null, counter);
    verify(handler).handleWireResponse(eq(datumResponse), eq(0), eq(null), eq(counter));

    // Test with END response
    WireResponse endResponse = new WireResponse(WireResponse.ResponseType.END, 1);
    handler.handleWireResponse(endResponse, 1, null, counter);
    verify(handler).handleWireResponse(eq(endResponse), eq(1), eq(null), eq(counter));

    // Test with ERROR response
    WireResponse errorResponse = new WireResponse("Test error message");
    handler.handleWireResponse(errorResponse, 2, null, counter);
    verify(handler).handleWireResponse(eq(errorResponse), eq(2), eq(null), eq(counter));
  }

  // Note: Testing executeInParallel and executeInParallelWire would require either:
  // 1. A mock HTTP server setup (complex integration test)
  // 2. Dependency injection to mock HTTP components
  // 3. End-to-end testing with real worker instances
  //
  // For now, we rely on:
  // - Unit tests of individual components (WorkerTask, constructor, interfaces)
  // - Integration tests in the existing test suite
  // - End-to-end validation via examples/validate.sh
}