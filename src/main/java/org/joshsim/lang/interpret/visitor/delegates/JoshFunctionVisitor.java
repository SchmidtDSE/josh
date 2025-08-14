/**
 * Delegate for function definitions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import java.util.ArrayList;
import java.util.List;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.engine.func.CompiledSelectorFromCallable;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.ReservedWordChecker;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.josh.ActionFragment;
import org.joshsim.lang.interpret.fragment.josh.CompiledCallableFragment;
import org.joshsim.lang.interpret.fragment.josh.EventHandlerGroupFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.machine.PushDownMachineCallable;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;


/**
 * Visitor delegate for handling function operations.
 *
 * <p>Visitor delegate which handles function definitions for event handlers and conditional event
 * handlers including both lambdas (single line functions) and full body functions (logic between
 * curly braces).</p>
 */
public class JoshFunctionVisitor implements JoshVisitorDelegate {

  private final JoshParserToMachineVisitor parent;
  private final BridgeGetter bridgeGetter;

  /**
   * Create a new visitor for function operations.
   *
   * @param toolbox The toolbox through which visitors can access supporting objects.
   */
  public JoshFunctionVisitor(DelegateToolbox toolbox) {
    parent = toolbox.getParent();
    bridgeGetter = toolbox.getBridgeGetter();
  }

  /**
   * Parse a single line lambda function definition.
   *
   * <p>Parse a single line lambda function definition which may be used as an event handler or a
   * conditional event handler.</p>
   *
   * @param ctx The ANTLR context from which to parse the lambda function definition.
   * @return JoshFragment containing the lambda function definition parsed.
   */
  public JoshFragment visitLambda(JoshLangParser.LambdaContext ctx) {
    EventHandlerAction innerAction = ctx
        .getChild(0)
        .accept(parent)
        .getCurrentAction();

    EventHandlerAction action = (machine) -> {
      innerAction.apply(machine);
      machine.end();
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Parse a return statement in a function body.
   *
   * <p>Parse a return statement that ends the execution of a function and returns a value.</p>
   *
   * @param ctx The ANTLR context from which to parse the return statement.
   * @return JoshFragment containing the return statement parsed.
   */
  public JoshFragment visitReturn(JoshLangParser.ReturnContext ctx) {
    EventHandlerAction innerAction = ctx
        .getChild(1)
        .accept(parent)
        .getCurrentAction();

    EventHandlerAction action = (machine) -> {
      innerAction.apply(machine);
      machine.end();
      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Parse a full function body with multiple statements.
   *
   * <p>Parse a function body enclosed in curly braces that may contain multiple statements.
   * The function must end with a return statement.</p>
   *
   * @param ctx The ANTLR context from which to parse the function body.
   * @return JoshFragment containing the function body parsed.
   * @throws IllegalStateException if the function body does not end with a return statement.
   */
  public JoshFragment visitFullBody(JoshLangParser.FullBodyContext ctx) {
    List<EventHandlerAction> innerActions = new ArrayList<>();

    int numChildren = ctx.getChildCount();
    int numStatements = numChildren - 2;
    for (int statementIndex = 0; statementIndex < numStatements; statementIndex++) {
      int childIndex = statementIndex + 1;

      EventHandlerAction statementAction = ctx
          .getChild(childIndex)
          .accept(parent)
          .getCurrentAction();

      innerActions.add(statementAction);
    }

    EventHandlerAction action = (machine) -> {
      for (EventHandlerAction innerAction : innerActions) {
        innerAction.apply(machine);
        if (machine.isEnded()) {
          break;
        }
      }

      if (!machine.isEnded()) {
        throw new IllegalStateException("Event handler finished without returning a value.");
      }

      return machine;
    };

    return new ActionFragment(action);
  }

  /**
   * Parse an inner member of an event handler group.
   *
   * <p>Process the inner content of an event handler group member, extracting the handler
   * action.</p>
   *
   * @param ctx The ANTLR context from which to parse the event handler group member.
   * @return JoshFragment containing the event handler action.
   */
  public JoshFragment visitEventHandlerGroupMemberInner(
      JoshLangParser.EventHandlerGroupMemberInnerContext ctx) {
    EventHandlerAction handlerAction = ctx.target.accept(parent).getCurrentAction();
    return new ActionFragment(handlerAction);
  }

  /**
   * Parse a conditional if event handler group member.
   *
   * <p>Process an if condition branch in an event handler group, creating both the action to
   * execute and the condition that determines whether this branch should be selected.</p>
   *
   * @param ctx The ANTLR context from which to parse the conditional 'if' event handler.
   * @return JoshFragment containing the compiled callable and selector for the conditional handler.
   */
  public JoshFragment visitConditionalIfEventHandlerGroupMember(
      JoshLangParser.ConditionalIfEventHandlerGroupMemberContext ctx) {
    EventHandlerAction innerAction = ctx.inner.accept(parent).getCurrentAction();
    EventHandlerAction conditionAction = ctx.target.accept(parent).getCurrentAction();

    CompiledCallable decoratedInterpreterAction = makeCallableMachine(innerAction);
    CompiledCallable decoratedConditionAction = makeCallableMachine(conditionAction);
    CompiledSelector decoratedConditionSelector = new CompiledSelectorFromCallable(
        decoratedConditionAction
    );

    return new CompiledCallableFragment(decoratedInterpreterAction, decoratedConditionSelector);
  }

  /**
   * Parse a conditional elif event handler group member.
   *
   * <p>Process an 'elif' (else if) condition branch in an event handler group, creating both the
   * action to execute and the condition that determines whether this branch should be selected.</p>
   *
   * @param ctx The ANTLR context from which to parse the conditional 'elif' event handler.
   * @return JoshFragment containing the compiled callable and selector for the conditional handler.
   */
  public JoshFragment visitConditionalElifEventHandlerGroupMember(
      JoshLangParser.ConditionalElifEventHandlerGroupMemberContext ctx) {
    EventHandlerAction innerAction = ctx.inner.accept(parent).getCurrentAction();
    EventHandlerAction conditionAction = ctx.target.accept(parent).getCurrentAction();

    CompiledCallable decoratedInterpreterAction = makeCallableMachine(innerAction);
    CompiledCallable decoratedConditionAction = makeCallableMachine(conditionAction);
    CompiledSelector decoratedConditionSelector = new CompiledSelectorFromCallable(
        decoratedConditionAction
    );

    return new CompiledCallableFragment(decoratedInterpreterAction, decoratedConditionSelector);
  }

  /**
   * Parse a conditional else event handler group member.
   *
   * <p>Process an else branch in an event handler group, which executes when no other
   * conditional branches are selected.</p>
   *
   * @param ctx The ANTLR context from which to parse the 'else' event handler.
   * @return JoshFragment containing the compiled callable for the else handler.
   */
  public JoshFragment visitConditionalElseEventHandlerGroupMember(
      JoshLangParser.ConditionalElseEventHandlerGroupMemberContext ctx) {
    EventHandlerAction innerAction = ctx.inner.accept(parent).getCurrentAction();
    CompiledCallable decoratedInterpreterAction = makeCallableMachine(innerAction);
    return new CompiledCallableFragment(decoratedInterpreterAction);
  }

  /**
   * Parse a single event handler group.
   *
   * <p>Process an event handler with a single implementation in which there are no conditionals
   * on handler execution (all handlers executed).</p>
   *
   * @param ctx The ANTLR context from which to parse the single event handler group.
   * @return JoshFragment containing the event handler group.
   */
  public JoshFragment visitEventHandlerGroupSingle(JoshLangParser.EventHandlerGroupSingleContext ctx) {
    String fullName = ctx.name.getText();
    JoshFragment innerFragment = ctx.getChild(1).accept(parent);

    CompiledCallable innerCallable = makeCallableMachine(innerFragment.getCurrentAction());

    EventKey eventKey = buildEventKey(fullName);
    EventHandler eventHandler = new EventHandler(
        innerCallable,
        eventKey.getAttribute(),
        eventKey.getEvent()
    );

    EventHandlerGroupBuilder eventHandlerGroupBuilder = new EventHandlerGroupBuilder();
    eventHandlerGroupBuilder.addEventHandler(eventHandler);
    eventHandlerGroupBuilder.setEventKey(eventKey);

    return new EventHandlerGroupFragment(eventHandlerGroupBuilder);
  }

  /**
   * Parse a multiple event handler group with conditional branches.
   *
   * <p>Process an event handler with multiple implementations selected by conditional branches
   * (if/elif/else structure). In other words, which handler is used is subject to conditionals.</p>
   *
   * @param ctx The ANTLR context from which to parse the multiple event handler group.
   * @return JoshFragment containing the event handler group with all conditional branches.
   */
  public JoshFragment visitEventHandlerGroupMultiple(
      JoshLangParser.EventHandlerGroupMultipleContext ctx) {
    String fullName = ctx.name.getText();
    EventKey eventKey = buildEventKey(fullName);

    EventHandlerGroupBuilder groupBuilder = new EventHandlerGroupBuilder();
    groupBuilder.setEventKey(eventKey);

    int numBranches = ctx.getChildCount() - 1;
    for (int branchIndex = 0; branchIndex < numBranches; branchIndex++) {
      int childIndex = branchIndex + 1;
      JoshFragment childFragment = ctx.getChild(childIndex).accept(parent);

      if (childFragment.getCompiledSelector().isPresent()) {
        groupBuilder.addEventHandler(new EventHandler(
            childFragment.getCompiledCallable(),
            eventKey.getAttribute(),
            eventKey.getEvent(),
            childFragment.getCompiledSelector().get()
        ));
      } else {
        groupBuilder.addEventHandler(new EventHandler(
            childFragment.getCompiledCallable(),
            eventKey.getAttribute(),
            eventKey.getEvent()
        ));
      }
    }

    return new EventHandlerGroupFragment(groupBuilder);
  }

  /**
   * Parse a general event handler.
   *
   * <p>Process a general event handler and validate that the attribute name is not a reserved
   * word.</p>
   *
   * @param ctx The ANTLR context from which to parse the general event handler.
   * @return JoshFragment containing the event handler.
   * @throws IllegalArgumentException if the attribute name is a reserved word.
   */
  public JoshFragment visitEventHandlerGeneral(JoshLangParser.EventHandlerGeneralContext ctx) {
    JoshFragment fragment = ctx.getChild(0).accept(parent);
    String attributeName = fragment.getEventHandlerGroup().getAttribute();
    ReservedWordChecker.checkVariableDeclaration(attributeName);
    return fragment;
  }

  /**
   * Check if a string is a valid event name.
   *
   * <p>Determines if the provided string matches one of the predefined event names.</p>
   *
   * @param candidate The string to check.
   * @return true if the string is a valid event name, false otherwise.
   */
  private boolean isEventName(String candidate) {
    return switch (candidate) {
      case "init", "start", "step", "end", "remove", "constant" -> true;
      default -> false;
    };
  }

  /**
   * Build an EventKey from a full event name.
   *
   * <p>Parses a dot-separated event name into attribute and event components. If an event name
   * like step is not specified, the constant initialization event step is used.</p>
   *
   * @param fullName The full dot-separated event name to parse.
   * @return An EventKey containing the parsed attribute and event names.
   */
  private EventKey buildEventKey(String fullName) {
    String[] namePieces = fullName.split("\\.");
    String candidateEventName = namePieces[namePieces.length - 1];
    boolean endsWithEventName = isEventName(candidateEventName);

    CompatibleStringJoiner attributeNameJoiner = CompatibilityLayerKeeper
        .get()
        .createStringJoiner(".");

    for (int i = 0; i < namePieces.length - 1; i++) {
      attributeNameJoiner.add(namePieces[i]);
    }

    String attributeName;
    String eventName;
    if (endsWithEventName) {
      attributeName = attributeNameJoiner.toString();
      eventName = candidateEventName;
    } else {
      attributeNameJoiner.add(candidateEventName);
      attributeName = attributeNameJoiner.toString();
      eventName = "constant";
    }

    return new EventKey(attributeName, eventName);
  }

  /**
   * Create a PushDownMachineCallable from an EventHandlerAction.
   *
   * <p>Wraps an EventHandlerAction in a PushDownMachineCallable to make it usable
   * as a CompiledCallable in the event handler system.</p>
   *
   * @param action The EventHandlerAction to wrap.
   * @return A PushDownMachineCallable that wraps the provided action.
   */
  private PushDownMachineCallable makeCallableMachine(EventHandlerAction action) {
    return new PushDownMachineCallable(action, bridgeGetter);
  }

}
