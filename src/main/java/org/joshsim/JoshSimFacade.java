/**
 * Facade for interacting with the Josh platform via code.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.bridge.QueryCacheEngineBridge;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.bridge.SimulationStepper;
import org.joshsim.lang.export.CombinedExportFacade;
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
   * @param engineGeometryFactory Factory though which to build simulation engine geometries.
   * @param parsed The result of parsing the Josh source successfully.
   * @return The parsed JoshProgram which can be used to run a specific simulation.
   */
  public static JoshProgram interpret(EngineGeometryFactory engineGeometryFactory,
        ParseResult parsed) {
    JoshInterpreter interpreter = new JoshInterpreter();
    return interpreter.interpret(parsed, engineGeometryFactory);
  }

  /**
   * Runs a simulation from the provided program.
   *
   * <p>Creates and executes a simulation using the provided program and simulation name.
   * The callback is invoked after each simulation step is completed.</p>
   *
   * @param engineGeometryFactory Factory with which to build engine geometries.
   * @param program The Josh program containing the simulation to run. This is the program in which
   *     the simulation will be initalized.
   * @param simulationName The name of the simulation to execute from the program. This will be
   *     initalized from the given program.
   * @param callback A callback that will be invoked after each simulation step. This is called
   *     as blocking.
   * @param serialPatches If true, patches will be processed serially. If false, they will be
   *     processed in parallel.
   */
  public static void runSimulation(EngineGeometryFactory engineGeometryFactory, JoshProgram program,
        String simulationName, SimulationStepCallback callback, boolean serialPatches) {
    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulationName).build();
    MutableEntity simEntity = new ShadowingEntity(simEntityRaw, simEntityRaw);
    EngineBridge bridge = new QueryCacheEngineBridge(
        engineGeometryFactory,
        simEntity,
        program.getConverter(),
        program.getPrototypes()
    );

    CombinedExportFacade exportFacade = new CombinedExportFacade(simEntity);
    SimulationStepper stepper = new SimulationStepper(bridge);

    exportFacade.start();

    while (!bridge.isComplete()) {
      long completedStep = stepper.perform(!serialPatches);
      exportFacade.write(bridge.getReplicate().getTimeStep(completedStep).orElseThrow());
      callback.onStep(completedStep);

      if (completedStep > 2) {
        bridge.getReplicate().deleteTimeStep(completedStep - 2);
      }
    }

    exportFacade.join();
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
