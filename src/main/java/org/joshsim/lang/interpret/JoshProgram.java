/**
 * Structures for representing a parsed Josh program.
 *
 * <p>Contains the core components needed to execute a Josh program, including the converter
 * for unit conversions and the simulation store for managing simulations.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.List;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.lang.bridge.EngineBridgeSimulationStore;


/**
 * Represents a parsed and executable Josh program.
 *
 * <p>This class holds the essential components required for executing a Josh program,
 * including facilities for value conversion and simulation management.</p>
 */
public class JoshProgram {

  /** The default per-timestep substep order, used unless a simulation declares its own phases. */
  private static final List<String> DEFAULT_SUBSTEP_ORDER = List.of("start", "step", "end");

  private final Converter converter;
  private final EngineBridgeSimulationStore simulations;
  private final EntityPrototypeStore prototypes;
  private BridgeGetter bridgeGetter;
  private List<String> substepOrder;

  /**
   * Creates a new Josh program instance.
   *
   * @param converter The converter to use for unit conversions within the program.
   * @param simulations The store which contains all simulations required for running for this
   *     program.
   */
  public JoshProgram(Converter converter, EngineBridgeSimulationStore simulations,
      EntityPrototypeStore prototypes) {
    this.converter = converter;
    this.simulations = simulations;
    this.prototypes = prototypes;
    this.substepOrder = DEFAULT_SUBSTEP_ORDER;
  }

  /**
   * Gets the converter used for unit conversions.
   *
   * @return The converter instance to use when executing this program.
   */
  public Converter getConverter() {
    return converter;
  }

  /**
   * Gets the simulation store containing all program simulations.
   *
   * @return The simulation store to use when executing this program.
   */
  public EngineBridgeSimulationStore getSimulations() {
    return simulations;
  }

  /**
   * Gets the store containing all entity prototypes required by the program.
   *
   * @return The entity prototype store to use when executing this program.
   */
  public EntityPrototypeStore getPrototypes() {
    return prototypes;
  }

  /**
   * Gets the bridge getter used during program interpretation.
   *
   * @return The bridge getter instance, or null if not set.
   */
  public BridgeGetter getBridgeGetter() {
    return bridgeGetter;
  }

  /**
   * Sets the bridge getter used during program interpretation.
   *
   * <p>This is used internally to allow access to the bridge getter after
   * program interpretation is complete.</p>
   *
   * @param bridgeGetter The bridge getter instance to store.
   */
  public void setBridgeGetter(BridgeGetter bridgeGetter) {
    this.bridgeGetter = bridgeGetter;
  }

  /**
   * Gets the ordered per-timestep substep sequence this program executes.
   *
   * <p>This is the program's declared {@code start phases ... end phases} order if it declared
   * one, or the default {@code start}/{@code step}/{@code end} otherwise. A caller building the
   * {@code EngineBridge} that drives the actual simulation step loop must pass this through;
   * otherwise every declared phase silently never runs, since the bridge falls back to its own
   * hardcoded default substep order.</p>
   *
   * @return The ordered substep names to execute each timestep.
   */
  public List<String> getSubstepOrder() {
    return substepOrder;
  }

  /**
   * Sets the ordered per-timestep substep sequence this program executes.
   *
   * @param substepOrder The ordered substep names, from the program's parsed
   *     {@link KnownEventSet}.
   */
  public void setSubstepOrder(List<String> substepOrder) {
    this.substepOrder = substepOrder;
  }

}
