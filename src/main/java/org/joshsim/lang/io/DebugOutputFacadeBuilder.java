/**
 * Builder for creating CombinedDebugOutputFacade from simulation entity configuration.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Builds a CombinedDebugOutputFacade by reading debugFiles.* attributes from simulation entity.
 *
 * <p>This builder follows the same pattern as {@link CombinedExportFacade} for reading
 * configuration from simulation entities. It reads attributes like:</p>
 * <ul>
 *   <li>debugFiles.organism - output path for organism debug messages</li>
 *   <li>debugFiles.patch - output path for patch debug messages</li>
 *   <li>debugFiles.agent - output path for agent debug messages</li>
 * </ul>
 *
 * <p>Supported path schemes depend on the ExportFacadeFactory implementation:</p>
 * <ul>
 *   <li>file:// - writes to local file system (JVM only)</li>
 *   <li>minio:// - writes to MinIO object storage (JVM only, requires MinIO configuration)</li>
 *   <li>stdout:// - writes to standard output</li>
 *   <li>memory://editor/debug - writes to browser console via callback (WASM only)</li>
 * </ul>
 *
 * <p>Example usage in Josh simulation for WASM/browser:</p>
 * <pre>
 * start simulation Example
 *   debugFiles.organism = "memory://editor/debug"
 * end simulation
 * </pre>
 *
 * <p>Example usage in Java:</p>
 * <pre>
 * CombinedDebugOutputFacade debugFacade = DebugOutputFacadeBuilder.build(simEntity, factory);
 * debugFacade.start();
 * // ... simulation runs ...
 * debugFacade.join();
 * </pre>
 */
public class DebugOutputFacadeBuilder {

  private static final String DEBUG_FILES_PREFIX = "debugFiles.";
  private static final String[] ENTITY_TYPES = {"organism", "patch", "agent", "disturbance"};

  /**
   * Builds a CombinedDebugOutputFacade from the simulation entity's configuration.
   *
   * <p>Uses the ExportFacadeFactory to create OutputStreamStrategy instances, allowing
   * debug output to use the same infrastructure (file, MinIO) as export output.</p>
   *
   * @param simEntity The simulation entity containing debugFiles.* attributes.
   * @param exportFactory The export factory to use for creating output stream strategies.
   * @return A CombinedDebugOutputFacade configured based on the entity's attributes.
   */
  public static CombinedDebugOutputFacade build(MutableEntity simEntity,
                                                 ExportFacadeFactory exportFactory) {
    Map<String, DebugOutputFacade> facades = new HashMap<>();

    simEntity.startSubstep("constant");

    for (String entityType : ENTITY_TYPES) {
      String attributeName = DEBUG_FILES_PREFIX + entityType;
      Optional<EngineValue> value = simEntity.getAttributeValue(attributeName);

      if (value.isPresent()) {
        try {
          String templatePath = value.get().getAsString();
          String path = exportFactory.getPath(templatePath);
          OutputStreamStrategy strategy = createStrategyFromPath(path, exportFactory);
          facades.put(entityType, new DebugOutputFacade(strategy));
        } catch (Exception e) {
          // Skip this entity type if strategy creation fails (e.g., in web environment)
          // Debug output will be silently ignored for this entity type
        }
      }
    }

    simEntity.endSubstep();

    return new CombinedDebugOutputFacade(facades);
  }

  /**
   * Creates an OutputStreamStrategy from a path string.
   *
   * <p>Handles stdout:// specially, memory:// for sandbox environments (WASM), and delegates
   * to JvmExportFacadeFactory for file:// and minio://.</p>
   *
   * @param path The path string (e.g., "file:///tmp/debug.txt", "memory://editor/debug", etc).
   * @param exportFactory The export factory to use for creating strategies.
   * @return The appropriate OutputStreamStrategy for the path.
   * @throws IllegalArgumentException if the path scheme is unsupported or factory doesn't support
   *     debug output.
   */
  private static OutputStreamStrategy createStrategyFromPath(String path,
                                                              ExportFacadeFactory exportFactory) {
    String cleanPath = path.replaceAll("\"", "");

    if (cleanPath.startsWith("stdout://") || cleanPath.equals("stdout")) {
      return new StdoutOutputStreamStrategy();
    }

    ExportTarget target = ExportTargetParser.parse(cleanPath);

    // Handle memory:// protocol for sandbox environments (WASM)
    if (target.getProtocol().equals("memory")) {
      if (exportFactory instanceof SandboxExportFacadeFactory sandboxFactory) {
        return sandboxFactory.createDebugOutputStreamStrategy();
      }
      // memory:// in JVM environment - just ignore (no output)
      throw new UnsupportedOperationException(
          "memory:// debug output is only supported in sandbox/WASM environments. "
          + "Use file:// or stdout:// for debug output in JVM environments."
      );
    }

    // Only JvmExportFacadeFactory supports file-based debug output
    // Other factories (e.g., SandboxExportFacadeFactory for WASM) don't support file I/O
    if (exportFactory instanceof JvmExportFacadeFactory jvmFactory) {
      return jvmFactory.createOutputStreamStrategy(target);
    }

    // Non-JVM environments don't support debug file output
    throw new UnsupportedOperationException(
        "Debug file output is only supported in JVM environments. "
        + "Use memory://editor/debug for WASM, stdout:// for console, "
        + "or remove debugFiles configuration."
    );
  }

}
