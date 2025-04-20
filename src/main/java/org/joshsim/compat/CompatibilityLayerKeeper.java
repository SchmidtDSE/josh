package org.joshsim.compat;

import java.util.Optional;


public class CompatibilityLayerKeeper {

  private static Optional<CompatabilityLayer> layer = Optional.empty();

  public static void set(CompatabilityLayer newLayer) {
    if (layer.isPresent()) {
      throw new IllegalStateException("Layer already set");
    }

    layer = Optional.of(newLayer);
  }

  public static CompatabilityLayer get() {
    if (!layer.isPresent()) {
      layer = Optional.of(new EmulatedCompatabilityLayer());
    }

    return layer.get();
  }

}
