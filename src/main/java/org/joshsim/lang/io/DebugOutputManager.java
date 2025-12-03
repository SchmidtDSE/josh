/**
 * Manager for debug output streams.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Manages debug output streams for different entity types.
 *
 * <p>This manager handles writing debug messages to configured output destinations
 * based on entity types (patch, organism, simulation, etc.).</p>
 */
public class DebugOutputManager {

  private final Map<EntityType, Writer> writers;
  private final Map<EntityType, String> targets;

  /**
   * Create a new debug output manager from simulation configuration.
   *
   * @param simEntity The simulation entity containing debugFiles.* configuration
   */
  public DebugOutputManager(MutableEntity simEntity) {
    this.writers = new HashMap<>();
    this.targets = new HashMap<>();

    // Note: Assumes simulation entity is in constant substep or attributes are accessible
    // Initialize debug outputs based on simulation configuration
    initializeDebugOutput(simEntity, "debugFiles.patch", EntityType.PATCH);
    initializeDebugOutput(simEntity, "debugFiles.agent", EntityType.AGENT);
    initializeDebugOutput(simEntity, "debugFiles.simulation", EntityType.SIMULATION);
  }

  /**
   * Write a debug message for a specific entity type.
   *
   * @param entityType The type of entity generating the debug output
   * @param message The debug message to write
   * @param stepNumber The current simulation step number
   */
  public void writeDebug(EntityType entityType, String message, long stepNumber) {
    Writer writer = writers.get(entityType);
    if (writer != null) {
      try {
        writer.write(String.format("[step=%d] %s%n", stepNumber, message));
        writer.flush();
      } catch (IOException e) {
        System.err.println("Error writing debug output: " + e.getMessage());
      }
    }
  }

  /**
   * Close all debug output streams.
   */
  public void close() {
    for (Writer writer : writers.values()) {
      try {
        writer.close();
      } catch (IOException e) {
        System.err.println("Error closing debug output: " + e.getMessage());
      }
    }
    writers.clear();
  }

  /**
   * Initialize debug output for a specific entity type.
   *
   * @param simEntity The simulation entity
   * @param attributeName The attribute name (e.g., "debugFiles.patch")
   * @param entityType The entity type to configure
   */
  private void initializeDebugOutput(MutableEntity simEntity, String attributeName,
      EntityType entityType) {
    Optional<EngineValue> targetMaybe = simEntity.getAttributeValue(attributeName);

    if (targetMaybe.isEmpty()) {
      return;  // No debug output configured for this entity type
    }

    String target = targetMaybe.get().getAsString();

    // Remove surrounding quotes if present
    if (target.startsWith("\"") && target.endsWith("\"")) {
      target = target.substring(1, target.length() - 1);
    }

    targets.put(entityType, target);

    try {
      Writer writer = createWriter(target);
      writers.put(entityType, writer);
    } catch (IOException e) {
      System.err.println("Failed to create debug output for " + attributeName + ": "
          + e.getMessage());
    }
  }

  /**
   * Create a writer for the specified target URI.
   *
   * @param target The target URI (file://, stdout://, etc.)
   * @return Writer for the target
   * @throws IOException if writer creation fails
   */
  private Writer createWriter(String target) throws IOException {
    if (target.startsWith("file://")) {
      String path = target.substring(7);
      return new BufferedWriter(new FileWriter(path, true));  // append mode
    } else if (target.equals("stdout://")) {
      return new BufferedWriter(new OutputStreamWriter(System.out));
    } else if (target.equals("stderr://")) {
      return new BufferedWriter(new OutputStreamWriter(System.err));
    } else {
      throw new IllegalArgumentException("Unsupported debug target: " + target);
    }
  }
}
