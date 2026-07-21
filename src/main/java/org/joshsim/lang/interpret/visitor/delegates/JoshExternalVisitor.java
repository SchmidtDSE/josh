
/**
 * Delegate handling external value parsing.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;


/**
 * Delegate which handles reading external values from source.
 */
public class JoshExternalVisitor implements JoshVisitorDelegate {

  private final JoshParserToMachineVisitor parent;
  private final ValueSupportFactory valueFactory;

  /**
   * Constructs a new instance of the JoshExternalVisitor class.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshExternalVisitor(DelegateToolbox toolbox) {
    parent = toolbox.getParent();
    valueFactory = toolbox.getValueFactory();
  }

  /**
   * Parse an external value reference.
   *
   * <p>Parse a reference to an external value at the current time step. A model wanting a
   * different lookup step (e.g. a resampled year during its own spin-up recipe) should use the
   * explicit {@code external X at <expr>} form instead -- this default never consults any
   * attribute implicitly, so an unrelated model attribute that happens to be named the same can
   * never silently redirect it.</p>
   *
   * @param ctx The context from which to parse the external value reference.
   * @return JoshFragment containing the external value reference parsed.
   */
  public JoshFragment visitExternalValue(JoshLangParser.ExternalValueContext ctx) {
    String name = ctx.name.getText();
    EventHandlerAction action = (machine) -> {
      machine.push(valueFactory.build(machine.getCurrentTimestep(), Units.of("count")));
      machine.pushExternalAtStep(name);
      return machine;
    };
    return new ActionFragment(action);
  }

  /**
   * Parse an external value reference at an explicitly computed time.
   *
   * <p>Parse a reference to an external value at a step given by an arbitrary expression (e.g. a
   * literal, {@code prior}, or a model-computed attribute like {@code current.year}), letting a
   * model opt in to a different lookup step per call site.</p>
   *
   * @param ctx The context from which to parse the external value at time reference.
   * @return JoshFragment containing the external value at time reference parsed.
   */
  public JoshFragment visitExternalValueAtTime(JoshLangParser.ExternalValueAtTimeContext ctx) {
    String name = ctx.name.getText();
    EventHandlerAction stepAction = ctx.step.accept(parent).getCurrentAction();
    EventHandlerAction action = (machine) -> {
      stepAction.apply(machine);
      machine.pushExternalAtStep(name);
      return machine;
    };
    return new ActionFragment(action);
  }

}
