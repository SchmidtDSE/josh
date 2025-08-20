/**
 * Unit tests for ProgressCalculator utility.
 *
 * <p>This test class validates the functionality of calculating progress percentages,
 * filtering progress updates, and formatting progress messages for simulation execution.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Tests for ProgressCalculator utility class.
 *
 * <p>This test suite validates progress calculation, percentage computations, intelligent
 * filtering, and message formatting for both single and multi-replicate scenarios.</p>
 */
public class ProgressCalculatorTest {

  /**
   * Tests progress calculation for a single replicate simulation.
   */
  @Test
  public void testProgressCalculationSingleReplicate() {
    ProgressCalculator calc = new ProgressCalculator(100, 1);
    
    // Test initial progress report (should always report first update)
    ProgressUpdate update1 = calc.updateStep(0);
    assertTrue(update1.shouldReport());
    assertEquals(0.0, update1.getPercentage(), 0.1);
    assertEquals("Progress: 0.0% (step 0/100)", update1.getMessage());
    
    // Test 25% progress
    ProgressUpdate update25 = calc.updateStep(25);
    assertTrue(update25.shouldReport()); // Should report due to 5% threshold
    assertEquals(25.0, update25.getPercentage(), 0.1);
    assertEquals("Progress: 25.0% (step 25/100)", update25.getMessage());
    
    // Test incremental step (should not report due to filtering)
    ProgressUpdate update26 = calc.updateStep(26);
    assertFalse(update26.shouldReport()); // Too soon for next report
    assertEquals(26.0, update26.getPercentage(), 0.1);
    
    // Test 50% progress (should report due to 5% threshold)
    ProgressUpdate update50 = calc.updateStep(50);
    assertTrue(update50.shouldReport());
    assertEquals(50.0, update50.getPercentage(), 0.1);
    assertEquals("Progress: 50.0% (step 50/100)", update50.getMessage());
  }

  /**
   * Tests progress calculation for multiple replicate simulation.
   */
  @Test
  public void testProgressCalculationMultipleReplicates() {
    ProgressCalculator calc = new ProgressCalculator(20, 5); // 20 steps per replicate, 5 replicates
    
    // Test progress in first replicate
    ProgressUpdate update1 = calc.updateStep(0);
    assertTrue(update1.shouldReport());
    assertEquals(0.0, update1.getPercentage(), 0.1);
    assertEquals("Progress: 0.0% (step 0/20, replicate 1/5)", update1.getMessage());
    
    // Test 25% through first replicate (5% total progress)
    ProgressUpdate update5 = calc.updateStep(5);
    assertTrue(update5.shouldReport());
    assertEquals(5.0, update5.getPercentage(), 0.1);
    assertEquals("Progress: 5.0% (step 5/20, replicate 1/5)", update5.getMessage());
    
    // Test completion of first replicate
    ProgressUpdate endUpdate1 = calc.updateReplicateCompleted(1);
    assertTrue(endUpdate1.shouldReport());
    assertEquals(20.0, endUpdate1.getPercentage(), 0.1);
    assertEquals("Replicate 1/5 completed", endUpdate1.getMessage());
    
    // Test progress in second replicate (should now be replicate 2)
    ProgressUpdate update21 = calc.updateStep(10);
    assertTrue(update21.shouldReport()); // First update of new replicate should report
    assertEquals(30.0, update21.getPercentage(), 0.1); // 20 + 10 = 30 out of 100 total
    assertEquals("Progress: 30.0% (step 10/20, replicate 2/5)", update21.getMessage());
  }

  /**
   * Tests replicate completion tracking and messaging.
   */
  @Test
  public void testReplicateCompletion() {
    ProgressCalculator calc = new ProgressCalculator(10, 3);
    
    // Complete first replicate
    ProgressUpdate end1 = calc.updateReplicateCompleted(1);
    assertTrue(end1.shouldReport());
    assertEquals(33.3, end1.getPercentage(), 0.1);
    assertTrue(end1.getMessage().contains("Replicate 1/3 completed"));
    
    // Complete second replicate
    ProgressUpdate end2 = calc.updateReplicateCompleted(2);
    assertTrue(end2.shouldReport());
    assertEquals(66.7, end2.getPercentage(), 0.1);
    assertTrue(end2.getMessage().contains("Replicate 2/3 completed"));
    
    // Complete final replicate
    ProgressUpdate end3 = calc.updateReplicateCompleted(3);
    assertTrue(end3.shouldReport());
    assertEquals(100.0, end3.getPercentage(), 0.1);
    assertTrue(end3.getMessage().contains("Replicate 3/3 completed"));
  }

  /**
   * Tests single replicate completion messaging.
   */
  @Test
  public void testSingleReplicateCompletion() {
    ProgressCalculator calc = new ProgressCalculator(50, 1);
    
    ProgressUpdate endUpdate = calc.updateReplicateCompleted(1);
    assertTrue(endUpdate.shouldReport());
    assertEquals(100.0, endUpdate.getPercentage(), 0.1);
    assertEquals("Replicate 1/1 completed", endUpdate.getMessage());
  }

  /**
   * Tests intelligent filtering of progress updates.
   */
  @Test
  public void testProgressFiltering() {
    ProgressCalculator calc = new ProgressCalculator(1000, 1); // Large number of steps
    
    // First update should always report
    ProgressUpdate update1 = calc.updateStep(0);
    assertTrue(update1.shouldReport());
    
    // Small incremental updates should be filtered
    ProgressUpdate update2 = calc.updateStep(1);
    assertFalse(update2.shouldReport()); // Too small percentage change
    
    ProgressUpdate update3 = calc.updateStep(5);
    assertFalse(update3.shouldReport()); // Still too small
    
    // Large percentage jump should report
    ProgressUpdate update50 = calc.updateStep(50);
    assertTrue(update50.shouldReport()); // 5% threshold reached
    
    // Subsequent small updates should be filtered again
    ProgressUpdate update51 = calc.updateStep(51);
    assertFalse(update51.shouldReport());
    
    // Step interval threshold test
    ProgressUpdate update61 = calc.updateStep(61);
    assertTrue(update61.shouldReport()); // 10 step interval + 1% change
  }

  /**
   * Tests edge cases and boundary conditions.
   */
  @Test
  public void testEdgeCases() {
    // Test with single step
    ProgressCalculator calc1 = new ProgressCalculator(1, 1);
    ProgressUpdate update1 = calc1.updateStep(0);
    assertTrue(update1.shouldReport());
    assertEquals(0.0, update1.getPercentage(), 0.1);
    
    ProgressUpdate end1 = calc1.updateReplicateCompleted(1);
    assertTrue(end1.shouldReport());
    assertEquals(100.0, end1.getPercentage(), 0.1);
    
    // Test with very large numbers
    ProgressCalculator calc2 = new ProgressCalculator(1000000, 1);
    ProgressUpdate update2 = calc2.updateStep(500000);
    assertTrue(update2.shouldReport()); // Should report 50%
    assertEquals(50.0, update2.getPercentage(), 0.1);
  }

  /**
   * Tests invalid constructor parameters.
   */
  @Test
  public void testInvalidConstructorParameters() {
    assertThrows(IllegalArgumentException.class, () -> {
      new ProgressCalculator(0, 1); // Zero steps
    });
    
    assertThrows(IllegalArgumentException.class, () -> {
      new ProgressCalculator(-5, 1); // Negative steps
    });
    
    assertThrows(IllegalArgumentException.class, () -> {
      new ProgressCalculator(10, 0); // Zero replicates
    });
    
    assertThrows(IllegalArgumentException.class, () -> {
      new ProgressCalculator(10, -2); // Negative replicates
    });
  }

  /**
   * Tests ProgressUpdate toString method.
   */
  @Test
  public void testProgressUpdateToString() {
    ProgressCalculator calc = new ProgressCalculator(100, 1);
    ProgressUpdate update = calc.updateStep(25);
    
    String result = update.toString();
    assertNotNull(result);
    assertTrue(result.contains("shouldReport=true"));
    assertTrue(result.contains("percentage=25.0"));
    assertTrue(result.contains("Progress: 25.0% (step 25/100)"));
  }

  /**
   * Tests progress calculation accuracy with fractional percentages.
   */
  @Test
  public void testFractionalPercentages() {
    ProgressCalculator calc = new ProgressCalculator(7, 3); // Creates fractional percentages
    
    // Test step 1 of 7 in first replicate
    ProgressUpdate update1 = calc.updateStep(1);
    assertTrue(update1.shouldReport());
    assertEquals(4.8, update1.getPercentage(), 0.1); // 1/21 * 100 ≈ 4.76%
    
    // Test step 3 of 7 in first replicate  
    ProgressUpdate update3 = calc.updateStep(3);
    assertTrue(update3.shouldReport()); // Should report due to 5% threshold
    assertEquals(14.3, update3.getPercentage(), 0.1); // 3/21 * 100 ≈ 14.29%
  }

  /**
   * Tests message formatting consistency.
   */
  @Test
  public void testMessageFormattingConsistency() {
    // Single replicate formatting
    ProgressCalculator calc1 = new ProgressCalculator(50, 1);
    ProgressUpdate update1 = calc1.updateStep(25);
    assertEquals("Progress: 50.0% (step 25/50)", update1.getMessage());
    
    // Multi-replicate formatting
    ProgressCalculator calc2 = new ProgressCalculator(50, 3);
    ProgressUpdate update2 = calc2.updateStep(25);
    assertEquals("Progress: 16.7% (step 25/50, replicate 1/3)", update2.getMessage());
    
    // Completion formatting
    ProgressUpdate end2 = calc2.updateReplicateCompleted(1);
    assertEquals("Replicate 1/3 completed", end2.getMessage());
  }

  /**
   * Tests progress tracking across multiple replicate transitions.
   */
  @Test
  public void testMultipleReplicateTransitions() {
    ProgressCalculator calc = new ProgressCalculator(4, 4); // 4 steps, 4 replicates = 16 total
    
    // Progress through first replicate
    ProgressUpdate step2 = calc.updateStep(2);
    assertEquals(12.5, step2.getPercentage(), 0.1); // 2/16 * 100
    
    // Complete first replicate
    ProgressUpdate end1 = calc.updateReplicateCompleted(1);
    assertEquals(25.0, end1.getPercentage(), 0.1); // 4/16 * 100
    
    // Progress in second replicate
    ProgressUpdate step6 = calc.updateStep(2);
    assertEquals(37.5, step6.getPercentage(), 0.1); // (4+2)/16 * 100
    
    // Complete second replicate
    ProgressUpdate end2 = calc.updateReplicateCompleted(2);
    assertEquals(50.0, end2.getPercentage(), 0.1); // 8/16 * 100
    
    // Complete third replicate
    ProgressUpdate end3 = calc.updateReplicateCompleted(3);
    assertEquals(75.0, end3.getPercentage(), 0.1); // 12/16 * 100
    
    // Complete final replicate
    ProgressUpdate end4 = calc.updateReplicateCompleted(4);
    assertEquals(100.0, end4.getPercentage(), 0.1); // 16/16 * 100
  }
}