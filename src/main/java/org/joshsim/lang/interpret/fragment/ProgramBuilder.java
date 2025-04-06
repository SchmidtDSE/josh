
/**
 * Builder for creating program fragments.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.prototype.EntityPrototypeStoreBuilder;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.value.converter.ConverterBuilder;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;
import org.joshsim.lang.interpret.JoshProgram;

/**
 * Builder for constructing programs from fragments.
 *
 * <p>This class provides a builder pattern implementation for creating program fragments
 * by aggregating conversions, entities, and simulation components into a complete program.</p>
 */
public class ProgramBuilder {

  private final ConverterBuilder converter;
  private final EntityPrototypeStoreBuilder entities;
  private final List<String> simulationNames;

  /**
   * Creates a new builder for program fragments with empty conversion and entity stores.
   */
  public ProgramBuilder() {
    converter = new ConverterBuilder();
    entities = new EntityPrototypeStoreBuilder();
    simulationNames = new ArrayList<>();
  }

  /**
   * Adds a fragment to the program being built.
   *
   * @param fragment The fragment to add to the program
   * @throws IllegalArgumentException if the fragment type is not supported at the top level
   */
  public void add(Fragment fragment) {
    switch (fragment.getFragmentType()) {
      case CONVERSIONS -> fragment.getConversions().forEach(converter::addConversion);
      case ENTITY -> addEntity(fragment.getEntity());
      default -> throw new IllegalArgumentException(
          "Unexpected top level fragment: " + fragment.getFragmentType()
      );
    }
  }

  /**
   * Builds and returns the complete program.
   *
   * @return The constructed JoshProgram instance
   */
  public JoshProgram build() {
    return new JoshProgram(converter.build(), buildSimulationStore(), entities.build());
  }

  /**
   * Adds an entity to the program and tracks simulation entities separately.
   *
   * @param entity The entity prototype to add
   */
  private void addEntity(EntityPrototype entity) {
    entities.add(entity);

    if (entity.getEntityType() == EntityType.SIMULATION) {
      simulationNames.add(entity.getIdentifier());
    }
  }

  private EngineBridgeSimulationStore buildSimulationStore() {
    Map<String, EntityPrototype> prototypes = new HashMap<>();

    for (String simulationName : simulationNames) {
      EntityPrototype prototype = entities.get(simulationName);
      prototypes.put(simulationName, prototype);
    }

    return new EngineBridgeSimulationStore(prototypes);
  }

}
