/**
 * Dependency analysis and graph extraction for Josh programs.
 *
 * <p>This package provides tools for tracking, analyzing, and visualizing
 * dependencies between attributes in Josh programs. It enables ecologists
 * and developers to understand how attributes depend on each other, including
 * cross-entity dependencies and event ordering.</p>
 *
 * <p>Key components:
 * <ul>
 *   <li>{@link org.joshsim.lang.analyze.DependencyTracker} - Thread-local tracking system</li>
 *   <li>{@link org.joshsim.lang.analyze.DependencyGraph} - Graph data structure</li>
 *   <li>{@link org.joshsim.lang.analyze.DependencyPathParser} - Path parsing with
 *       cross-entity support</li>
 *   <li>{@link org.joshsim.lang.analyze.DependencyGraphExtractor} - Graph extraction
 *       from programs</li>
 *   <li>{@link org.joshsim.lang.analyze.JsonExporter} - JSON export for visualization</li>
 * </ul>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.analyze;
