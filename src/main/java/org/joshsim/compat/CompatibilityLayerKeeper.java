
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
   * This can only be done once; subsequent attempts will throw an exception.
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
   * Returns the current CompatabilityLayer implementation.
   * If no layer has been set, creates and returns an EmulatedCompatabilityLayer.
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
