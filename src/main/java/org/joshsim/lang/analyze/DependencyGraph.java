/**
 * Data structures for representing a dependency graph.
 *
 * <p>This class provides the core graph structure for representing dependencies
 * between nodes in a Josh program. Each node represents an attribute at a specific
 * event (e.g., "JoshuaTree.age.step"), and edges represent dependencies between them.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.analyze;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a directed dependency graph for a Josh program.
 *
 * <p>The graph consists of nodes (representing entity attributes at specific events)
 * and edges (representing dependencies between nodes). Nodes are stored in a map
 * to prevent duplicates, while edges are stored in a list to preserve order and
 * allow multiple edges with the same endpoints but different labels.</p>
 *
 * <p>Example usage:
 * <pre>
 * DependencyGraph graph = new DependencyGraph();
 * graph.addNode("JoshuaTree.age.step", "JoshuaTree", "age", "step");
 * graph.addNode("JoshuaTree.age.init", "JoshuaTree", "age", "init");
 * graph.addEdge("JoshuaTree.age.step", "JoshuaTree.age.step", "prior.age");
 * </pre>
 */
public class DependencyGraph {

  private Map<String, Node> nodes = new LinkedHashMap<>();
  private List<Edge> edges = new ArrayList<>();

  /**
   * Adds a node to the graph without source location information.
   *
   * <p>If a node with the same ID already exists, this method does nothing.
   * This ensures that each unique node appears only once in the graph.</p>
   *
   * @param id The unique identifier for this node (e.g., "JoshuaTree.age.step")
   * @param entityType The entity type (e.g., "JoshuaTree", "Patch", "Simulation")
   * @param attribute The attribute name (e.g., "age", "height")
   * @param event The event name (e.g., "init", "step", "end")
   */
  public void addNode(String id, String entityType, String attribute, String event) {
    nodes.putIfAbsent(id, new Node(id, entityType, attribute, event, null, null));
  }

  /**
   * Adds a node to the graph with source location information.
   *
   * <p>If a node with the same ID already exists, the source location is merged with existing
   * source locations. This allows tracking multiple conditional handlers for the same node.</p>
   *
   * @param id The unique identifier for this node (e.g., "JoshuaTree.age.step")
   * @param entityType The entity type (e.g., "JoshuaTree", "Patch", "Simulation")
   * @param attribute The attribute name (e.g., "age", "height")
   * @param event The event name (e.g., "init", "step", "end")
   * @param sourceLine The line number in the source file (can be null)
   * @param sourceText The source code text for this node (can be null)
   */
  public void addNode(String id, String entityType, String attribute, String event,
      Integer sourceLine, String sourceText) {
    addNode(id, entityType, attribute, event,
        sourceLine != null || sourceText != null
            ? Collections.singletonList(new SourceLocation(sourceLine, sourceText))
            : Collections.emptyList());
  }

  /**
   * Adds a node to the graph with multiple source locations.
   *
   * <p>If a node with the same ID already exists, the source locations are merged.
   * This method is used internally to handle nodes with multiple conditional handlers.</p>
   *
   * @param id The unique identifier for this node (e.g., "JoshuaTree.age.step")
   * @param entityType The entity type (e.g., "JoshuaTree", "Patch", "Simulation")
   * @param attribute The attribute name (e.g., "age", "height")
   * @param event The event name (e.g., "init", "step", "end")
   * @param newSources The source locations to add for this node
   */
  public void addNode(String id, String entityType, String attribute, String event,
      List<SourceLocation> newSources) {
    Node existingNode = nodes.get(id);
    if (existingNode != null) {
      // Merge source locations with deduplication
      List<SourceLocation> mergedSources = deduplicateSources(existingNode.allSources, newSources);

      // Keep the primary source from the first occurrence
      nodes.put(id, new Node(id, entityType, attribute, event,
          existingNode.sourceLine, existingNode.sourceText, mergedSources));
    } else if (!newSources.isEmpty()) {
      // New node with source locations (deduplicated)
      List<SourceLocation> dedupedSources = deduplicateSources(new ArrayList<>(), newSources);
      SourceLocation primarySource = dedupedSources.isEmpty() ? newSources.get(0) : dedupedSources.get(0);
      nodes.put(id, new Node(id, entityType, attribute, event,
          primarySource.line, primarySource.text, dedupedSources));
    } else {
      // New node without source locations
      nodes.put(id, new Node(id, entityType, attribute, event, null, null));
    }
  }

  /**
   * Deduplicates source locations by (line, condition) tuple.
   *
   * @param existing The existing sources (already deduplicated)
   * @param newSources The new sources to merge
   * @return A deduplicated list combining both
   */
  private List<SourceLocation> deduplicateSources(List<SourceLocation> existing,
      List<SourceLocation> newSources) {
    Set<String> seen = new HashSet<>();
    List<SourceLocation> result = new ArrayList<>();

    // Add existing sources first (they're already in the result)
    for (SourceLocation loc : existing) {
      String key = Objects.toString(loc.line, "null") + "|" + Objects.toString(loc.condition, "null");
      if (!seen.contains(key)) {
        seen.add(key);
        result.add(loc);
      }
    }

    // Add new sources if not duplicates
    for (SourceLocation loc : newSources) {
      String key = Objects.toString(loc.line, "null") + "|" + Objects.toString(loc.condition, "null");
      if (!seen.contains(key)) {
        seen.add(key);
        result.add(loc);
      }
    }

    return result;
  }

  /**
   * Adds an edge to the graph.
   *
   * <p>Edges are always added, even if an edge with the same endpoints exists.
   * This allows tracking multiple dependency paths between the same two nodes.</p>
   *
   * @param from The source node ID
   * @param to The target node ID
   * @param label The dependency path label (e.g., "prior.age", "here.fire")
   * @param resolutionType The type of dependency resolution
   *        (e.g., "temporal-prior", "spatial-patch")
   * @param description Human-readable explanation of the dependency
   */
  public void addEdge(String from, String to, String label, String resolutionType,
      String description) {
    edges.add(new Edge(from, to, label, resolutionType, description));
  }

  /**
   * Gets all nodes in the graph.
   *
   * @return A collection of all nodes in insertion order
   */
  public Collection<Node> getNodes() {
    return nodes.values();
  }

  /**
   * Gets all edges in the graph.
   *
   * @return A list of all edges in insertion order
   */
  public List<Edge> getEdges() {
    return edges;
  }

  /**
   * Represents a source location for a node.
   *
   * <p>Contains the line number and source text for a specific occurrence of a node
   * definition. Nodes can have multiple source locations when they have multiple
   * conditional event handlers.</p>
   */
  public static class SourceLocation {
    /** Line number in the Josh source file. */
    public final Integer line;

    /** The actual Josh source code text. */
    public final String text;

    /** The condition expression (can be null). */
    public final String condition;

    /** The value being assigned (can be null). */
    public final String assignedValue;

    /** True if this is an else branch (can be null). */
    public final Boolean isElseBranch;

    /**
     * Creates a new source location without conditional context.
     *
     * @param line The line number in the source file
     * @param text The source code text
     */
    public SourceLocation(Integer line, String text) {
      this(line, text, null, null, null);
    }

    /**
     * Creates a new source location with conditional context.
     *
     * @param line The line number in the source file
     * @param text The source code text
     * @param condition The condition expression (can be null)
     * @param assignedValue The value being assigned (can be null)
     * @param isElseBranch True if this is an else branch (can be null)
     */
    public SourceLocation(Integer line, String text, String condition,
        String assignedValue, Boolean isElseBranch) {
      this.line = line;
      this.text = text;
      this.condition = condition;
      this.assignedValue = assignedValue;
      this.isElseBranch = isElseBranch;
    }
  }

  /**
   * Represents a node in the dependency graph.
   *
   * <p>Each node corresponds to a specific attribute of an entity at a particular event.
   * For example, "JoshuaTree.age.step" represents the age attribute of JoshuaTree
   * during the step event.</p>
   *
   * <p>Nodes can have multiple source locations when they are defined by multiple
   * conditional event handlers (e.g., state.step:if(age >= 10) on multiple lines).</p>
   */
  public static class Node {
    /** Unique identifier for this node. */
    public final String id;

    /** Entity type (e.g., "JoshuaTree", "Patch", "Simulation"). */
    public final String entityType;

    /** Attribute name (e.g., "age", "height"). */
    public final String attribute;

    /** Event name (e.g., "init", "step", "end"). */
    public final String event;

    /**
     * Primary line number in the Josh source file (first occurrence).
     * Can be null if not available. For backward compatibility.
     */
    public final Integer sourceLine;

    /**
     * Primary source code text that defines this node (first occurrence).
     * Can be null if not available. For backward compatibility.
     */
    public final String sourceText;

    /**
     * All source locations for this node.
     * May include multiple locations for conditional handlers.
     * This list is unmodifiable.
     */
    public final List<SourceLocation> allSources;

    /**
     * Creates a new node with source location information.
     *
     * @param id The unique identifier for this node
     * @param entityType The entity type
     * @param attribute The attribute name
     * @param event The event name
     * @param sourceLine The line number in the source file (can be null)
     * @param sourceText The source code text (can be null)
     */
    public Node(String id, String entityType, String attribute, String event,
        Integer sourceLine, String sourceText) {
      this(id, entityType, attribute, event, sourceLine, sourceText,
           sourceLine != null || sourceText != null
               ? Collections.singletonList(new SourceLocation(sourceLine, sourceText))
               : Collections.emptyList());
    }

    /**
     * Creates a new node with multiple source locations.
     *
     * @param id The unique identifier for this node
     * @param entityType The entity type
     * @param attribute The attribute name
     * @param event The event name
     * @param sourceLine The primary line number in the source file (can be null)
     * @param sourceText The primary source code text (can be null)
     * @param allSources All source locations for this node (for conditional handlers)
     */
    public Node(String id, String entityType, String attribute, String event,
        Integer sourceLine, String sourceText, List<SourceLocation> allSources) {
      this.id = id;
      this.entityType = entityType;
      this.attribute = attribute;
      this.event = event;
      this.sourceLine = sourceLine;
      this.sourceText = sourceText;
      this.allSources = Collections.unmodifiableList(new ArrayList<>(allSources));
    }
  }

  /**
   * Represents an edge in the dependency graph.
   *
   * <p>Each edge represents a dependency from one node to another, labeled with
   * the path expression that creates the dependency (e.g., "prior.age"). The edge
   * also includes metadata about how the dependency is resolved and a human-readable
   * description.</p>
   */
  public static class Edge {
    /** Source node ID. */
    public final String from;

    /** Target node ID. */
    public final String to;

    /** Dependency path label. */
    public final String label;

    /**
     * Type of dependency resolution.
     * One of: "temporal-prior", "temporal-current", "spatial-patch",
     * "spatial-collection", "simulation", "explicit-entity", "simple"
     */
    public final String resolutionType;

    /** Human-readable explanation of the dependency. */
    public final String description;

    /**
     * Creates a new edge.
     *
     * @param from The source node ID
     * @param to The target node ID
     * @param label The dependency path label
     * @param resolutionType The type of dependency resolution
     * @param description Human-readable explanation of the dependency
     */
    public Edge(String from, String to, String label, String resolutionType, String description) {
      this.from = from;
      this.to = to;
      this.label = label;
      this.resolutionType = resolutionType;
      this.description = description;
    }
  }
}
