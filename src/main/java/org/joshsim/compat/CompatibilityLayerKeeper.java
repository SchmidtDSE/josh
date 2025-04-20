/**
 * Logic for a singleton offering access to a compatability layer.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.util.Optional;

/**
 * Singleton keeper for the platform-specific CompatabilityLayer implementation.
 *
 * <p>This class manages access to the platform-specific implementation of CompatabilityLayer,
 * ensuring only one instance is used throughout the application. If no layer is explicitly set,
 * it defaults to an EmulatedCompatabilityLayer.</p>
 */
public class CompatibilityLayerKeeper {

  private static Optional<CompatabilityLayer> layer = Optional.empty();

  /**
   * Sets the platform-specific CompatabilityLayer implementation.
   *
   * @param newLayer The CompatabilityLayer implementation to use
   * @throws IllegalStateException if a layer has already been set
   */
  public static void set(CompatabilityLayer newLayer) {
    if (layer.isPresent()) {
      throw new IllegalStateException("Layer already set");
    }

    layer = Optional.of(newLayer);
  }

  /**
   * Returns the current CompatabilityLayer implementation or a default.
   *
   * @return The current CompatabilityLayer implementation
   */
  public static CompatabilityLayer get() {
    if (!layer.isPresent()) {
      layer = Optional.of(new EmulatedCompatabilityLayer());
    }

    return layer.get();
  }

}
