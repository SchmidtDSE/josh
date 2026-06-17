/**
 * Shared resolver for which replicate indices a run should compute.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Single source of truth for turning replicate selection knobs into a concrete, ordered list of
 * absolute replicate indices.
 *
 * <p>Every execution route (local {@code run}, {@code runRemote} local- and offload-leader, and
 * {@code batchRemote} via Kubernetes or HTTP) ultimately needs the same thing: the ordered list of
 * absolute replicate indices to compute. Historically each route expanded {@code (replicates,
 * replicateStart)} into the half-open range {@code [start, start+count)} on its own. This utility
 * centralizes that expansion and adds an explicit-index-list mode, so an arbitrary, possibly
 * non-contiguous set (e.g. {@code 3,7,8}) can be dispatched identically everywhere.</p>
 *
 * <p>The explicit list is always optional: when absent, {@link #resolve} reproduces the existing
 * range behavior byte-for-byte, so callers that never set indices are unaffected.</p>
 */
public final class ReplicateSelection {

  private ReplicateSelection() {
    // Prevent instantiation of utility class
  }

  /**
   * Parses a comma-separated list of replicate indices.
   *
   * <p>Empty/null/whitespace-only input yields {@link Optional#empty()} (meaning "use the range").
   * Otherwise the indices are returned in the order written, after validating that each is
   * non-negative and that none repeats.</p>
   *
   * @param csv comma-separated indices (e.g. {@code "3,7,8"}), or null/empty for none
   * @return the ordered indices, or empty if none were given
   * @throws IllegalArgumentException if the format is invalid, an index is negative, or an index
   *     repeats
   */
  public static Optional<List<Integer>> parse(String csv) {
    if (csv == null || csv.trim().isEmpty()) {
      return Optional.empty();
    }
    List<Integer> indices;
    try {
      indices = Arrays.stream(csv.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(Integer::parseInt)
          .collect(Collectors.toList());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid replicate-indices format: " + csv
          + ". Expected comma-separated non-negative integers (e.g., '3,7,8')");
    }
    if (indices.isEmpty()) {
      return Optional.empty();
    }
    validate(indices);
    return Optional.of(indices);
  }

  /**
   * Validates an explicit index list: every index must be non-negative and unique.
   *
   * @param indices the indices to validate
   * @throws IllegalArgumentException if an index is negative or repeats
   */
  public static void validate(List<Integer> indices) {
    for (int idx : indices) {
      if (idx < 0) {
        throw new IllegalArgumentException("replicate-indices must be >= 0, got: " + idx);
      }
    }
    LinkedHashSet<Integer> seen = new LinkedHashSet<>(indices);
    if (seen.size() != indices.size()) {
      throw new IllegalArgumentException("replicate-indices must be unique, got: " + indices);
    }
  }

  /**
   * Resolves the ordered list of absolute replicate indices to compute.
   *
   * @param indices an explicit, already-validated index list, or empty to use the range
   * @param start the starting index of the half-open range {@code [start, start+count)}, used only
   *     when {@code indices} is empty
   * @param count the number of replicates in the range, used only when {@code indices} is empty
   * @return the ordered list of absolute indices
   */
  public static List<Integer> resolve(Optional<List<Integer>> indices, int start, int count) {
    if (indices.isPresent()) {
      return indices.get();
    }
    List<Integer> range = new ArrayList<>(Math.max(count, 0));
    for (int i = 0; i < count; i++) {
      range.add(start + i);
    }
    return range;
  }

  /**
   * Reports whether an index list is the default dense range {@code [0, size)} — i.e.
   * {@code 0, 1, ..., size-1} in order.
   *
   * <p>Wire formats use this to stay byte-for-byte compatible: when the selection is the default
   * range, no explicit index field is emitted, exactly as before this option existed.</p>
   *
   * @param indices the indices to check
   * @return true if {@code indices} equals {@code [0, indices.size())} in order
   */
  public static boolean isDefaultRange(List<Integer> indices) {
    for (int i = 0; i < indices.size(); i++) {
      if (indices.get(i) != i) {
        return false;
      }
    }
    return true;
  }

  /**
   * Renders an index list back to the canonical comma-separated wire form (e.g. {@code "3,7,8"}).
   *
   * @param indices the indices to render
   * @return the comma-separated representation
   */
  public static String toCsv(List<Integer> indices) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < indices.size(); i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append(indices.get(i));
    }
    return builder.toString();
  }
}
