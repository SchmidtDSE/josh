/**
 * Delegate which handles distribution operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;


/**
 * Visitor delegate which handles distribution operations.
 *
 * <p>Visitor delegate which handles distribution operations like different sampling operations or
 * slicing.</p>
 */
public class JoshDistributionVisitor implements JoshVisitorDelegate {

  private final JoshParserToMachineVisitor parent;
  private final EngineValueFactory valueFactory;
  private final EngineValue singleCount;

  /**
   * Create a new visitor for distribution operations.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshDistributionVisitor(DelegateToolbox toolbox) {
    parent = toolbox.getParent();
    valueFactory = toolbox.getValueFactory();
    singleCount = valueFactory.build(1, Units.of("count"));
  }

  /**
   * Parse a slice expression.
   *
   * @param ctx The ANTLR context from which to parse the slice expression.
   * @return Fragment containing the slice expression parsed.
   */
  public Fragment visitSlice(JoshLangParser.SliceContext ctx) {
    EventHandlerAction subjectAction = ctx.subject.accept(parent).getCurrentAction();
    EventHandlerAction selectionAction = ctx.selection.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      subjectAction.apply(machine);
      selectionAction.apply(machine);
      return machine.slice();
    };

    return new ActionFragment(action);
  }

  /**
   * Parse a simple sample expression.
   *
   * <p>Parse a sample expression without a count parameter, defaulting to a single sample.</p>
   *
   * @param ctx The ANTLR context from which to parse the sample expression.
   * @return Fragment containing the sample expression parsed.
   */
  public Fragment visitSampleSimple(JoshLangParser.SampleSimpleContext ctx) {
    EventHandlerAction targetAction = ctx.target.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      targetAction.apply(machine);
      machine.push(singleCount);
      machine.sample(true);
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Parse a parameterized sample expression.
   *
   * <p>Parse a sample expression with a count parameter specifying how many samples to take.</p>
   *
   * @param ctx The ANTLR context from which to parse the sample expression.
   * @return Fragment containing the sample expression parsed.
   */
  public Fragment visitSampleParam(JoshLangParser.SampleParamContext ctx) {
    EventHandlerAction countAction = ctx.count.accept(parent).getCurrentAction();
    EventHandlerAction targetAction = ctx.target.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      countAction.apply(machine);
      targetAction.apply(machine);
      machine.sample(true);
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Parse a parameterized sample expression with an argument for replacement behaviors.
   *
   * <p>Parse a sample expression with a named sampling strategy with a parameter for replacement
   * behaviors.</p>
   *
   * @param ctx The ANTLR context from which to parse the sample expression.
   * @return Fragment containing the sample expression parsed.
   */
  public Fragment visitSampleParamReplacement(JoshLangParser.SampleParamReplacementContext ctx) {
    EventHandlerAction countAction = ctx.count.accept(parent).getCurrentAction();
    EventHandlerAction targetAction = ctx.target.accept(parent).getCurrentAction();
    String replacementStr = ctx.replace.getText();
    boolean withReplacement = replacementStr.equals("with");

    EventHandlerAction action = (machine) -> {
      countAction.apply(machine);
      targetAction.apply(machine);
      machine.sample(withReplacement);
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Parse a uniform distribution sampling expression.
   *
   * <p>Parse an expression that samples from a uniform distribution between the low and high
   * bounds.</p>
   *
   * @param ctx The ANTLR context from which to parse the uniform sampling expression.
   * @return Fragment containing the uniform sampling expression parsed.
   */
  public Fragment visitUniformSample(JoshLangParser.UniformSampleContext ctx) {
    EventHandlerAction lowAction = ctx.low.accept(parent).getCurrentAction();
    EventHandlerAction highAction = ctx.high.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      lowAction.apply(machine);
      highAction.apply(machine);
      machine.randUniform();
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Parse a normal distribution sampling expression.
   *
   * <p>Parse an expression that samples from a normal distribution with the specified mean and
   * standard deviation.</p>
   *
   * @param ctx The ANTLR context from which to parse the normal sampling expression.
   * @return Fragment containing the normal sampling expression parsed.
   */
  public Fragment visitNormalSample(JoshLangParser.NormalSampleContext ctx) {
    EventHandlerAction meanAction = ctx.mean.accept(parent).getCurrentAction();
    EventHandlerAction stdAction = ctx.stdev.accept(parent).getCurrentAction();

    EventHandlerAction action = (machine) -> {
      meanAction.apply(machine);
      stdAction.apply(machine);
      machine.randNorm();
      return machine;
    };

    return new ActionFragment(action);
  }

}
