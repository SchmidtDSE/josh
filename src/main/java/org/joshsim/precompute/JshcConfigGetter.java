/**
 * Implementation of ConfigGetter that loads configuration from .jshc files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import org.joshsim.engine.config.Config;
import org.joshsim.engine.config.ConfigInputParser;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.bridge.ConfigGetter;
import org.joshsim.lang.io.InputGetterStrategy;

/**
 * ConfigGetter implementation that loads and caches Config objects from .jshc files.
 *
 * <p>This class uses an InputGetterStrategy to load .jshc files and parses them using
 * ConfigInputParser. Configs are cached to avoid repeated parsing of the same file.</p>
 */
public class JshcConfigGetter implements ConfigGetter {

  private final InputGetterStrategy inputStrategy;
  private final EngineValueFactory valueFactory;
  private final ConcurrentHashMap<String, Config> configCache;

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
  public Config getConfig(String name) {
    // Check if it's already cached
    Config cached = configCache.get(name);
    if (cached != null) {
      return cached;
    }

    // Ensure the name ends with .jshc
    String fileName = name;
    if (!fileName.endsWith(".jshc")) {
      fileName += ".jshc";
    }

    try (InputStream inputStream = inputStrategy.open(fileName)) {
      // Load the file content
      String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

      // Parse the config
      ConfigInputParser parser = new ConfigInputParser(valueFactory);
      Config config = parser.parse(content);

      // Cache it
      configCache.put(name, config);

      return config;
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to load config file: " + fileName, e);
    }
  }
}
