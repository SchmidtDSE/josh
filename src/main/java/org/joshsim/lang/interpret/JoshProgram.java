package org.joshsim.lang.interpret;

import org.joshsim.engine.value.Converter;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;


public class JoshProgram {

  private final Converter converter;
  private final EngineBridgeSimulationStore simulations;

  public JoshProgram(Converter converter, EngineBridgeSimulationStore simulations) {
    this.converter = converter;
    this.simulations = simulations;
  }

  public Converter getConverter() {
    return converter;
  }

  public EngineBridgeSimulationStore getSimulations() {
    return simulations;
  }

}
