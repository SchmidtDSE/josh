/**
 * Logic for a singleton offering access to a Compatibility layer.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.util.Optional;

/**
 * Singleton keeper for the platform-specific CompatibilityLayer implementation.
 *
 * <p>This class manages access to the platform-specific implementation of CompatibilityLayer,
 * ensuring only one instance is used throughout the application. If no layer is explicitly set,
 * it defaults to an EmulatedCompatibilityLayer.</p>
 */
public class CompatibilityLayerKeeper {

  private static Optional<CompatibilityLayer> layer = Optional.empty();

  /**
   * Sets the platform-specific CompatibilityLayer implementation.
   *
   * @param newLayer The CompatibilityLayer implementation to use
   * @throws IllegalStateException if a layer has already been set
   */
  public static void set(CompatibilityLayer newLayer) {
    layer = Optional.of(newLayer);
  }

  /**
   * Returns the current CompatibilityLayer implementation or a default.
   *
   * @return The current CompatibilityLayer implementation
   */
  public static CompatibilityLayer get() {
    if (!layer.isPresent()) {
      layer = Optional.of(new EmulatedCompatibilityLayer());
    }

    return layer.get();
  }

}
