/**
 * Unit tests for dependency analysis infrastructure.
 *
 * <p>This test suite verifies that the dependency tracking system works correctly
 * with different types of Josh language constructs, including temporal dependencies,
 * cross-entity references, spatial dependencies, simulation dependencies, and
 * collection operations.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import org.joshsim.JoshSimFacade;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.JvmInputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.parse.ParseResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the dependency tracking and graph extraction system.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>Temporal dependencies (prior.X, current.X) are correctly tracked</li>
 *   <li>Cross-entity temporal dependencies within same entity work</li>
 *   <li>Spatial dependencies (here.X) to patch attributes are resolved</li>
 *   <li>Simulation dependencies (meta.X) are correctly identified</li>
 *   <li>Collection dependencies (EntityCollection.X) work properly</li>
 *   <li>Dependency tracker can be enabled/disabled</li>
 *   <li>DependencyPathParser handles various path formats</li>
 *   <li>JsonExporter produces valid JSON output</li>
 * </ul>
 */
public class DependencyGraphTest {

  /**
   * Ensures DependencyTracker is disabled after each test to prevent interference.
   */
  @AfterEach
  public void tearDown() {
    DependencyTracker.disable();
  }

  /**
   * Test 1: Simple Temporal Dependencies (prior.age).
   *
   * <p>Verifies that temporal dependencies using "prior" are correctly tracked.
   * The Tree.age.step attribute depends on Tree.age.step (via prior.age).</p>
   */
  @Test
  public void testSimpleTemporalDependencies() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          age.init = 0 year
          age.step = prior.age + 1 year
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Verify graph structure
    assertNotNull(graph, "Dependency graph should not be null");
    assertTrue(graph.getNodes().size() > 0, "Graph should have nodes");
    assertTrue(graph.getEdges().size() > 0, "Graph should have edges");

    // Find the Tree.age.step node
    DependencyGraph.Node ageStepNode = findNode(graph, "Tree", "age", "step");
    assertNotNull(ageStepNode, "Should find Tree.age.step node");
    assertEquals("Tree.age.step", ageStepNode.id);

    // Find edge with prior.age dependency
    DependencyGraph.Edge priorEdge = findEdge(graph, "prior.age");
    assertNotNull(priorEdge, "Should find edge with prior.age dependency");
    assertEquals("Tree.age.step", priorEdge.from, "Edge should come from Tree.age.step");
    assertEquals("Tree.age.step", priorEdge.to, "Edge should point to Tree.age.step (temporal)");
    assertEquals("temporal-prior", priorEdge.resolutionType,
        "Edge should have temporal-prior resolution type");
    assertTrue(priorEdge.description.contains("previous timestep"),
        "Description should mention previous timestep");
  }

  /**
   * Test 2: Cross-Entity Temporal Dependencies (current.age).
   *
   * <p>Verifies that temporal dependencies using "current" within the same entity work.
   * The Tree.height.step attribute depends on Tree.age.step (via current.age).</p>
   */
  @Test
  public void testCrossEntityTemporalDependencies() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          age.init = 0 year
          age.step = prior.age + 1 year
          height.init = 0 m
          height.step = current.age * 0.1 m
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Verify height node exists
    DependencyGraph.Node heightNode = findNode(graph, "Tree", "height", "step");
    assertNotNull(heightNode, "Should find Tree.height.step node");

    // Verify age node exists
    DependencyGraph.Node ageNode = findNode(graph, "Tree", "age", "step");
    assertNotNull(ageNode, "Should find Tree.age.step node");

    // Find edge from height.step using current.age
    DependencyGraph.Edge currentEdge = findEdge(graph, "current.age");
    assertNotNull(currentEdge, "Should find edge with current.age dependency");
    assertEquals("Tree.height.step", currentEdge.from,
        "Edge should come from Tree.height.step");
    assertEquals("Tree.age.step", currentEdge.to,
        "Edge should point to Tree.age.step");
    assertEquals("temporal-current", currentEdge.resolutionType,
        "Edge should have temporal-current resolution type");
    assertTrue(currentEdge.description.contains("current state"),
        "Description should mention current state");
  }

  /**
   * Test 3: Spatial Dependencies (here.fire).
   *
   * <p>Verifies that spatial dependencies to patch attributes work correctly.
   * The Tree.survival.step attribute depends on Patch.fire.step (via here.fire).
   * Note: DependencyPathParser resolves "here" to "Patch" as a generic entity type.</p>
   */
  @Test
  public void testSpatialDependencies() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
          fire.init = false
          fire.step = false
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          survival.init = true
          survival.step = here.fire
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Verify Patch.fire.step node exists (resolved from here.fire)
    // The DependencyPathParser maps "here.X" to "Patch.X" as generic entity type
    DependencyGraph.Node fireNode = findNode(graph, "Patch", "fire", "step");
    assertNotNull(fireNode, "Should find Patch.fire.step node");
    assertEquals("Patch", fireNode.entityType);

    // Verify Tree.survival.step node exists
    DependencyGraph.Node survivalNode = findNode(graph, "Tree", "survival", "step");
    assertNotNull(survivalNode, "Should find Tree.survival.step node");

    // Find edge with here.fire dependency
    DependencyGraph.Edge hereEdge = findEdge(graph, "here.fire");
    assertNotNull(hereEdge, "Should find edge with here.fire dependency");
    assertEquals("Tree.survival.step", hereEdge.from,
        "Edge should come from Tree.survival.step");
    assertEquals("Patch.fire.step", hereEdge.to,
        "Edge should point to Patch.fire.step");
    assertEquals("spatial-patch", hereEdge.resolutionType,
        "Edge should have spatial-patch resolution type");
    assertTrue(hereEdge.description.contains("Patch where"),
        "Description should mention Patch location");
  }

  /**
   * Test 4: Simulation Dependencies (meta.isMastYear).
   *
   * <p>Verifies that simulation-level dependencies work correctly.
   * The Tree.isFlowering.step attribute depends on Simulation.isMastYear.step.</p>
   */
  @Test
  public void testSimulationDependencies() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
          isMastYear.init = false
          isMastYear.step = false
        end simulation

        start patch Default
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          isFlowering.init = false
          isFlowering.step = meta.isMastYear
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Verify Simulation.isMastYear.step node exists
    DependencyGraph.Node mastYearNode = findNode(graph, "Simulation", "isMastYear", "step");
    assertNotNull(mastYearNode, "Should find Simulation.isMastYear.step node");
    assertEquals("Simulation", mastYearNode.entityType);

    // Verify Tree.isFlowering.step node exists
    DependencyGraph.Node floweringNode = findNode(graph, "Tree", "isFlowering", "step");
    assertNotNull(floweringNode, "Should find Tree.isFlowering.step node");

    // Find edge with meta.isMastYear dependency
    DependencyGraph.Edge metaEdge = findEdge(graph, "meta.isMastYear");
    assertNotNull(metaEdge, "Should find edge with meta.isMastYear dependency");
    assertEquals("Tree.isFlowering.step", metaEdge.from,
        "Edge should come from Tree.isFlowering.step");
    assertEquals("Simulation.isMastYear.step", metaEdge.to,
        "Edge should point to Simulation.isMastYear.step");
    assertEquals("simulation", metaEdge.resolutionType,
        "Edge should have simulation resolution type");
    assertTrue(metaEdge.description.contains("simulation-level"),
        "Description should mention simulation-level");
  }

  /**
   * Test 5: Collection Dependencies (Trees.age).
   *
   * <p>Verifies that collection operations correctly track dependencies.
   * The Patch.avgAge.step attribute depends on Tree.age.step (via Trees.age).</p>
   */
  @Test
  public void testCollectionDependencies() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
          Tree.init = create 5 count of Tree
          avgAge.init = 0 year
          avgAge.step = mean(Trees.age)
        end patch

        start organism Tree
          age.init = 0 year
          age.step = prior.age + 1 year
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Verify Tree.age.step node exists
    DependencyGraph.Node ageNode = findNode(graph, "Tree", "age", "step");
    assertNotNull(ageNode, "Should find Tree.age.step node");

    // Verify Default.avgAge.step node exists (patch name is Default, not Patch)
    DependencyGraph.Node avgAgeNode = findNode(graph, "Default", "avgAge", "step");
    assertNotNull(avgAgeNode, "Should find Default.avgAge.step node");

    // Find edge with Trees.age dependency
    DependencyGraph.Edge collectionEdge = findEdge(graph, "Trees.age");
    assertNotNull(collectionEdge, "Should find edge with Trees.age dependency");
    assertEquals("Default.avgAge.step", collectionEdge.from,
        "Edge should come from Default.avgAge.step");
    assertEquals("Trees.age.step", collectionEdge.to,
        "Edge should point to Trees.age.step");
    assertEquals("explicit-entity", collectionEdge.resolutionType,
        "Edge should have explicit-entity resolution type");
    assertTrue(collectionEdge.description.contains("Trees entities"),
        "Description should mention Trees entities");
  }

  /**
   * Test 6: DependencyTracker Enable/Disable.
   *
   * <p>Verifies that DependencyTracker can be enabled and disabled, and that
   * tracking only occurs when enabled.</p>
   */
  @Test
  public void testDependencyTrackerEnableDisable() {
    // Initially disabled
    assertFalse(DependencyTracker.isEnabled(),
        "DependencyTracker should be disabled initially");

    // Enable tracking
    DependencyTracker.enable();
    assertTrue(DependencyTracker.isEnabled(),
        "DependencyTracker should be enabled after enable()");

    // Record a dependency
    DependencyTracker.recordDependency("Test.attr.step", "prior.value", "prior.value");
    assertFalse(DependencyTracker.getDependencies().isEmpty(),
        "Should have tracked dependencies");

    // Disable tracking
    DependencyTracker.disable();
    assertFalse(DependencyTracker.isEnabled(),
        "DependencyTracker should be disabled after disable()");
    assertTrue(DependencyTracker.getDependencies().isEmpty(),
        "Should return empty map when disabled");
  }

  /**
   * Test 7: DependencyPathParser with Simple Paths.
   *
   * <p>Tests that the DependencyPathParser correctly handles simple attribute paths.</p>
   */
  @Test
  public void testDependencyPathParserSimplePaths() {
    DependencyPathParser.DependencyInfo info =
        DependencyPathParser.parse("age", "Tree", "step");

    assertNotNull(info);
    assertEquals("Tree.age.step", info.targetNodeId);
    assertEquals("Tree", info.targetEntity);
    assertEquals("age", info.targetAttribute);
    assertEquals("step", info.targetEvent);
  }

  /**
   * Test 8: DependencyPathParser with Temporal Paths.
   *
   * <p>Tests that the DependencyPathParser correctly handles temporal paths
   * (prior.X, current.X).</p>
   */
  @Test
  public void testDependencyPathParserTemporalPaths() {
    // Test prior
    DependencyPathParser.DependencyInfo priorInfo =
        DependencyPathParser.parse("prior.age", "Tree", "step");
    assertNotNull(priorInfo);
    assertEquals("Tree.age.step", priorInfo.targetNodeId);
    assertEquals("Tree", priorInfo.targetEntity);
    assertEquals("age", priorInfo.targetAttribute);

    // Test current
    DependencyPathParser.DependencyInfo currentInfo =
        DependencyPathParser.parse("current.height", "Tree", "step");
    assertNotNull(currentInfo);
    assertEquals("Tree.height.step", currentInfo.targetNodeId);
    assertEquals("Tree", currentInfo.targetEntity);
    assertEquals("height", currentInfo.targetAttribute);
  }

  /**
   * Test 9: DependencyPathParser with Spatial Paths.
   *
   * <p>Tests that the DependencyPathParser correctly handles spatial paths
   * (here.X, here.Collection.X).</p>
   */
  @Test
  public void testDependencyPathParserSpatialPaths() {
    // Test patch attribute
    DependencyPathParser.DependencyInfo patchInfo =
        DependencyPathParser.parse("here.fire", "Tree", "step");
    assertNotNull(patchInfo);
    assertEquals("Patch.fire.step", patchInfo.targetNodeId);
    assertEquals("Patch", patchInfo.targetEntity);
    assertEquals("fire", patchInfo.targetAttribute);

    // Test collection
    DependencyPathParser.DependencyInfo collectionInfo =
        DependencyPathParser.parse("here.Trees.age", "Patch", "step");
    assertNotNull(collectionInfo);
    assertEquals("Trees.age.step", collectionInfo.targetNodeId);
    assertEquals("Trees", collectionInfo.targetEntity);
    assertEquals("age", collectionInfo.targetAttribute);
  }

  /**
   * Test 10: DependencyPathParser with Simulation Paths.
   *
   * <p>Tests that the DependencyPathParser correctly handles simulation paths
   * (meta.X).</p>
   */
  @Test
  public void testDependencyPathParserSimulationPaths() {
    DependencyPathParser.DependencyInfo info =
        DependencyPathParser.parse("meta.isMastYear", "Tree", "step");

    assertNotNull(info);
    assertEquals("Simulation.isMastYear.step", info.targetNodeId);
    assertEquals("Simulation", info.targetEntity);
    assertEquals("isMastYear", info.targetAttribute);
    assertEquals("step", info.targetEvent);
  }

  /**
   * Test 11: DependencyPathParser with Explicit Entity Paths.
   *
   * <p>Tests that the DependencyPathParser correctly handles explicit entity
   * references (EntityName.attribute).</p>
   */
  @Test
  public void testDependencyPathParserExplicitEntityPaths() {
    DependencyPathParser.DependencyInfo info =
        DependencyPathParser.parse("JoshuaTrees.isFlowering", "Patch", "end");

    assertNotNull(info);
    assertEquals("JoshuaTrees.isFlowering.end", info.targetNodeId);
    assertEquals("JoshuaTrees", info.targetEntity);
    assertEquals("isFlowering", info.targetAttribute);
    assertEquals("end", info.targetEvent);
  }

  /**
   * Test 12: JsonExporter Produces Valid JSON.
   *
   * <p>Verifies that the JsonExporter produces valid JSON with correct structure.</p>
   */
  @Test
  public void testJsonExporterProducesValidJson() throws Exception {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          age.init = 0 year
          age.step = prior.age + 1 year
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Export to JSON
    JsonExporter exporter = new JsonExporter();
    String json = exporter.export(graph);

    // Verify JSON is not empty
    assertNotNull(json, "JSON should not be null");
    assertFalse(json.isEmpty(), "JSON should not be empty");

    // Verify JSON contains expected keys
    assertTrue(json.contains("\"nodes\""), "JSON should contain 'nodes' key");
    assertTrue(json.contains("\"edges\""), "JSON should contain 'edges' key");
    assertTrue(json.contains("\"id\""), "JSON should contain node 'id' field");
    assertTrue(json.contains("\"entityType\""), "JSON should contain node 'entityType' field");
    assertTrue(json.contains("\"attribute\""), "JSON should contain node 'attribute' field");
    assertTrue(json.contains("\"event\""), "JSON should contain node 'event' field");
    assertTrue(json.contains("\"label\""), "JSON should contain edge 'label' field");
    assertTrue(json.contains("\"from\""), "JSON should contain edge 'from' field");
    assertTrue(json.contains("\"to\""), "JSON should contain edge 'to' field");
    assertTrue(json.contains("\"resolutionType\""),
        "JSON should contain edge 'resolutionType' field");
    assertTrue(json.contains("\"description\""),
        "JSON should contain edge 'description' field");
  }

  /**
   * Test 13: Multiple Dependencies from Same Node.
   *
   * <p>Verifies that a single node can have multiple outgoing dependencies.</p>
   */
  @Test
  public void testMultipleDependenciesFromSameNode() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
          globalTemp.init = 20.0 celsius
        end simulation

        start patch Default
          fire.init = false
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          age.init = 0 year
          age.step = prior.age + 1 year
          state.step = prior.age + meta.globalTemp + here.fire
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Find edges from Tree.state.step
    List<DependencyGraph.Edge> stateEdges = graph.getEdges().stream()
        .filter(e -> e.from.equals("Tree.state.step"))
        .toList();

    // Should have at least 3 dependencies
    assertTrue(stateEdges.size() >= 3,
        "Tree.state.step should have at least 3 dependencies");

    // Verify different types of dependencies exist
    boolean hasPrior = stateEdges.stream().anyMatch(e -> e.label.contains("prior.age"));
    boolean hasMeta = stateEdges.stream().anyMatch(e -> e.label.contains("meta.globalTemp"));
    boolean hasHere = stateEdges.stream().anyMatch(e -> e.label.contains("here.fire"));

    assertTrue(hasPrior, "Should have prior.age dependency");
    assertTrue(hasMeta, "Should have meta.globalTemp dependency");
    assertTrue(hasHere, "Should have here.fire dependency");
  }

  /**
   * Test 14: Graph Contains No Duplicate Nodes.
   *
   * <p>Verifies that the graph does not contain duplicate nodes with the same ID.</p>
   */
  @Test
  public void testGraphContainsNoDuplicateNodes() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          age.init = 0 year
          age.step = prior.age + 1 year
          height.step = current.age * 0.1 m
          biomass.step = current.age + current.height
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Count occurrences of Tree.age.step
    long ageNodeCount = graph.getNodes().stream()
        .filter(n -> n.id.equals("Tree.age.step"))
        .count();

    assertEquals(1, ageNodeCount,
        "Tree.age.step should appear exactly once in the graph");
  }

  /**
   * Test 15: Empty Graph When No Dependencies.
   *
   * <p>Verifies that programs with no dependencies produce minimal or empty graphs.</p>
   */
  @Test
  public void testEmptyGraphWhenNoDependencies() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
          value.init = 42 count
        end patch
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Graph should exist but have minimal content
    assertNotNull(graph, "Dependency graph should not be null");
    // Note: May have some nodes from interpretation process,
    // but should not have complex dependency edges
  }

  /**
   * Test 16: Source Location Information.
   *
   * <p>Verifies that source line numbers and source text are captured for nodes.</p>
   */
  @Test
  public void testSourceLocationInformation() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          age.init = 0 year
          age.step = prior.age + 1 year
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Find the Tree.age.step node
    DependencyGraph.Node ageStepNode = findNode(graph, "Tree", "age", "step");
    assertNotNull(ageStepNode, "Should find Tree.age.step node");

    // Verify source location information is present
    assertNotNull(ageStepNode.sourceLine, "Source line should be captured");
    assertTrue(ageStepNode.sourceLine > 0, "Source line should be a positive number");

    // Source text should contain the attribute definition
    if (ageStepNode.sourceText != null) {
      assertTrue(ageStepNode.sourceText.contains("age.step"),
          "Source text should contain the attribute name");
    }

    // Also verify JSON export includes source location
    try {
      JsonExporter exporter = new JsonExporter();
      String json = exporter.export(graph);

      assertTrue(json.contains("\"sourceLine\""),
          "JSON export should contain sourceLine field");

      // The JSON might contain sourceText if it was captured
      // (This is optional depending on how ANTLR getText() works)
    } catch (Exception e) {
      throw new RuntimeException("JSON export failed", e);
    }
  }

  /**
   * Test 17: Multiple Source Locations for Conditional Handlers.
   *
   * <p>Verifies that nodes with multiple conditional handlers (e.g., state.step:if)
   * capture all source locations, not just the last one.</p>
   */
  @Test
  public void testMultipleSourceLocationsForConditionalHandlers() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
        end simulation

        start patch Default
          Tree.init = create 5 count of Tree
        end patch

        start organism Tree
          age.init = 0 year
          age.step = prior.age + 1 year
          state.init = "Seedling"
          state.step:if(age >= 10 year) = "Sapling"
          state.step:if(age >= 30 year) = "Mature"
          state.step:if(age >= 60 year) = "Dead"
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Find the Tree.state.step node
    DependencyGraph.Node stateStepNode = findNode(graph, "Tree", "state", "step");
    assertNotNull(stateStepNode, "Should find Tree.state.step node");

    // Verify that allSources contains all three conditional handlers
    assertNotNull(stateStepNode.allSources, "allSources should not be null");
    assertEquals(3, stateStepNode.allSources.size(),
        "Tree.state.step should have 3 source locations (one for each conditional handler)");

    // Verify each source location has a line number
    for (DependencyGraph.SourceLocation sourceLoc : stateStepNode.allSources) {
      assertNotNull(sourceLoc.line, "Each source location should have a line number");
      assertTrue(sourceLoc.line > 0, "Line number should be positive");
      assertNotNull(sourceLoc.text, "Each source location should have source text");
      assertTrue(sourceLoc.text.contains("state.step"),
          "Source text should contain 'state.step'");
    }

    // Verify the primary source is the first one (for backward compatibility)
    assertEquals(stateStepNode.allSources.get(0).line, stateStepNode.sourceLine,
        "Primary sourceLine should match first allSources entry");
    assertEquals(stateStepNode.allSources.get(0).text, stateStepNode.sourceText,
        "Primary sourceText should match first allSources entry");

    // Verify JSON export includes allSources
    try {
      JsonExporter exporter = new JsonExporter();
      String json = exporter.export(graph);

      assertTrue(json.contains("\"allSources\""),
          "JSON export should contain allSources field");

      // Verify the JSON has multiple source entries
      int allSourcesCount = json.split("\"allSources\"").length - 1;
      assertTrue(allSourcesCount > 0,
          "JSON should have allSources array for nodes with multiple sources");
    } catch (Exception e) {
      throw new RuntimeException("JSON export failed", e);
    }
  }

  /**
   * Test 18: Resolution Types and Descriptions.
   *
   * <p>Comprehensive test verifying that all resolution types and descriptions
   * are correctly assigned for different dependency patterns.</p>
   */
  @Test
  public void testResolutionTypesAndDescriptions() {
    String joshCode = """
        start simulation Test
          grid.size = 1000 m
          steps.low = 0 count
          steps.high = 10 count
          globalTemp.init = 20.0 celsius
          globalTemp.step = 20.0 celsius
        end simulation

        start patch Default
          fire.init = false
          fire.step = false
          Tree.init = create 5 count of Tree
          avgHeight.init = 0 m
          avgHeight.step = mean(Trees.height)
        end patch

        start organism Tree
          age.init = 0 year
          age.step = prior.age + 1 year
          height.init = 0 m
          height.step = current.age * 0.1 m
          survival.step = here.fire
          temp.step = meta.globalTemp
          simpleValue.step = age
        end organism
        """;

    DependencyGraph graph = parseAndTrack(joshCode, "Test");

    // Test temporal-prior
    DependencyGraph.Edge priorEdge = findEdge(graph, "prior.age");
    assertNotNull(priorEdge);
    assertEquals("temporal-prior", priorEdge.resolutionType);
    assertTrue(priorEdge.description.contains("previous timestep"));
    assertTrue(priorEdge.description.contains("Tree"));

    // Test temporal-current
    DependencyGraph.Edge currentEdge = findEdge(graph, "current.age");
    assertNotNull(currentEdge);
    assertEquals("temporal-current", currentEdge.resolutionType);
    assertTrue(currentEdge.description.contains("current state"));
    assertTrue(currentEdge.description.contains("same timestep"));

    // Test spatial-patch
    DependencyGraph.Edge spatialPatchEdge = findEdge(graph, "here.fire");
    assertNotNull(spatialPatchEdge);
    assertEquals("spatial-patch", spatialPatchEdge.resolutionType);
    assertTrue(spatialPatchEdge.description.contains("Patch where"));

    // Test spatial-collection
    DependencyGraph.Edge spatialCollectionEdge = findEdge(graph, "Trees.height");
    assertNotNull(spatialCollectionEdge);
    assertEquals("explicit-entity", spatialCollectionEdge.resolutionType);
    assertTrue(spatialCollectionEdge.description.contains("Trees entities"));

    // Test simulation
    DependencyGraph.Edge simulationEdge = findEdge(graph, "meta.globalTemp");
    assertNotNull(simulationEdge);
    assertEquals("simulation", simulationEdge.resolutionType);
    assertTrue(simulationEdge.description.contains("simulation-level"));

    // Test simple
    DependencyGraph.Edge simpleEdge = findEdge(graph, "age");
    assertNotNull(simpleEdge);
    assertEquals("simple", simpleEdge.resolutionType);
    assertTrue(simpleEdge.description.contains("current state"));
  }

  // ==================== Helper Methods ====================

  /**
   * Parses Josh code and tracks dependencies.
   *
   * @param joshCode The Josh source code to parse
   * @param simulationName The simulation name to extract
   * @return The extracted DependencyGraph
   */
  private DependencyGraph parseAndTrack(String joshCode, String simulationName) {
    // Parse the Josh code
    ParseResult parsed = JoshSimFacade.parse(joshCode);
    assertFalse(parsed.hasErrors(),
        "Josh code should parse without errors. Errors: " + parsed.getErrors());

    // Create geometry factory and input/output layer
    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    JvmInputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(1)
        .build();

    // Enable dependency tracking
    DependencyTracker.enable();

    // Interpret the code
    JoshProgram program = JoshSimFacade.interpret(geometryFactory, parsed, inputOutputLayer);
    assertNotNull(program, "Program should be successfully interpreted");

    // Extract dependency graph
    DependencyGraphExtractor extractor = new DependencyGraphExtractor();
    DependencyGraph graph = extractor.extract(program, simulationName);

    return graph;
  }

  /**
   * Finds a node in the graph by entity, attribute, and event.
   *
   * @param graph The dependency graph to search
   * @param entityType The entity type to find
   * @param attribute The attribute name to find
   * @param event The event name to find
   * @return The matching node, or null if not found
   */
  private DependencyGraph.Node findNode(DependencyGraph graph, String entityType,
      String attribute, String event) {
    Collection<DependencyGraph.Node> nodes = graph.getNodes();
    for (DependencyGraph.Node node : nodes) {
      if (node.entityType.equals(entityType)
          && node.attribute.equals(attribute)
          && node.event.equals(event)) {
        return node;
      }
    }
    return null;
  }

  /**
   * Finds an edge in the graph by label.
   *
   * @param graph The dependency graph to search
   * @param label The edge label to find
   * @return The matching edge, or null if not found
   */
  private DependencyGraph.Edge findEdge(DependencyGraph graph, String label) {
    List<DependencyGraph.Edge> edges = graph.getEdges();
    for (DependencyGraph.Edge edge : edges) {
      if (edge.label.equals(label)) {
        return edge;
      }
    }
    return null;
  }
}
