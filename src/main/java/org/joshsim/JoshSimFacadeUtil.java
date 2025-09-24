/**
 * Utilities to help construct Josh facades.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.config.JshcConfigGetter;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.bridge.QueryCacheEngineBridge;
import org.joshsim.lang.bridge.ShadowingEntity;
import org.joshsim.lang.bridge.SimulationStepper;
import org.joshsim.lang.interpret.JoshInterpreter;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.CombinedExportFacade;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.parse.JoshParser;
import org.joshsim.lang.parse.ParseResult;
import org.joshsim.precompute.JshdExternalGetter;


/**
 * Utility functions common to all Josh facades.
 */
public class JoshSimFacadeUtil {

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
   * @param valueFactory Factory with which to build simulation engine values.
   * @param geometryFactory Factory though which to build simulation engine geometries.
   * @param parsed The result of parsing the Josh source successfully.
   * @param inputOutputLayer Layer to use to interact with external files and resources.
   * @return The parsed JoshProgram which can be used to run a specific simulation.
   */
  public static JoshProgram interpret(EngineValueFactory valueFactory,
        EngineGeometryFactory geometryFactory, ParseResult parsed,
        InputOutputLayer inputOutputLayer) {
    JoshInterpreter interpreter = new JoshInterpreter();
    return interpreter.interpret(parsed, valueFactory, geometryFactory, inputOutputLayer);
  }

  /**
   * Runs a simulation from the provided program.
   *
   * <p>Creates and executes a simulation using the provided program and simulation name.
   * The callback is invoked after each simulation step is completed.</p>
   *
   * @param valueFactory Factory with which to build simulation engine values.
   * @param geometryFactory Factory with which to build engine geometries.
   * @param program The Josh program containing the simulation to run. This is the program in which
   *     the simulation will be initalized.
   * @param simulationName The name of the simulation to execute from the program. This will be
   *     initalized from the given program.
   * @param callback A callback that will be invoked after each simulation step. This is called
   *     as blocking.
   * @param serialPatches If true, patches will be processed serially. If false, they will be
   *     processed in parallel.
   * @param outputSteps Optional set of step numbers to export. If empty, all steps are exported.
   *     If present, only steps contained in the set will have their output written to export files.
   *     All steps continue to execute for simulation state continuity regardless of this filter.
   */
  public static void runSimulation(EngineValueFactory valueFactory,
        EngineGeometryFactory geometryFactory, InputOutputLayer inputOutputLayer,
        JoshProgram program, String simulationName, SimulationStepCallback callback,
        boolean serialPatches, Optional<Set<Integer>> outputSteps) {

    MutableEntity simEntityRaw = program.getSimulations().getProtoype(simulationName).build();
    MutableEntity simEntity = new ShadowingEntity(valueFactory, simEntityRaw, simEntityRaw);
    EngineBridge bridge = new QueryCacheEngineBridge(
        valueFactory,
        geometryFactory,
        simEntity,
        program.getConverter(),
        program.getPrototypes(),
        new JshdExternalGetter(inputOutputLayer.getInputStrategy(), valueFactory),
        new JshcConfigGetter(inputOutputLayer.getInputStrategy(), valueFactory)
    );

    CombinedExportFacade exportFacade = new CombinedExportFacade(
        simEntity,
        inputOutputLayer.getExportFacadeFactory()
    );
    SimulationStepper stepper = new SimulationStepper(bridge);

    exportFacade.start();

    while (!bridge.isComplete()) {
      long completedStep = stepper.perform(serialPatches);
      if (outputSteps.isEmpty() || outputSteps.get().contains((int) completedStep)) {
        exportFacade.write(bridge.getReplicate().getTimeStep(completedStep).orElseThrow());
      }
      callback.onStep(completedStep);

      if (completedStep > 2) {
        bridge.getReplicate().deleteTimeStep(completedStep - 2);
      }
    }

    exportFacade.join();
  }

  /**
   * Runs a simulation from the provided program with backward compatibility.
   *
   * <p>Creates and executes a simulation using the provided program and simulation name.
   * The callback is invoked after each simulation step is completed. This method provides
   * backward compatibility by exporting all steps.</p>
   *
   * @param valueFactory Factory with which to build simulation engine values.
   * @param geometryFactory Factory with which to build engine geometries.
   * @param program The Josh program containing the simulation to run. This is the program in which
   *     the simulation will be initalized.
   * @param simulationName The name of the simulation to execute from the program. This will be
   *     initalized from the given program.
   * @param callback A callback that will be invoked after each simulation step. This is called
   *     as blocking.
   * @param serialPatches If true, patches will be processed serially. If false, they will be
   *     processed in parallel.
   */
  public static void runSimulation(EngineValueFactory valueFactory,
        EngineGeometryFactory geometryFactory, InputOutputLayer inputOutputLayer,
        JoshProgram program, String simulationName, SimulationStepCallback callback,
        boolean serialPatches) {
    runSimulation(valueFactory, geometryFactory, inputOutputLayer, program,
        simulationName, callback, serialPatches, Optional.empty());
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
