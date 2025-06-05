/**
 * Structure to assist visitor delegates.
 * 
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;


/**
 * Set of supporting objects to help a delegate.
 */
public class DelegateToolbox {
  
  private final JoshParserToMachineVisitor parent;
  private final EngineValueFactory valueFactory;

  /**
   * Constructs a new DelegateToolbox instance.
   *
   * @param parent The parent visitor that this toolbox supports
   * @param valueFactory The factory used for creating engine values
   */
  public DelegateToolbox(JoshParserToMachineVisitor parent, EngineValueFactory valueFactory) {
    this.parent = parent;
    this.valueFactory = valueFactory;
  }


  /**
   * Gets the parent visitor that this delegate toolbox supports.
   *
   * @return The parent JoshParserToMachineVisitor instance
   */
  public JoshParserToMachineVisitor getParent() {
    return parent;
  }

  /**
   * Gets the value factory used for creating engine values.
   *
   * @return The EngineValueFactory instance
   */
  public EngineValueFactory getValueFactory() {
    return valueFactory;
  }

}
