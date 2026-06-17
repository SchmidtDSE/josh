/**
 * Delegate for stanzas and programs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import java.util.ArrayList;
import java.util.List;
import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.prototype.ParentlessEntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.action.EventHandlerAction;
import org.joshsim.lang.interpret.fragment.ProgramBuilder;
import org.joshsim.lang.interpret.fragment.josh.ConversionsFragment;
import org.joshsim.lang.interpret.fragment.josh.EntityFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.fragment.josh.ProgramFragment;
import org.joshsim.lang.interpret.fragment.josh.StateFragment;
import org.joshsim.lang.interpret.machine.PushDownMachineCallable;
import org.joshsim.lang.interpret.visitor.JoshParserToMachineVisitor;


/**
 * Visitor which handles language stanzas and program creation.
 *
 * <p>Visitor which handles parsing stanzas and forming programs from those stanzas, including those
 * for organisms, units, simulations, etc.</p>
 */
public class JoshStanzaVisitor implements JoshVisitorDelegate {

  private final JoshParserToMachineVisitor parent;
  private final ValueSupportFactory valueFactory;
  private final BridgeGetter bridgeGetter;

  /**
   * Create a new stanza visitor.
   *
   * @param toolbox Toolbox through which to access supporting objects.
   */
  public JoshStanzaVisitor(DelegateToolbox toolbox) {
    parent = toolbox.getParent();
    valueFactory = toolbox.getValueFactory();
    bridgeGetter = toolbox.getBridgeGetter();
  }

  /**
   * Visit a stanza which defines logic for a specific organism / agent state.
   *
   * @param ctx The stanza context to visit.
   * @return A fragment representing the stanza logic.
   */
  public JoshFragment visitStateStanza(JoshLangParser.StateStanzaContext ctx) {
    List<EventHandlerGroupBuilder> groups = new ArrayList<>();
    String stateName = ctx.getChild(2).getText();

    int numHandlerGroups = ctx.getChildCount() - 5;
    for (int handlerGroupIndex = 0; handlerGroupIndex < numHandlerGroups; handlerGroupIndex++) {
      int childIndex = handlerGroupIndex + 3;
      JoshFragment childFragment = ctx.getChild(childIndex).accept(parent);
      EventHandlerGroupBuilder groupBuilder = childFragment.getEventHandlerGroup();
      groupBuilder.setState(stateName);
      groups.add(groupBuilder);
    }

    return new StateFragment(groups);
  }

  /**
   * Visit a stanza which defines an entity such as an agent, organism, or simulation.
   *
   * <p>Process an entity stanza which contains event handlers and other definitions
   * for a specific entity type.</p>
   *
   * @param ctx The entity stanza context to visit.
   * @return A fragment representing the entity definition.
   * @throws IllegalArgumentException if the stanza start and end types don't match.
   */
  public JoshFragment visitEntityStanza(JoshLangParser.EntityStanzaContext ctx) {
    final int numChildren = ctx.getChildCount();
    final int numInner = numChildren - 5;

    String entityType = ctx.getChild(1).getText();
    String identifier = ctx.getChild(2).getText();
    String closeEntityType = ctx.getChild(numChildren - 1).getText();
    if (!entityType.equals(closeEntityType)) {
      String message = String.format(
          "Stanza start and end type different: %s, %s",
          entityType,
          closeEntityType
      );
      throw new IllegalArgumentException(message);
    }

    EntityBuilder entityBuilder = new EntityBuilder(valueFactory);
    entityBuilder.ensureStateDefaultHandler();
    entityBuilder.setName(identifier);

    for (int innerIndex = 0; innerIndex < numInner; innerIndex++) {
      int childIndex = innerIndex + 3;
      if (ctx.getChild(childIndex) instanceof JoshLangParser.PhaseStanzaContext phaseCtx) {
        capturePhase(phaseCtx, entityBuilder, entityType);
        continue;
      }

      JoshFragment childFragment = ctx.getChild(childIndex).accept(parent);

      for (EventHandlerGroupBuilder groupBuilder : childFragment.getEventHandlerGroups()) {
        entityBuilder.addEventHandlerGroup(groupBuilder.buildKey(), groupBuilder.build());
      }
    }

    EntityPrototype prototype = new ParentlessEntityPrototype(
        identifier,
        getEntityType(entityType),
        entityBuilder
    );

    return new EntityFragment(prototype);
  }

  /**
   * Capture a spin-up / spin-down phase onto the simulation being built.
   *
   * <p>A phase contributes two synthetic handlers: a constant-substep duration
   * ({@code __<phase>Steps}) the bridge reads at construction to anchor the clock, and a per-step
   * year expression ({@code __<phase>Year}) the bridge evaluates on demand while in that phase to
   * pick which data year's forcing is felt. The year handler is registered under a dedicated event
   * (the phase name) so the stepper never runs it automatically — it is only drawn while the phase
   * is active, leaving the random sequence of other phases untouched.</p>
   *
   * @param ctx The phase stanza to capture.
   * @param entityBuilder The simulation entity builder to attach the phase handlers to.
   * @param entityType The enclosing stanza type; phases are only valid inside a simulation.
   */
  private void capturePhase(JoshLangParser.PhaseStanzaContext ctx, EntityBuilder entityBuilder,
        String entityType) {
    if (!"simulation".equals(entityType)) {
      throw new IllegalArgumentException(
          "spinup / spindown blocks are only allowed inside a simulation stanza.");
    }

    String phase = ctx.phaseType(0).getText();
    String closePhase = ctx.phaseType(1).getText();
    if (!phase.equals(closePhase)) {
      throw new IllegalArgumentException(String.format(
          "Phase start and end type different: %s, %s", phase, closePhase));
    }

    EventHandlerAction yearAction = ctx.yearExpr.accept(parent).getCurrentAction();
    EventHandlerAction durationAction = ctx.duration.accept(parent).getCurrentAction();

    addPhaseHandler(entityBuilder, "__" + phase + "Steps", "constant", durationAction);
    addPhaseHandler(entityBuilder, "__" + phase + "Year", phase, yearAction);
  }

  /**
   * Register a single synthetic event handler on the entity being built.
   *
   * @param entityBuilder The entity builder to attach the handler to.
   * @param attribute The synthetic attribute name the handler resolves.
   * @param event The substep / event under which the handler resolves.
   * @param action The compiled action evaluated by the handler.
   */
  private void addPhaseHandler(EntityBuilder entityBuilder, String attribute, String event,
        EventHandlerAction action) {
    // Wrap so the machine is ended after the expression runs, matching how a handler body (lambda)
    // is compiled; getResult() requires the machine to have ended.
    EventHandlerAction endedAction = (machine) -> {
      action.apply(machine);
      machine.end();
      return machine;
    };
    CompiledCallable callable = new PushDownMachineCallable(endedAction, bridgeGetter);
    EventKey eventKey = EventKey.of(attribute, event);
    EventHandlerGroupBuilder groupBuilder = new EventHandlerGroupBuilder();
    groupBuilder.setEventKey(eventKey);
    groupBuilder.addEventHandler(new EventHandler(callable, attribute, event));
    entityBuilder.addEventHandlerGroup(groupBuilder.buildKey(), groupBuilder.build());
  }

  /**
   * Visit a stanza which defines unit conversions.
   *
   * <p>Process a unit stanza which contains conversion definitions from a source unit
   * to various destination units.</p>
   *
   * @param ctx The unit stanza context to visit.
   * @return A fragment representing the unit conversions.
   */
  public JoshFragment visitUnitStanza(JoshLangParser.UnitStanzaContext ctx) {
    String sourceUnitName = ctx.getChild(2).getText();
    Units sourceUnits = Units.of(sourceUnitName);

    List<Conversion> conversions = new ArrayList<>();
    int numChildren = ctx.getChildCount();
    int numConversions = numChildren - 5;
    for (int conversionIndex = 0; conversionIndex < numConversions; conversionIndex++) {
      int childIndex = conversionIndex + 3;
      JoshFragment childFragment = ctx.getChild(childIndex).accept(parent);
      Conversion incompleteConversion = childFragment.getConversion();
      Conversion completeConversion = new DirectConversion(
          sourceUnits,
          incompleteConversion.getDestinationUnits(),
          incompleteConversion.getConversionCallable(),
          incompleteConversion.isCommunicativeSafe()
      );
      conversions.add(completeConversion);
    }

    return new ConversionsFragment(conversions);
  }

  /**
   * Visit a configuration statement.
   *
   * <p>Process a configuration statement, which is currently reserved for future use.
   * Specifically, this may be used for interacting with the UI for user-controled
   * configuration.</p>
   *
   * @param ctx The configuration statement context to visit.
   * @return A fragment representing the configuration statement.
   * @throws RuntimeException as this feature is not yet implemented.
   */
  public JoshFragment visitConfigStatement(JoshLangParser.ConfigStatementContext ctx) {
    throw new RuntimeException("Configuration statements reserved for future use.");
  }

  /**
   * Visit an import statement.
   *
   * <p>Process an import statement, which is currently reserved for future use. Specifically, this
   * may be used for multi-file scripts.</p>
   *
   * @param ctx The import statement context to visit.
   * @return A fragment representing the import statement.
   * @throws RuntimeException as this feature is not yet implemented.
   */
  public JoshFragment visitImportStatement(JoshLangParser.ImportStatementContext ctx) {
    throw new RuntimeException("Import statements reserved for future use.");
  }

  /**
   * Visit a complete program.
   *
   * <p>Process a complete program consisting of multiple stanzas and statements.</p>
   *
   * @param ctx The program context to visit.
   * @return A fragment representing the complete program.
   */
  public JoshFragment visitProgram(JoshLangParser.ProgramContext ctx) {
    ProgramBuilder builder = new ProgramBuilder();
    int numChildren = ctx.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      JoshFragment childFragment = ctx.getChild(i).accept(parent);
      builder.add(childFragment);
    }
    return new ProgramFragment(builder);
  }

  /**
   * Convert a string entity type name to an EntityType enum value.
   *
   * <p>Maps string representations of entity types used in the language to
   * the corresponding EntityType enum values used in the engine.</p>
   *
   * @param entityType The string representation of the entity type.
   * @return The corresponding EntityType enum value.
   * @throws IllegalArgumentException if the entity type is unknown.
   */
  private EntityType getEntityType(String entityType) {
    return switch (entityType) {
      case "agent", "organism" -> EntityType.AGENT;
      case "management" -> EntityType.AGENT;
      case "disturbance" -> EntityType.DISTURBANCE;
      case "external" -> EntityType.EXTERNAL_RESOURCE;
      case "patch" -> EntityType.PATCH;
      case "simulation" -> EntityType.SIMULATION;
      default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
    };
  }

}
