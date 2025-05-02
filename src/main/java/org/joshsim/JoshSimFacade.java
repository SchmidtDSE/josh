/**
 * Facade for interacting with the Josh platform via code.
 *
 * @license BSD-3-Clause
 */

package org.joshsim;

import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.JvmCompatibilityLayer;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.JvmInputOutputLayer;
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
    setupForJvm();
    return JoshSimFacadeUtil.parse(code);
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
    setupForJvm();
    return JoshSimFacadeUtil.interpret(engineGeometryFactory, parsed);
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
        String simulationName, JoshSimFacadeUtil.SimulationStepCallback callback,
        boolean serialPatches) {
    setupForJvm();
    JoshSimFacadeUtil.runSimulation(
        engineGeometryFactory,
        new JvmInputOutputLayer(),
        program,
        simulationName,
        callback,
        serialPatches
    );
  }

  /**
   * Configures the system for execution within a Java Virtual Machine (JVM) environment.
   *
   * <p>This method sets the platform-specific compatibility layer to an instance of
   * JvmCompatibilityLayer, which provides the necessary abstractions to enable simulations to run
   * within the standard JVM environment.</p>
   */
  private static void setupForJvm() {
    CompatibilityLayerKeeper.set(new JvmCompatibilityLayer());
  }

}
