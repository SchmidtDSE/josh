package org.joshsim.compat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


class CompatibilityLayerKeeperTest {

  @Test
  void testGetDefault() {
    CompatabilityLayer result = CompatibilityLayerKeeper.get();
    assertNotNull(result, "Returned layer should not be null");
  }

}