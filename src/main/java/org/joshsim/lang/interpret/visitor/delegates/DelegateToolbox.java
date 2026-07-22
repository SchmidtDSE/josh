/**
 * Structure to assist visitor delegates.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.KnownEventSet;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;


/**
 * Set of supporting objects to help a delegate.
 */
public class DelegateToolbox {

  private final JoshParserToMachineVisitor parent;
  private final ValueSupportFactory valueFactory;
  private final BridgeGetter bridgeGetter;
  private final KnownEventSet knownEventSet;

  /**
   * Construct a new DelegateToolbox instance.
   *
   * @param parent The parent visitor that this toolbox supports
   * @param valueFactory The factory used for creating engine values
   * @param bridgeGetter The bridge getter used for getting the current replicate executing.
   * @param knownEventSet The set of event names declared by the program being interpreted.
   */
  public DelegateToolbox(JoshParserToMachineVisitor parent, ValueSupportFactory valueFactory,
        BridgeGetter bridgeGetter, KnownEventSet knownEventSet) {
    this.parent = parent;
    this.valueFactory = valueFactory;
    this.bridgeGetter = bridgeGetter;
    this.knownEventSet = knownEventSet;
  }


  /**
   * Get the parent visitor that this delegate toolbox supports.
   *
   * @return The parent JoshParserToMachineVisitor instance
   */
  public JoshParserToMachineVisitor getParent() {
    return parent;
  }

  /**
   * Get the value factory used for creating engine values.
   *
   * @return The ValueSupportFactory instance
   */
  public ValueSupportFactory getValueFactory() {
    return valueFactory;
  }

  /**
   * Get the bridge getter used for getting the current replicate executing.
   *
   * @return The BridgeGetter instance to use in the delegate
   */
  public BridgeGetter getBridgeGetter() {
    return bridgeGetter;
  }

  /**
   * Get the set of event names declared by the program being interpreted.
   *
   * @return The KnownEventSet instance
   */
  public KnownEventSet getKnownEventSet() {
    return knownEventSet;
  }

}
