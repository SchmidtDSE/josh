/**
 * Facade for interacting with the Josh platform via code.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.bridge.QueryCacheEngineBridge;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.bridge.SimulationStepper;
import org.joshsim.lang.interpret.JoshInterpreter;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.parse.JoshParser;
import org.joshsim.lang.parse.ParseResult;


/**
 * Entry point into the Josh platform when used as a library.
 *
 * <p>Facade which helps facilitate common operations within the Josh simulation platform when used
 * as a library as opposed to as an interactive / command-line tool.</p>
 */
public class JoshSimFacade {

  /**
   * Parse a Josh script.
   *
   * <p>Parse a Josh script such as to to check for syntax errors or generate an AST in support of
   * developer tools.</p>
   *
   * @param code String code to parse as a Josh source.
   * @return The result of parsing the code where hasErrors and getErrors can report on syntax
   *     issues found.
   */
  public static ParseResult parse(String code) {
    JoshParser parser = new JoshParser();
    return parser.parse(code);
  }

  /**
   * Interpret a parsed Josh script to Java objects which can run the simulation.
   *
   * @param parsed The result of parsing the Josh source successfully.
   * @return The parsed JoshProgram which can be used to run a specific simulation.
   */
  public static JoshProgram interpret(ParseResult parsed) {
    JoshInterpreter interpreter = new JoshInterpreter();
    return interpreter.interpret(parsed);
  }

  /**
   * Runs a simulation from the provided program.
   *
   * <p>Creates and executes a simulation using the provided program and simulation name.
   * The callback is invoked after each simulation step is completed.</p>
   *
   * @param program The Josh program containing the simulation to run. This is the program in which
   *     the simulation will be initalized.
   * @param simulationName The name of the simulation to execute from the program. This will be
   *     initalized from the given program.
   * @param callback A callback that will be invoked after each simulation step. This is called
   *     as blocking.
   */
  public static void runSimulation(JoshProgram program, String simulationName,
      SimulationStepCallback callback) {
    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulationName).build();
    MutableEntity simEntity = new ShadowingEntity(simEntityRaw, simEntityRaw);
    EngineBridge bridge = new QueryCacheEngineBridge(
        simEntity,
        program.getConverter(),
        program.getPrototypes()
    );
    SimulationStepper stepper = new SimulationStepper(bridge);
    while (!bridge.isComplete()) {
      long completedStep = stepper.perform();
      callback.onStep(completedStep);
      if (completedStep > 2) {
        bridge.getReplicate().deleteTimeStep(completedStep - 2);
      }
    }
  }

  /**
   * Callback interface for receiving simulation step completion notifications.
   */
  public interface SimulationStepCallback {
    /**
     * Called when a simulation step is completed.
     *
     * @param stepNumber The number of the step that was just completed
     */
    void onStep(long stepNumber);
  }

}
