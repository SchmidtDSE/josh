/**
 * Exports dependency graphs to JSON format.
 *
 * <p>This class uses Jackson ObjectMapper to serialize DependencyGraph instances
 * into JSON format suitable for visualization tools. The output format includes
 * nodes with their metadata and edges with their labels.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.analyze;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Exports dependency graphs to JSON format.
 *
 * <p>The exported JSON format contains two main arrays: "nodes" and "edges".
 * Each node includes its ID, entity type, attribute, event, and a display label.
 * Each edge includes the source ID, target ID, and the dependency path label.</p>
 *
 * <p>Example output:
 * <pre>
 * {
 *   "nodes": [
 *     {
 *       "id": "JoshuaTree.age.step",
 *       "entityType": "JoshuaTree",
 *       "attribute": "age",
 *       "event": "step",
 *       "label": "age.step",
 *       "sourceLine": 42,
 *       "sourceText": "age.step = prior.age + 1 year"
 *     }
 *   ],
 *   "edges": [
 *     {
 *       "from": "JoshuaTree.state.step",
 *       "to": "JoshuaTree.age.step",
 *       "label": "prior.age",
 *       "resolutionType": "temporal-prior",
 *       "description": "Reads 'age' from this JoshuaTree's previous timestep state"
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>Note: sourceLine and sourceText fields are optional and may not be present
 * for all nodes if source location information was not available during parsing.</p>
 *
 * <p>Example usage:
 * <pre>
 * DependencyGraph graph = extractor.extract(program, "MySimulation");
 * JsonExporter exporter = new JsonExporter();
 * String json = exporter.export(graph);
 * System.out.println(json);
 * </pre>
 */
public class JsonExporter {

  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Exports a dependency graph to JSON format.
   *
   * <p>Converts the graph structure into a JSON representation with nodes and edges.
   * The output is formatted with indentation for readability.</p>
   *
   * @param graph The dependency graph to export
   * @return A JSON string representation of the graph
   * @throws Exception If JSON serialization fails
   */
  public String export(DependencyGraph graph) throws Exception {
    ObjectNode root = mapper.createObjectNode();

    // Create nodes array
    ArrayNode nodesArray = mapper.createArrayNode();
    for (DependencyGraph.Node node : graph.getNodes()) {
      ObjectNode nodeObj = mapper.createObjectNode();
      nodeObj.put("id", node.id);
      nodeObj.put("entityType", node.entityType);
      nodeObj.put("attribute", node.attribute);
      nodeObj.put("event", node.event);
      nodeObj.put("label", node.attribute + "." + node.event);

      // Add primary source location information if available (for backward compatibility)
      if (node.sourceLine != null) {
        nodeObj.put("sourceLine", node.sourceLine);
      }
      if (node.sourceText != null) {
        nodeObj.put("sourceText", node.sourceText);
      }

      // Add all source locations (for nodes with multiple conditional handlers)
      if (!node.allSources.isEmpty()) {
        ArrayNode allSourcesArray = mapper.createArrayNode();
        for (DependencyGraph.SourceLocation sourceLoc : node.allSources) {
          ObjectNode sourceObj = mapper.createObjectNode();
          if (sourceLoc.line != null) {
            sourceObj.put("line", sourceLoc.line);
          }
          if (sourceLoc.text != null) {
            sourceObj.put("text", sourceLoc.text);
          }
          if (sourceLoc.condition != null) {
            sourceObj.put("condition", sourceLoc.condition);
          }
          if (sourceLoc.assignedValue != null) {
            sourceObj.put("assignedValue", sourceLoc.assignedValue);
          }
          if (sourceLoc.isElseBranch != null && sourceLoc.isElseBranch) {
            sourceObj.put("isElseBranch", sourceLoc.isElseBranch);
          }
          allSourcesArray.add(sourceObj);
        }
        nodeObj.set("allSources", allSourcesArray);
      }

      nodesArray.add(nodeObj);
    }
    root.set("nodes", nodesArray);

    // Create edges array
    ArrayNode edgesArray = mapper.createArrayNode();
    for (DependencyGraph.Edge edge : graph.getEdges()) {
      ObjectNode edgeObj = mapper.createObjectNode();
      edgeObj.put("from", edge.from);
      edgeObj.put("to", edge.to);
      edgeObj.put("label", edge.label);
      edgeObj.put("resolutionType", edge.resolutionType);
      edgeObj.put("description", edge.description);
      edgesArray.add(edgeObj);
    }
    root.set("edges", edgesArray);

    // Return formatted JSON string
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
  }
}
