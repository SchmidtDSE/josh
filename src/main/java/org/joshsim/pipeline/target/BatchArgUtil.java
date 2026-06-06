/**
 * Shared encoders for batch dispatch arguments.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import java.util.Map;
import java.util.stream.Collectors;


/**
 * Encoders for arguments shared between {@link KubernetesTarget} and {@link HttpBatchTarget}.
 *
 * <p>The encoding format is part of the contract between the dispatcher and the
 * pod-side {@code run-entrypoint.sh}, so it lives in one place rather than being
 * duplicated per target.</p>
 */
public final class BatchArgUtil {

  private BatchArgUtil() {
    // utility
  }

  /**
   * Encodes custom tags as a newline-delimited {@code key=value} string.
   *
   * <p>Used both as a K8s env var ({@code JOSH_CUSTOM_TAGS}) and as an HTTP form field
   * ({@code customTags}). The pod-side {@code run-entrypoint.sh} reads the env var and
   * emits one {@code --custom-tag=key=value} flag per non-empty line; the server-side
   * {@link org.joshsim.cloud.JoshSimBatchHandler} splits the form value the same way.
   * Newline delimiting matches {@code RunCommand}'s {@code String[] customTags} shape
   * and avoids needing a JSON parser ({@code jq}) in the JRE-only batch image.</p>
   *
   * <p>Insertion order is preserved when the input is a {@link java.util.LinkedHashMap},
   * which both call sites use. Empty input produces the empty string.</p>
   *
   * @param customTags Tag map; may be empty but must not be null.
   * @return Newline-delimited {@code key=value} entries, or empty string if the map is empty.
   */
  public static String encodeCustomTags(Map<String, String> customTags) {
    return customTags.entrySet().stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("\n"));
  }
}
