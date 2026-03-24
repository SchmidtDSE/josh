/**
 * Tests for RunRemoteCommand root-cause error message extraction.
 *
 * <p>Verifies that the getRootCauseMessage helper walks the exception chain
 * to find the first non-null message, which fixes the issue where nested
 * exceptions caused "null" to be printed as the error message.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Tests the private getRootCauseMessage method via reflection to verify
 * it correctly walks the exception chain.
 */
public class RunRemoteCommandErrorMessageTest {

  /**
   * Invokes the private getRootCauseMessage method on RunRemoteCommand via reflection.
   */
  private String invokeGetRootCauseMessage(Exception e) throws Exception {
    RunRemoteCommand command = new RunRemoteCommand();
    Method method = RunRemoteCommand.class.getDeclaredMethod(
        "getRootCauseMessage", Exception.class
    );
    method.setAccessible(true);
    return (String) method.invoke(command, e);
  }

  @Test
  public void testDirectMessage() throws Exception {
    Exception e = new RuntimeException("direct message");
    assertEquals("direct message", invokeGetRootCauseMessage(e));
  }

  @Test
  public void testNullMessageWithCause() throws Exception {
    // Simulates: RuntimeException(null) wrapping IOException("real cause")
    Exception cause = new java.io.IOException("real cause");
    Exception wrapper = new RuntimeException((String) null, cause);

    assertEquals("real cause", invokeGetRootCauseMessage(wrapper));
  }

  @Test
  public void testDeepChainWithNullMessages() throws Exception {
    // Simulates: RuntimeException(null) -> RuntimeException(null) -> IOException("deep root")
    Exception root = new java.io.IOException("deep root cause");
    Exception mid = new RuntimeException((String) null, root);
    Exception outer = new RuntimeException((String) null, mid);

    assertEquals("deep root cause", invokeGetRootCauseMessage(outer));
  }

  @Test
  public void testEmptyMessageSkipped() throws Exception {
    // Empty string should be skipped
    Exception cause = new RuntimeException("actual message");
    Exception wrapper = new RuntimeException("", cause);

    assertEquals("actual message", invokeGetRootCauseMessage(wrapper));
  }

  @Test
  public void testAllNullMessagesFallbackToClassName() throws Exception {
    // When entire chain has null messages, fall back to class name
    Exception inner = new RuntimeException((String) null);
    Exception outer = new RuntimeException((String) null, inner);

    assertEquals("RuntimeException", invokeGetRootCauseMessage(outer));
  }

  @Test
  public void testFirstNonNullMessageReturned() throws Exception {
    // Should return the first non-null message in the chain, not the deepest
    Exception deep = new RuntimeException("deep");
    Exception mid = new RuntimeException("mid", deep);
    Exception outer = new RuntimeException((String) null, mid);

    assertEquals("mid", invokeGetRootCauseMessage(outer));
  }

  @Test
  public void testTypicalWorkerFailureChain() throws Exception {
    // Simulates the real failure chain:
    // RuntimeException(null)
    //   -> ExecutionException(cause) — getMessage() returns "java.lang.RuntimeException: ..."
    //     -> RuntimeException("Error processing wire response stream")
    //       -> ClosedChannelException(null)
    Exception closedChannel = new java.nio.channels.ClosedChannelException();
    Exception wireError = new RuntimeException("Error processing wire response stream",
        closedChannel);
    Exception execException = new java.util.concurrent.ExecutionException(wireError);
    Exception aggregate = new RuntimeException((String) null, execException);

    // ExecutionException.getMessage() includes the cause's toString(), so it's non-null
    String result = invokeGetRootCauseMessage(aggregate);
    assertTrue(result.contains("Error processing wire response stream"),
        "Expected message containing root cause, got: " + result);
  }
}
