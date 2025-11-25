/**
 * Thread-local dependency tracking for Josh programs.
 *
 * <p>This class provides a thread-safe mechanism to track dependencies during
 * program interpretation. It records when one node (e.g., "JoshuaTree.state.step")
 * depends on another node through a specific path (e.g., "prior.age").</p>
 *
 * <p>Tracking is opt-in via the {@link #enable()} method and uses ThreadLocal
 * storage to avoid global state and ensure thread safety.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.analyze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thread-local dependency tracking system.
 *
 * <p>Records dependencies between nodes during program interpretation. Each dependency
 * is stored as a mapping from "fromNode -> toPath" to a set of dependency paths that
 * connect them. This allows tracking multiple different paths that create the same
 * dependency relationship.</p>
 *
 * <p>Example usage:
 * <pre>
 * DependencyTracker.enable();
 * try {
 *   // During interpretation, record dependencies:
 *   DependencyTracker.recordDependency(
 *     "JoshuaTree.state.step",
 *     "prior.age",
 *     "prior.age"
 *   );
 *
 *   // Later, retrieve all tracked dependencies:
 *   Map&lt;String, Set&lt;String&gt;&gt; deps = DependencyTracker.getDependencies();
 * } finally {
 *   DependencyTracker.disable();
 * }
 * </pre>
 */
public class DependencyTracker {

  /**
   * Represents source location information for a node.
   */
  public static class NodeSourceInfo {
    /** Line number in the source file. */
    public final Integer sourceLine;

    /** The source code text. */
    public final String sourceText;

    /**
     * Creates a new NodeSourceInfo.
     *
     * @param sourceLine The line number in the source file
     * @param sourceText The source code text
     */
    public NodeSourceInfo(Integer sourceLine, String sourceText) {
      this.sourceLine = sourceLine;
      this.sourceText = sourceText;
    }
  }

  private static final ThreadLocal<DependencyTracker> INSTANCE = new ThreadLocal<>();

  private boolean enabled = false;
  private Map<String, Set<String>> dependencies = new HashMap<>();
  private Map<String, List<NodeSourceInfo>> nodeSources = new HashMap<>();

  /**
   * Enables dependency tracking for the current thread.
   *
   * <p>Creates a new DependencyTracker instance and stores it in ThreadLocal storage.
   * All subsequent calls to {@link #recordDependency} on this thread will be tracked
   * until {@link #disable()} is called.</p>
   */
  public static void enable() {
    DependencyTracker tracker = new DependencyTracker();
    tracker.enabled = true;
    INSTANCE.set(tracker);
  }

  /**
   * Disables dependency tracking for the current thread.
   *
   * <p>Removes the DependencyTracker instance from ThreadLocal storage, discarding
   * all tracked dependencies for this thread.</p>
   */
  public static void disable() {
    INSTANCE.remove();
  }

  /**
   * Checks if dependency tracking is currently enabled for this thread.
   *
   * @return true if tracking is enabled, false otherwise
   */
  public static boolean isEnabled() {
    DependencyTracker tracker = INSTANCE.get();
    return tracker != null && tracker.enabled;
  }

  /**
   * Records a dependency between two nodes.
   *
   * <p>If tracking is not enabled, this method does nothing. Otherwise, it records
   * that the "from" node depends on the "to" node through the specified path.</p>
   *
   * @param from The node ID that has the dependency (e.g., "JoshuaTree.state.step")
   * @param to The target of the dependency (can be a path like "prior.age")
   * @param path The specific path expression that creates this dependency
   */
  public static void recordDependency(String from, String to, String path) {
    if (!isEnabled()) {
      return;
    }

    DependencyTracker tracker = INSTANCE.get();
    String key = from + " -> " + to;
    tracker.dependencies.computeIfAbsent(key, k -> new HashSet<>()).add(path);
  }

  /**
   * Gets all tracked dependencies for the current thread.
   *
   * <p>Returns a copy of the dependency map to prevent external modification.
   * Each entry maps from "fromNode -> toPath" to a set of dependency paths.</p>
   *
   * @return A map of all tracked dependencies, or an empty map if tracking is disabled
   */
  public static Map<String, Set<String>> getDependencies() {
    if (!isEnabled()) {
      return Collections.emptyMap();
    }
    return new HashMap<>(INSTANCE.get().dependencies);
  }

  /**
   * Records source location information for a node.
   *
   * <p>If tracking is not enabled, this method does nothing. Otherwise, it appends
   * the source location (line number and text) to the list for the specified node ID.
   * This allows tracking multiple conditional handlers that create the same node.</p>
   *
   * @param nodeId The node ID (e.g., "JoshuaTree.age.step")
   * @param sourceLine The line number in the source file (can be null)
   * @param sourceText The source code text (can be null)
   */
  public static void recordNodeSource(String nodeId, Integer sourceLine, String sourceText) {
    if (!isEnabled()) {
      return;
    }

    DependencyTracker tracker = INSTANCE.get();
    if (sourceLine != null || sourceText != null) {
      tracker.nodeSources.computeIfAbsent(nodeId, k -> new ArrayList<>())
          .add(new NodeSourceInfo(sourceLine, sourceText));
    }
  }

  /**
   * Gets all tracked node source information for the current thread.
   *
   * <p>Returns a copy of the node source map to prevent external modification.
   * Each node ID maps to a list of source locations, allowing nodes with multiple
   * conditional handlers to track all their sources.</p>
   *
   * @return A map of node IDs to lists of source location information,
   *         or an empty map if tracking is disabled
   */
  public static Map<String, List<NodeSourceInfo>> getNodeSources() {
    if (!isEnabled()) {
      return Collections.emptyMap();
    }
    // Create a deep copy to prevent external modification
    Map<String, List<NodeSourceInfo>> copy = new HashMap<>();
    for (Map.Entry<String, List<NodeSourceInfo>> entry : INSTANCE.get().nodeSources.entrySet()) {
      copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return copy;
  }
}
