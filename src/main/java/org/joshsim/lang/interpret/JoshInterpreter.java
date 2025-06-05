/**
 * Structures for interpreting a Josh source.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Iterator;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.parse.ParseResult;


/**
 * Strategy to interpret a Josh program into a single JoshProgram.
 */
public class JoshInterpreter {

  /**
   * Interpret a Josh source into a JoshProgram.
   *
   * @param parseResult The result of parsing to interpret.
   * @param valueFactory Factory through which to build engine values.
   * @param geometryFactory Factory through which to build engine geometries
   * @param inputOutputLayer Layer to use in interacting with external files.
   * @return Parsed simulations.
   */
  public JoshProgram interpret(ParseResult parseResult, EngineValueFactory valueFactory,
        EngineGeometryFactory geometryFactory, InputOutputLayer inputOutputLayer) {
    if (parseResult.hasErrors()) {
      throw new RuntimeException("Cannot interpret program with parse errors.");
    }

    FutureBridgeGetter bridgeGetter = new FutureBridgeGetter(valueFactory);

    JoshParserToMachineVisitor visitor = new JoshParserToMachineVisitor(valueFactory, bridgeGetter);
    Fragment fragment = visitor.visit(parseResult.getProgram().orElseThrow());

    JoshProgram program = fragment.getProgram();
    bridgeGetter.setProgram(program);
    bridgeGetter.setGeometryFactory(geometryFactory);
    bridgeGetter.setInputOutputLayer(inputOutputLayer);

    EngineBridgeSimulationStore simulationStore = program.getSimulations();
    Iterator<String> simulationNames = simulationStore.getSimulations().iterator();
    if (simulationNames.hasNext()) {
      bridgeGetter.setSimulationName(simulationNames.next());
    }

    return program;
  }

  /**
   * Interpret a Josh source into a JoshProgram.
   *
   * @param parseResult The result of parsing to interpret.
   * @param simulationName The name of the simulation to be executed.
   * @param valueFactory Factory through which to build engine values.
   * @param geometryFactory Factory through which to build engine geometries.
   * @param inputOutputLayer Layer to use in interacting with external files.
   * @return Parsed simulations.
   */
  public JoshProgram interpret(ParseResult parseResult, String simulationName,
        EngineValueFactory valueFactory, EngineGeometryFactory geometryFactory,
        InputOutputLayer inputOutputLayer) {
    if (parseResult.hasErrors()) {
      throw new RuntimeException("Cannot interpret program with parse errors.");
    }

    FutureBridgeGetter bridgeGetter = new FutureBridgeGetter(valueFactory);

    JoshParserToMachineVisitor visitor = new JoshParserToMachineVisitor(valueFactory, bridgeGetter);
    Fragment fragment = visitor.visit(parseResult.getProgram().orElseThrow());

    JoshProgram program = fragment.getProgram();
    bridgeGetter.setProgram(program);
    bridgeGetter.setSimulationName(simulationName);
    bridgeGetter.setGeometryFactory(geometryFactory);
    bridgeGetter.setInputOutputLayer(inputOutputLayer);

    return program;
  }

}
