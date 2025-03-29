package org.joshsim.lang.interpret.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joshsim.engine.entity.EntityType;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.prototype.EntityPrototypeStoreBuilder;
import org.joshsim.engine.value.ConverterBuilder;
import org.joshsim.lang.bridge.EngineBridgeOperation;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;
import org.joshsim.lang.interpret.JoshProgram;


public class ProgramFragmentBuilder {

  private final ConverterBuilder converter;
  private final EntityPrototypeStoreBuilder entities;
  private final List<String> simulationNames;

  public ProgramFragmentBuilder() {
    converter = new ConverterBuilder();
    entities = new EntityPrototypeStoreBuilder();
    simulationNames = new ArrayList<>();
  }

  public void add(Fragment fragment) {
    switch (fragment.getFragmentType()) {
      case CONVERSIONS -> fragment.getConversions().forEach(converter::addConversion);
      case ENTITY -> addEntity(fragment.getEntity());
      default -> throw new IllegalArgumentException(
          "Unexpected top level fragment: " + fragment.getFragmentType()
      );
    }
  }

  public JoshProgram build() {
    return new JoshProgram(converter.build(), buildSimulationStore());
  }

  private EngineBridgeSimulationStore buildSimulationStore() {
    Map<String, EngineBridgeOperation> simulationSteps = new HashMap<>();

    for (String simulationName : simulationNames) {
      EntityPrototype prototype = entities.get(simulationName);
    }

    return null;  // TODO
  }

  private void addEntity(EntityPrototype entity) {
    entities.add(entity);

    if (entity.getEntityType() == EntityType.SIMULATION) {
      simulationNames.add(entity.getIdentifier());
    }
  }

}
