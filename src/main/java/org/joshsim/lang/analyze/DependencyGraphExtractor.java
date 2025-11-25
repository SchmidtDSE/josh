/**
 * Extracts dependency graphs from Josh programs.
 *
 * <p>This class processes tracked dependencies from the DependencyTracker and
 * constructs a complete DependencyGraph with all nodes and edges. It uses the
 * DependencyPathParser to resolve dependency paths to their target nodes.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.analyze;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joshsim.lang.analyze.DependencyTracker.NodeSourceInfo;
import org.joshsim.lang.interpret.JoshProgram;

/**
 * Extracts a dependency graph from tracked dependencies.
 *
 * <p>This class retrieves all dependencies recorded by the DependencyTracker during
 * program interpretation and constructs a DependencyGraph. It parses dependency paths
 * using the DependencyPathParser to resolve cross-entity references and temporal
 * dependencies.</p>
 *
 * <p>Example usage:
 * <pre>
 * // Assuming DependencyTracker.enable() was called during interpretation
 * DependencyGraphExtractor extractor = new DependencyGraphExtractor();
 * DependencyGraph graph = extractor.extract(program, "MySimulation");
 * </pre>
 */
public class DependencyGraphExtractor {

  /**
   * Extracts a dependency graph from the current tracked dependencies.
   *
   * <p>This method retrieves all dependencies recorded by the DependencyTracker
   * and constructs a complete dependency graph. Each "from" node and each
   * dependency target is added to the graph, and edges are created for each
   * dependency path.</p>
   *
   * <p>The method expects that DependencyTracker has been enabled and populated
   * during program interpretation. If tracking is not enabled, an empty graph
   * will be returned.</p>
   *
   * @param program The Josh program (currently unused but reserved for future use)
   * @param simulationName The simulation name (currently unused but reserved for future use)
   * @return A DependencyGraph containing all tracked nodes and edges
   */
  public DependencyGraph extract(JoshProgram program, String simulationName) {
    DependencyGraph graph = new DependencyGraph();

    // Get all tracked dependencies from the current thread
    Map<String, Set<String>> trackedDeps = DependencyTracker.getDependencies();

    // Get all tracked node sources (now a map of node ID to list of source info)
    Map<String, List<NodeSourceInfo>> nodeSources = DependencyTracker.getNodeSources();

    // Process each dependency relationship
    for (Map.Entry<String, Set<String>> entry : trackedDeps.entrySet()) {
      String[] parts = entry.getKey().split(" -> ");
      String fromNode = parts[0];

      // Parse fromNode to get entity/attribute/event
      String[] fromParts = fromNode.split("\\.");
      if (fromParts.length < 3) {
        // Skip malformed node IDs
        continue;
      }

      String fromEntity = fromParts[0];
      String fromAttr = fromParts[1];
      String fromEvent = fromParts[2];

      // Get source info list for the from node
      List<NodeSourceInfo> fromSourceInfoList = nodeSources.get(fromNode);
      List<DependencyGraph.SourceLocation> fromSourceLocations = new ArrayList<>();
      if (fromSourceInfoList != null) {
        for (NodeSourceInfo sourceInfo : fromSourceInfoList) {
          fromSourceLocations.add(new DependencyGraph.SourceLocation(
              sourceInfo.sourceLine, sourceInfo.sourceText));
        }
      }

      // Add the source node with all source location info
      graph.addNode(fromNode, fromEntity, fromAttr, fromEvent, fromSourceLocations);

      // Process each dependency path for this relationship
      for (String path : entry.getValue()) {
        // Parse the dependency path to resolve the target node
        DependencyPathParser.DependencyInfo depInfo =
            DependencyPathParser.parse(path, fromEntity, fromEvent);

        // Get source info list for the target node
        List<NodeSourceInfo> targetSourceInfoList =
            nodeSources.get(depInfo.targetNodeId);
        List<DependencyGraph.SourceLocation> targetSourceLocations = new ArrayList<>();
        if (targetSourceInfoList != null) {
          for (NodeSourceInfo sourceInfo : targetSourceInfoList) {
            targetSourceLocations.add(new DependencyGraph.SourceLocation(
                sourceInfo.sourceLine, sourceInfo.sourceText));
          }
        }

        // Add the target node with all source location info
        graph.addNode(
            depInfo.targetNodeId,
            depInfo.targetEntity,
            depInfo.targetAttribute,
            depInfo.targetEvent,
            targetSourceLocations
        );

        // Add the edge from source to target with resolution metadata
        graph.addEdge(fromNode, depInfo.targetNodeId, path,
            depInfo.resolutionType, depInfo.description);
      }
    }

    return graph;
  }
}
