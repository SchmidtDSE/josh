/**
 * Parser for dependency paths in Josh programs.
 *
 * <p>This class parses dependency path expressions (e.g., "prior.age", "here.fire",
 * "meta.isMastYear") and resolves them to target nodes within the dependency graph.
 * It handles various path types including temporal, spatial, cross-entity, and
 * simulation-level references.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.analyze;

/**
 * Parses dependency paths and resolves them to target nodes.
 *
 * <p>Supports the following path patterns:
 * <ul>
 *   <li>Simple paths: "age" → same entity, same event</li>
 *   <li>Temporal: "prior.age", "current.height" → same entity, same event</li>
 *   <li>Spatial (patch): "here.fire" → Patch entity, same event</li>
 *   <li>Spatial (collection): "here.Trees.age" → Trees collection, same event</li>
 *   <li>Simulation: "meta.isMastYear" → Simulation entity, same event</li>
 *   <li>Explicit entity: "JoshuaTrees.isFlowering" → explicit entity reference</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * DependencyInfo info = DependencyPathParser.parse(
 *   "prior.age",
 *   "JoshuaTree",
 *   "step"
 * );
 * // Returns: targetNodeId="JoshuaTree.age.step",
 * //          targetEntity="JoshuaTree",
 * //          targetAttribute="age",
 * //          targetEvent="step"
 * </pre>
 */
public class DependencyPathParser {

  /**
   * Parses a dependency path and returns information about the target node.
   *
   * <p>The parser examines the path structure to determine what entity and attribute
   * it refers to. For simple paths, it assumes the same entity and event as the
   * current context. For qualified paths, it resolves the entity based on the
   * prefix (prior, current, here, meta, or explicit entity name).</p>
   *
   * @param path The dependency path to parse (e.g., "prior.age", "here.fire")
   * @param currentEntity The entity context where this dependency appears
   * @param currentEvent The event context where this dependency appears
   * @return DependencyInfo containing the resolved target node information
   */
  public static DependencyInfo parse(String path, String currentEntity, String currentEvent) {
    String[] parts = path.split("\\.");

    // Simple path: "age" -> same entity, same event
    if (parts.length == 1) {
      String resolutionType = "simple";
      String description = String.format("Reads '%s' from this %s's current state",
          parts[0], currentEntity);
      return new DependencyInfo(
          String.format("%s.%s.%s", currentEntity, parts[0], currentEvent),
          currentEntity,
          parts[0],
          currentEvent,
          resolutionType,
          description
      );
    }

    String first = parts[0];

    // Temporal: "prior.age", "current.height"
    // These refer to the same entity's attributes in temporal context
    if (first.equals("prior")) {
      String resolutionType = "temporal-prior";
      String description = String.format("Reads '%s' from this %s's previous timestep state",
          parts[1], currentEntity);
      return new DependencyInfo(
          String.format("%s.%s.%s", currentEntity, parts[1], currentEvent),
          currentEntity,
          parts[1],
          currentEvent,
          resolutionType,
          description
      );
    }

    if (first.equals("current")) {
      String resolutionType = "temporal-current";
      String description = String.format(
          "Reads '%s' from this %s's current state (same timestep)",
          parts[1], currentEntity);
      return new DependencyInfo(
          String.format("%s.%s.%s", currentEntity, parts[1], currentEvent),
          currentEntity,
          parts[1],
          currentEvent,
          resolutionType,
          description
      );
    }

    // Spatial: "here.maxTemperature" or "here.Trees.age"
    if (first.equals("here")) {
      if (parts.length == 2) {
        // Patch attribute: "here.fire"
        String resolutionType = "spatial-patch";
        String description = String.format("Reads '%s' from the Patch where this %s is located",
            parts[1], currentEntity);
        return new DependencyInfo(
            String.format("Patch.%s.%s", parts[1], currentEvent),
            "Patch",
            parts[1],
            currentEvent,
            resolutionType,
            description
        );
      } else {
        // Organism collection: "here.Trees.age"
        String collectionName = parts[1];
        String attribute = parts[2];
        String resolutionType = "spatial-collection";
        String description = String.format("Reads '%s' from %s organisms in this Patch",
            attribute, collectionName);
        return new DependencyInfo(
            String.format("%s.%s.%s", collectionName, attribute, currentEvent),
            collectionName,
            attribute,
            currentEvent,
            resolutionType,
            description
        );
      }
    }

    // Simulation: "meta.isMastYear"
    if (first.equals("meta")) {
      String resolutionType = "simulation";
      String description = String.format("Reads '%s' from simulation-level attributes",
          parts[1]);
      return new DependencyInfo(
          String.format("Simulation.%s.%s", parts[1], currentEvent),
          "Simulation",
          parts[1],
          currentEvent,
          resolutionType,
          description
      );
    }

    // Explicit entity: "JoshuaTrees.isFlowering"
    // Default case: treat first part as entity name, second as attribute
    String resolutionType = "explicit-entity";
    String description = String.format("Reads '%s' from %s entities",
        parts[1], parts[0]);
    return new DependencyInfo(
        String.format("%s.%s.%s", parts[0], parts[1], currentEvent),
        parts[0],
        parts[1],
        currentEvent,
        resolutionType,
        description
    );
  }

  /**
   * Information about a resolved dependency target.
   *
   * <p>Contains all the information needed to identify the target node of a
   * dependency, including its full ID, entity type, attribute name, event, and
   * metadata about how the dependency is resolved.</p>
   */
  public static class DependencyInfo {
    /** Full node ID (e.g., "JoshuaTree.age.step"). */
    public final String targetNodeId;

    /** Target entity type. */
    public final String targetEntity;

    /** Target attribute name. */
    public final String targetAttribute;

    /** Target event name. */
    public final String targetEvent;

    /**
     * Type of dependency resolution.
     * One of: "temporal-prior", "temporal-current", "spatial-patch",
     * "spatial-collection", "simulation", "explicit-entity", "simple"
     */
    public final String resolutionType;

    /** Human-readable explanation of the dependency. */
    public final String description;

    /**
     * Creates a new DependencyInfo instance.
     *
     * @param nodeId The full target node ID
     * @param entity The target entity type
     * @param attr The target attribute name
     * @param event The target event name
     * @param resolutionType The type of dependency resolution
     * @param description Human-readable explanation of the dependency
     */
    public DependencyInfo(String nodeId, String entity, String attr, String event,
        String resolutionType, String description) {
      this.targetNodeId = nodeId;
      this.targetEntity = entity;
      this.targetAttribute = attr;
      this.targetEvent = event;
      this.resolutionType = resolutionType;
      this.description = description;
    }
  }
}
