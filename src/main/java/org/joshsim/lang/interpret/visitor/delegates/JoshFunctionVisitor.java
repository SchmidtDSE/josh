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
import org.joshsim.lang.interpret.fragment.ActionFragment;
import org.joshsim.lang.interpret.fragment.CompiledCallableFragment;
import org.joshsim.lang.interpret.fragment.EventHandlerGroupFragment;
import org.joshsim.lang.interpret.fragment.Fragment;
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
   * @return Fragment containing the lambda function definition parsed.
   */
  public Fragment visitLambda(JoshLangParser.LambdaContext ctx) {
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

  public Fragment visitReturn(JoshLangParser.ReturnContext ctx) {
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

  public Fragment visitFullBody(JoshLangParser.FullBodyContext ctx) {
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

  public Fragment visitEventHandlerGroupMemberInner(
      JoshLangParser.EventHandlerGroupMemberInnerContext ctx) {
    EventHandlerAction handlerAction = ctx.target.accept(parent).getCurrentAction();
    return new ActionFragment(handlerAction);
  }

  public Fragment visitConditionalIfEventHandlerGroupMember(
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

  public Fragment visitConditionalElifEventHandlerGroupMember(
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

  public Fragment visitConditionalElseEventHandlerGroupMember(
      JoshLangParser.ConditionalElseEventHandlerGroupMemberContext ctx) {
    EventHandlerAction innerAction = ctx.inner.accept(parent).getCurrentAction();
    CompiledCallable decoratedInterpreterAction = makeCallableMachine(innerAction);
    return new CompiledCallableFragment(decoratedInterpreterAction);
  }

  public Fragment visitEventHandlerGroupSingle(JoshLangParser.EventHandlerGroupSingleContext ctx) {
    String fullName = ctx.name.getText();
    Fragment innerFragment = ctx.getChild(1).accept(parent);

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

  public Fragment visitEventHandlerGroupMultiple(
      JoshLangParser.EventHandlerGroupMultipleContext ctx) {
    String fullName = ctx.name.getText();
    EventKey eventKey = buildEventKey(fullName);

    EventHandlerGroupBuilder groupBuilder = new EventHandlerGroupBuilder();
    groupBuilder.setEventKey(eventKey);

    int numBranches = ctx.getChildCount() - 1;
    for (int branchIndex = 0; branchIndex < numBranches; branchIndex++) {
      int childIndex = branchIndex + 1;
      Fragment childFragment = ctx.getChild(childIndex).accept(parent);

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

  public Fragment visitEventHandlerGeneral(JoshLangParser.EventHandlerGeneralContext ctx) {
    Fragment fragment = ctx.getChild(0).accept(parent);
    String attributeName = fragment.getEventHandlerGroup().getAttribute();
    ReservedWordChecker.checkVariableDeclaration(attributeName);
    return fragment;
  }

  private boolean isEventName(String candidate) {
    return switch (candidate) {
      case "init", "start", "step", "end", "remove", "constant" -> true;
      default -> false;
    };
  }

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

  private PushDownMachineCallable makeCallableMachine(EventHandlerAction action) {
    return new PushDownMachineCallable(action, bridgeGetter);
  }

}
