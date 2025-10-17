/**
 * Implementation of ConfigGetter that loads configuration from .jshc files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.bridge.ConfigGetter;
import org.joshsim.lang.interpret.ConfigInterpreter;
import org.joshsim.lang.io.InputGetterStrategy;

/**
 * ConfigGetter implementation that loads and caches Config objects from .jshc files.
 *
 * <p>This class uses an InputGetterStrategy to load .jshc files and parses them using
 * ConfigInterpreter. Configs are cached to avoid repeated parsing of the same file.</p>
 */
public class JshcConfigGetter implements ConfigGetter {

  private final InputGetterStrategy inputStrategy;
  private final EngineValueFactory valueFactory;
  private final ConcurrentHashMap<String, Optional<Config>> configCache;

  /**
   * Creates a new JshcConfigGetter with the specified input strategy and value factory.
   *
   * @param inputStrategy Strategy for loading input files
   * @param valueFactory Factory for creating engine values
   */
  public JshcConfigGetter(InputGetterStrategy inputStrategy, EngineValueFactory valueFactory) {
    this.inputStrategy = inputStrategy;
    this.valueFactory = valueFactory;
    this.configCache = new ConcurrentHashMap<>();
  }

  @Override
  public Optional<Config> getConfig(String name) {
    // Check if it's already cached (including cached empty results)
    if (configCache.containsKey(name)) {
      return configCache.get(name);
    }

    // Use the name directly without appending extension
    // The caller (MinimalEngineBridge) is responsible for providing the full filename
    String fileName = name;

    // Check if the file exists before trying to open it
    if (!inputStrategy.exists(fileName)) {
      // Cache the empty result to avoid repeated checks
      configCache.put(name, Optional.empty());
      return Optional.empty();
    }

    try (InputStream inputStream = inputStrategy.open(fileName)) {
      // Load the file content
      String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

      // Parse the config
      ConfigInterpreter interpreter = new ConfigInterpreter();
      Config config = interpreter.interpret(content, valueFactory);

      // Cache it
      Optional<Config> result = Optional.of(config);
      configCache.put(name, result);

      return result;
    } catch (IOException | RuntimeException e) {
      // If there's an error reading or parsing, cache empty and return empty
      configCache.put(name, Optional.empty());
      return Optional.empty();
    }
  }
}
