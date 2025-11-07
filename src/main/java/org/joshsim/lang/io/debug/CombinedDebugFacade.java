/**
 * Convenience facade to manage zero or more debug outputs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.io.ExportTarget;
import org.joshsim.lang.io.ExportTargetParser;

/**
 * Facade that manages debug facades per entity type.
 *
 * <p>Allows separate debug outputs for patch, organism, agent, etc., similar to how
 * CombinedExportFacade manages export outputs. If no debug file locations are specified,
 * this facade acts as a no-op with zero overhead.</p>
 */
public class CombinedDebugFacade {

  private final Map<String, DebugFacade> debugFacades;
  private final DebugFacadeFactory debugFactory;
  private final int replicateNumber;

  /**
   * Constructs a facade to manage debug operations across multiple entity types.
   *
   * @param simEntity the mutable entity representing the simulation context, used to retrieve
   *     and configure debug facades.
   * @param debugFactory The factory through which to build debug facades.
   */
  public CombinedDebugFacade(MutableEntity simEntity, DebugFacadeFactory debugFactory) {
    this.debugFactory = debugFactory;
    this.replicateNumber = debugFactory.getReplicateNumber();
    this.debugFacades = new HashMap<>();

    // Parse debug file configurations
    simEntity.startSubstep("constant");

    Optional<DebugFacade> patchFacade = getDebugFacade(simEntity, "debugFiles.patch");
    patchFacade.ifPresent(f -> debugFacades.put("patch", f));

    Optional<DebugFacade> organismFacade = getDebugFacade(simEntity, "debugFiles.organism");
    organismFacade.ifPresent(f -> debugFacades.put("organism", f));

    Optional<DebugFacade> agentFacade = getDebugFacade(simEntity, "debugFiles.agent");
    agentFacade.ifPresent(f -> debugFacades.put("agent", f));

    simEntity.endSubstep();
  }

  /**
   * Write debug message for a specific entity type.
   *
   * <p>If no debug facade is configured for the given entity type, this is a silent no-op
   * ensuring zero overhead when debugging is not enabled.</p>
   *
   * @param message The debug message to write.
   * @param step The simulation step number.
   * @param entityType The entity type name (e.g., "ForeverTree", "patch").
   */
  public void write(String message, long step, String entityType) {
    // Try to find facade for this entity type
    DebugFacade facade = debugFacades.get(entityType);
    if (facade != null) {
      facade.write(message, step, entityType, replicateNumber);
    }
    // Silent no-op if no facade configured (zero overhead)
  }

  /**
   * Start all debug facades.
   *
   * <p>Initiates the writer threads for all configured debug facades.</p>
   */
  public void start() {
    debugFacades.values().forEach(DebugFacade::start);
  }

  /**
   * Wait for all debug facades to complete.
   *
   * <p>Blocks until all pending debug messages have been written and all writer
   * threads have terminated.</p>
   */
  public void join() {
    debugFacades.values().forEach(DebugFacade::join);
  }

  /**
   * Check if any debug output is configured.
   *
   * @return true if at least one debug facade is configured, false otherwise.
   */
  public boolean isConfigured() {
    return !debugFacades.isEmpty();
  }

  /**
   * Retrieves a DebugFacade instance based on the given simulation entity and attribute.
   *
   * <p>This method attempts to retrieve the attribute value specified by the given
   * attribute name and constructs a DebugFacade if the attribute value is valid.
   * If no valid attribute value exists, an empty Optional is returned.</p>
   *
   * @param simEntity the mutable entity representing the simulation context from which
   *     the attribute value is retrieved and the DebugFacade is constructed.
   * @param attribute the name of the attribute used to determine the target debug configuration.
   * @return an Optional containing the constructed DebugFacade, or an empty Optional if
   *     the attribute value is not present or invalid.
   */
  private Optional<DebugFacade> getDebugFacade(MutableEntity simEntity, String attribute) {
    Optional<EngineValue> destination = simEntity.getAttributeValue(attribute);

    if (destination.isEmpty()) {
      return Optional.empty();
    }

    String templatePath = destination.get().getAsString();
    String path = debugFactory.getPath(templatePath);
    ExportTarget target = ExportTargetParser.parse(path);

    return Optional.of(debugFactory.build(target));
  }
}
