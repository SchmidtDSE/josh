/**
 * Unit tests for WireRewriteUtil utility class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for WireRewriteUtil functionality.
 *
 * <p>Tests wire format generation methods and response rewriting capabilities
 * to ensure proper wire protocol compliance and response manipulation.</p>
 */
public class WireRewriteUtilTest {

  @Test
  public void testFormatProgressResponse() {
    String result = WireRewriteUtil.formatProgressResponse(42);
    assertEquals("[progress 42]\n", result);
  }

  @Test
  public void testFormatProgressResponseWithZero() {
    String result = WireRewriteUtil.formatProgressResponse(0);
    assertEquals("[progress 0]\n", result);
  }

  @Test
  public void testFormatDatumResponse() {
    String result = WireRewriteUtil.formatDatumResponse(5, "simulation data here");
    assertEquals("[5] simulation data here\n", result);
  }

  @Test
  public void testFormatDatumResponseWithZeroReplicate() {
    String result = WireRewriteUtil.formatDatumResponse(0, "test data");
    assertEquals("[0] test data\n", result);
  }

  @Test
  public void testFormatEndResponse() {
    String result = WireRewriteUtil.formatEndResponse(3);
    assertEquals("[end 3]\n", result);
  }

  @Test
  public void testFormatErrorResponse() {
    String result = WireRewriteUtil.formatErrorResponse("Something went wrong");
    assertEquals("[error] Something went wrong\n", result);
  }

  @Test
  public void testRewriteReplicateNumberDatum() {
    WireResponse original = new WireResponse(0, "test data");
    WireResponse rewritten = WireRewriteUtil.rewriteReplicateNumber(original, 5);

    assertEquals(WireResponse.ResponseType.DATUM, rewritten.getType());
    assertEquals(5, rewritten.getReplicateNumber());
    assertEquals("test data", rewritten.getDataLine());
  }

  @Test
  public void testRewriteReplicateNumberEnd() {
    WireResponse original = new WireResponse(WireResponse.ResponseType.END, 0);
    WireResponse rewritten = WireRewriteUtil.rewriteReplicateNumber(original, 7);

    assertEquals(WireResponse.ResponseType.END, rewritten.getType());
    assertEquals(7, rewritten.getReplicateNumber());
  }

  @Test
  public void testRewriteReplicateNumberProgress() {
    WireResponse original = new WireResponse(42);
    WireResponse rewritten = WireRewriteUtil.rewriteReplicateNumber(original, 5);

    // Progress responses don't have replicate numbers - should return original
    assertEquals(original, rewritten);
    assertEquals(WireResponse.ResponseType.PROGRESS, rewritten.getType());
    assertEquals(42, rewritten.getStepCount());
  }

  @Test
  public void testRewriteReplicateNumberError() {
    WireResponse original = new WireResponse("Error message");
    WireResponse rewritten = WireRewriteUtil.rewriteReplicateNumber(original, 5);

    // Error responses don't have replicate numbers - should return original
    assertEquals(original, rewritten);
    assertEquals(WireResponse.ResponseType.ERROR, rewritten.getType());
    assertEquals("Error message", rewritten.getErrorMessage());
  }

  @Test
  public void testRewriteReplicateNumberNullResponse() {
    assertThrows(IllegalArgumentException.class, () -> {
      WireRewriteUtil.rewriteReplicateNumber(null, 5);
    });
  }

  @Test
  public void testRewriteProgressToCumulative() {
    AtomicInteger cumulativeCounter = new AtomicInteger(10);
    WireResponse original = new WireResponse(5);

    WireResponse rewritten =
        WireRewriteUtil.rewriteProgressToCumulative(original, cumulativeCounter);

    assertEquals(WireResponse.ResponseType.PROGRESS, rewritten.getType());
    assertEquals(15, rewritten.getStepCount()); // 10 + 5
    assertEquals(15, cumulativeCounter.get()); // Counter should be updated
  }

  @Test
  public void testRewriteProgressToCumulativeMultipleTimes() {
    AtomicInteger cumulativeCounter = new AtomicInteger(0);

    WireResponse first = new WireResponse(10);
    WireResponse rewritten1 =
        WireRewriteUtil.rewriteProgressToCumulative(first, cumulativeCounter);
    assertEquals(10, rewritten1.getStepCount());
    assertEquals(10, cumulativeCounter.get());

    WireResponse second = new WireResponse(5);
    WireResponse rewritten2 =
        WireRewriteUtil.rewriteProgressToCumulative(second, cumulativeCounter);
    assertEquals(15, rewritten2.getStepCount());
    assertEquals(15, cumulativeCounter.get());
  }

  @Test
  public void testRewriteProgressToCumulativeNullResponse() {
    AtomicInteger cumulativeCounter = new AtomicInteger(0);
    assertThrows(IllegalArgumentException.class, () -> {
      WireRewriteUtil.rewriteProgressToCumulative(null, cumulativeCounter);
    });
  }

  @Test
  public void testRewriteProgressToCumulativeNonProgressResponse() {
    AtomicInteger cumulativeCounter = new AtomicInteger(0);
    WireResponse datumResponse = new WireResponse(5, "data");

    assertThrows(IllegalArgumentException.class, () -> {
      WireRewriteUtil.rewriteProgressToCumulative(datumResponse, cumulativeCounter);
    });
  }

  @Test
  public void testFormatWireResponseProgress() {
    WireResponse response = new WireResponse(42);
    String result = WireRewriteUtil.formatWireResponse(response);
    assertEquals("[progress 42]\n", result);
  }

  @Test
  public void testFormatWireResponseDatum() {
    WireResponse response = new WireResponse(3, "simulation data");
    String result = WireRewriteUtil.formatWireResponse(response);
    assertEquals("[3] simulation data\n", result);
  }

  @Test
  public void testFormatWireResponseEnd() {
    WireResponse response = new WireResponse(WireResponse.ResponseType.END, 5);
    String result = WireRewriteUtil.formatWireResponse(response);
    assertEquals("[end 5]\n", result);
  }

  @Test
  public void testFormatWireResponseError() {
    WireResponse response = new WireResponse("Something failed");
    String result = WireRewriteUtil.formatWireResponse(response);
    assertEquals("[error] Something failed\n", result);
  }

  @Test
  public void testFormatWireResponseNull() {
    assertThrows(IllegalArgumentException.class, () -> {
      WireRewriteUtil.formatWireResponse(null);
    });
  }

  @Test
  public void testWireResponseToWireFormatIntegration() {
    // Test that WireRewriteUtil.formatWireResponse produces same result as
    // response.toWireFormat() + "\n"
    WireResponse response = new WireResponse(2, "test data");

    String viaUtil = WireRewriteUtil.formatWireResponse(response);
    String viaMethod = response.toWireFormat() + "\n";

    assertEquals(viaMethod, viaUtil);
  }
}
