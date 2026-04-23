/**
 * Logic to get jshdz external data within a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.io.IOException;
import java.io.InputStream;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.lang.bridge.ExternalResourceGetter;
import org.joshsim.lang.io.InputGetterStrategy;


/**
 * Utility to read jshdz files (XZ-compressed jshd) from InputStreams.
 *
 * <p>Mirrors {@link JshdExternalGetter} but decompresses XZ-wrapped {@code .jshdz} files
 * before parsing. Only usable on the JVM; not compiled to WebAssembly via TeaVM.</p>
 */
public class JshdZExternalGetter implements ExternalResourceGetter {

  private final InputGetterStrategy inputStrategy;
  private final ValueSupportFactory valueFactory;

  /**
   * Create a new decorator around an input getter to read jshdz files.
   *
   * @param inputStrategy The strategy to use in loading files from which jshdz will be parsed.
   * @param valueFactory The factory to use in making new EngineValues.
   */
  public JshdZExternalGetter(InputGetterStrategy inputStrategy, ValueSupportFactory valueFactory) {
    this.inputStrategy = inputStrategy;
    this.valueFactory = valueFactory;
  }

  @Override
  public DataGridLayer getResource(String name) {
    if (!name.endsWith(".jshdz")) {
      throw new IllegalArgumentException("Expected a .jshdz file name. Got: " + name);
    }

    GridSerializationStrategy strategy = new XzGridSerializationStrategy(
        new BinaryGridSerializationStrategy(valueFactory)
    );

    try (InputStream inputStream = inputStrategy.open(name)) {
      return strategy.deserialize(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Failure in loading a jshdz resource: " + e);
    }
  }

}
