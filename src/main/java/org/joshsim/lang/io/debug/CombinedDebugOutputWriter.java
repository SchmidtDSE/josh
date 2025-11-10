/**
 * Adapter that wraps CombinedTextWriter to provide debug facade interface compatibility.
 *
 * <p>This class provides a bridge between the old DebugFacade interface and the new
 * OutputWriter&lt;String&gt; implementation. It allows the simulation engine to continue
 * using the DebugFacade API while internally delegating to the unified OutputWriter system.</p>
 *
 * <p>This adapter routes debug messages to different output files based on entity type,
 * using CombinedTextWriter internally. It maintains ThreadLocal context for entity type
 * routing.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.io.CombinedTextWriter;
import org.joshsim.lang.io.OutputWriter;
import org.joshsim.lang.io.OutputWriterFactory;

/**
 * Combined debug output using the new OutputWriter system.
 *
 * <p>This class manages debug output for multiple entity types by delegating to
 * CombinedTextWriter. It provides the same interface as CombinedDebugFacade but
 * uses the new unified output architecture internally.</p>
 */
public class CombinedDebugOutputWriter {

  private final CombinedTextWriter combinedWriter;
  private final Map<String, OutputWriter<String>> writersByEntityType;

  /**
   * Constructs a combined debug output writer.
   *
   * @param simEntity The mutable entity representing the simulation context
   * @param factory The factory through which to build output writers
   */
  public CombinedDebugOutputWriter(MutableEntity simEntity, OutputWriterFactory factory) {
    this.writersByEntityType = new HashMap<>();

    // Parse debug file configurations
    simEntity.startSubstep("constant");

    getDebugWriter(simEntity, "debugFiles.patch", factory)
        .ifPresent(w -> writersByEntityType.put("patch", w));

    getDebugWriter(simEntity, "debugFiles.organism", factory)
        .ifPresent(w -> writersByEntityType.put("organism", w));

    getDebugWriter(simEntity, "debugFiles.agent", factory)
        .ifPresent(w -> writersByEntityType.put("agent", w));

    simEntity.endSubstep();

    // Create the combined writer
    this.combinedWriter = new CombinedTextWriter(writersByEntityType);
  }

  /**
   * Write debug message for a specific entity type.
   *
   * <p>If no writer is configured for the given entity type, this is a silent no-op
   * ensuring zero overhead when debugging is not enabled.</p>
   *
   * @param message The debug message to write
   * @param step The simulation step number
   * @param entityType The entity type name (e.g., "ForeverTree", "patch")
   */
  public void write(String message, long step, String entityType) {
    combinedWriter.setCurrentEntityType(entityType);
    combinedWriter.write(message, step);
  }

  /**
   * Start all debug writers.
   *
   * <p>Initiates the writer threads for all configured debug writers.</p>
   */
  public void start() {
    combinedWriter.start();
  }

  /**
   * Wait for all debug writers to complete.
   *
   * <p>Blocks until all pending debug messages have been written and all writer
   * threads have terminated.</p>
   */
  public void join() {
    combinedWriter.join();
  }

  /**
   * Check if any debug output is configured.
   *
   * @return true if at least one debug writer is configured, false otherwise
   */
  public boolean isConfigured() {
    return combinedWriter.isConfigured();
  }

  /**
   * Get the underlying CombinedTextWriter for bridge getter injection.
   *
   * <p>This allows the bridge getter to access the combined writer directly
   * so that debug() function calls can produce output.</p>
   *
   * @return The underlying CombinedTextWriter
   */
  public CombinedTextWriter getCombinedWriter() {
    return combinedWriter;
  }

  /**
   * Retrieves a debug output writer based on the given simulation entity and attribute.
   *
   * @param simEntity The mutable entity representing the simulation context
   * @param attribute The name of the attribute used to determine the target debug configuration
   * @param factory The factory to create the output writer
   * @return An Optional containing the constructed OutputWriter, or an empty Optional
   */
  private Optional<OutputWriter<String>> getDebugWriter(MutableEntity simEntity,
                                                         String attribute,
                                                         OutputWriterFactory factory) {
    Optional<EngineValue> destination = simEntity.getAttributeValue(attribute);

    if (destination.isEmpty()) {
      return Optional.empty();
    }

    String templatePath = destination.get().getAsString();
    String path = factory.resolvePath(templatePath);

    return Optional.of(factory.createTextWriter(path));
  }
}
