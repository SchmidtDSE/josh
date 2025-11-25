/**
 * Tests for conditional context tracking in the dependency analysis system.
 *
 * <p>This test class verifies that conditional expressions (if/elif/else branches)
 * are properly captured and tracked throughout the dependency analysis pipeline,
 * from DependencyTracker through DependencyGraph to JSON export.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for conditional context tracking.
 *
 * <p>Tests verify that conditional information (condition expressions, assigned values,
 * and else branch flags) are properly captured at each level of the dependency tracking
 * system.</p>
 */
public class ConditionalTrackingTest {

  /**
   * Clean up dependency tracking after each test.
   */
  @AfterEach
  void cleanup() {
    DependencyTracker.disable();
  }

  /**
   * Tests that recordConditionalSource captures condition fields correctly.
   */
  @Test
  void testRecordConditionalSource() {
    // Test that recordConditionalSource captures condition fields
    DependencyTracker.enable();

    DependencyTracker.recordConditionalSource(
        "Tree.state.step",
        10,
        "state.step:if(age >= 10) = \"Sapling\"",
        "age >= 10",
        "\"Sapling\"",
        false
    );

    var nodeSources = DependencyTracker.getNodeSources();
    assertNotNull(nodeSources.get("Tree.state.step"));
    assertEquals(1, nodeSources.get("Tree.state.step").size());

    var sourceInfo = nodeSources.get("Tree.state.step").get(0);
    assertEquals(10, sourceInfo.sourceLine);
    assertEquals("state.step:if(age >= 10) = \"Sapling\"", sourceInfo.sourceText);
    assertEquals("age >= 10", sourceInfo.condition);
    assertEquals("\"Sapling\"", sourceInfo.assignedValue);
    assertEquals(false, sourceInfo.isElseBranch);
  }

  /**
   * Tests that else branches are properly flagged.
   */
  @Test
  void testRecordElseBranch() {
    // Test that else branches are properly flagged
    DependencyTracker.enable();

    DependencyTracker.recordConditionalSource(
        "Tree.state.step",
        15,
        "state.step:else = \"Unknown\"",
        null,  // else has no condition
        "\"Unknown\"",
        true   // isElseBranch
    );

    var nodeSources = DependencyTracker.getNodeSources();
    var sourceInfo = nodeSources.get("Tree.state.step").get(0);
    assertNull(sourceInfo.condition);
    assertTrue(sourceInfo.isElseBranch);
    assertEquals("\"Unknown\"", sourceInfo.assignedValue);
  }

  /**
   * Tests that multiple conditional branches are tracked for the same node.
   */
  @Test
  void testMultipleConditionalBranches() {
    DependencyTracker.enable();

    // Record multiple conditional branches for the same node
    DependencyTracker.recordConditionalSource(
        "Tree.state.step",
        38,
        "state.step:if(current.age >= 10 years and current.state == \"Seedling\") = \"Sapling\"",
        "current.age >= 10 years and current.state == \"Seedling\"",
        "\"Sapling\"",
        false
    );

    DependencyTracker.recordConditionalSource(
        "Tree.state.step",
        41,
        "state.step:if(current.age >= 30 years and current.state == \"Sapling\") = \"Mature\"",
        "current.age >= 30 years and current.state == \"Sapling\"",
        "\"Mature\"",
        false
    );

    DependencyTracker.recordConditionalSource(
        "Tree.state.step",
        44,
        "state.step:if(current.age >= 60 years and current.state == \"Mature\") = \"Dead\"",
        "current.age >= 60 years and current.state == \"Mature\"",
        "\"Dead\"",
        false
    );

    var nodeSources = DependencyTracker.getNodeSources();
    var sourceList = nodeSources.get("Tree.state.step");
    assertNotNull(sourceList);
    assertEquals(3, sourceList.size());

    // Verify first branch
    var firstSource = sourceList.get(0);
    assertEquals(38, firstSource.sourceLine);
    assertEquals("\"Sapling\"", firstSource.assignedValue);
    assertTrue(firstSource.condition.contains("age >= 10"));

    // Verify second branch
    var secondSource = sourceList.get(1);
    assertEquals(41, secondSource.sourceLine);
    assertEquals("\"Mature\"", secondSource.assignedValue);
    assertTrue(secondSource.condition.contains("age >= 30"));

    // Verify third branch
    var thirdSource = sourceList.get(2);
    assertEquals(44, thirdSource.sourceLine);
    assertEquals("\"Dead\"", thirdSource.assignedValue);
    assertTrue(thirdSource.condition.contains("age >= 60"));
  }

  /**
   * Tests DependencyGraph.SourceLocation with condition fields.
   */
  @Test
  void testSourceLocationWithCondition() {
    // Test DependencyGraph.SourceLocation with condition fields
    var sourceLocation = new DependencyGraph.SourceLocation(
        10,
        "state.step:if(age >= 10) = \"Sapling\"",
        "age >= 10",
        "\"Sapling\"",
        false
    );

    assertEquals(10, sourceLocation.line);
    assertEquals("state.step:if(age >= 10) = \"Sapling\"", sourceLocation.text);
    assertEquals("age >= 10", sourceLocation.condition);
    assertEquals("\"Sapling\"", sourceLocation.assignedValue);
    assertEquals(false, sourceLocation.isElseBranch);
  }

  /**
   * Tests backward compatibility with old SourceLocation constructor.
   */
  @Test
  void testBackwardCompatibility() {
    // Test that the old constructor still works (backward compatibility)
    var sourceLocation = new DependencyGraph.SourceLocation(10, "some text");

    assertEquals(10, sourceLocation.line);
    assertEquals("some text", sourceLocation.text);
    assertNull(sourceLocation.condition);
    assertNull(sourceLocation.assignedValue);
    assertNull(sourceLocation.isElseBranch);
  }

  /**
   * Tests that tracking is disabled when not explicitly enabled.
   */
  @Test
  void testTrackingDisabled() {
    // Ensure tracking is disabled (cleanup from previous tests)
    DependencyTracker.disable();

    // Try to record conditional source
    DependencyTracker.recordConditionalSource(
        "Tree.state.step",
        10,
        "state.step:if(age >= 10) = \"Sapling\"",
        "age >= 10",
        "\"Sapling\"",
        false
    );

    // Should return empty map when disabled
    var nodeSources = DependencyTracker.getNodeSources();
    assertTrue(nodeSources.isEmpty());
  }

  /**
   * Tests that NodeSourceInfo preserves all conditional fields.
   */
  @Test
  void testNodeSourceInfoWithConditionals() {
    var sourceInfo = new DependencyTracker.NodeSourceInfo(
        42,
        "height.step:if(current.state == \"Seedling\") = prior.height + 0.1 m",
        "current.state == \"Seedling\"",
        "prior.height + 0.1 m",
        false
    );

    assertEquals(42, sourceInfo.sourceLine);
    assertEquals("height.step:if(current.state == \"Seedling\") = prior.height + 0.1 m",
        sourceInfo.sourceText);
    assertEquals("current.state == \"Seedling\"", sourceInfo.condition);
    assertEquals("prior.height + 0.1 m", sourceInfo.assignedValue);
    assertEquals(false, sourceInfo.isElseBranch);
  }

  /**
   * Tests that NodeSourceInfo backward compatibility constructor works.
   */
  @Test
  void testNodeSourceInfoBackwardCompatibility() {
    var sourceInfo = new DependencyTracker.NodeSourceInfo(
        42,
        "age.step = prior.age + 1 year"
    );

    assertEquals(42, sourceInfo.sourceLine);
    assertEquals("age.step = prior.age + 1 year", sourceInfo.sourceText);
    assertNull(sourceInfo.condition);
    assertNull(sourceInfo.assignedValue);
    assertNull(sourceInfo.isElseBranch);
  }
}
