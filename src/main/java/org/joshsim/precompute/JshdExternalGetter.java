/**
 * Logic to get jshd external data within a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.io.IOException;
import java.io.InputStream;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.bridge.ExternalResourceGetter;
import org.joshsim.lang.io.InputGetterStrategy;


/**
 * Utility to read jshd files from InputStreams.
 */
public class JshdExternalGetter implements ExternalResourceGetter {

  private final InputGetterStrategy inputStrategy;
  private final EngineValueFactory valueFactory;

  /**
   * Create a new decorator around an input getter to read jshd files.
   *
   * @param inputStrategy The strategy to use in loading files from which jshd will be parsed.
   */
  public JshdExternalGetter(InputGetterStrategy inputStrategy) {
    this.inputStrategy = inputStrategy;
    this.valueFactory = CompatibilityLayerKeeper.get().getEngineValueFactory();
  }

  /**
   * Create a new decorator around an input getter to read jshd files.
   *
   * @param inputStrategy The strategy to use in loading files from which jshd will be parsed.
   * @param valueFactory The factory to use in making new EngineValues.
   */
  public JshdExternalGetter(InputGetterStrategy inputStrategy, EngineValueFactory valueFactory) {
    this.inputStrategy = inputStrategy;
    this.valueFactory = valueFactory;
  }

  @Override
  public DataGridLayer getResource(String name) {
    if (name.contains(".") && !name.endsWith(".jshd")) {
      throw new IllegalArgumentException("Can only open jshd files with this getter. Got " + name);
    }

    if (!name.endsWith(".jshd")) {
      name += ".jshd";
    }

    try (InputStream binaryInputStream = inputStrategy.open(name)) {
      byte[] fileBytes = binaryInputStream.readAllBytes();
      return JshdUtil.loadFromBytes(valueFactory, fileBytes);
    } catch (IOException e) {
      throw new RuntimeException("Failure in loading a jshd resource: " + e);
    }
  }

}
