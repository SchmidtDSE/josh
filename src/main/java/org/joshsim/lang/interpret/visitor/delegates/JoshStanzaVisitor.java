/**
 * Delegate for stanzas and programs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor.delegates;

import java.util.ArrayList;
import java.util.List;
import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.handler.EventHandlerGroupBuilder;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.prototype.EntityOverwriteBehavior;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.prototype.ParentlessEntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.KnownEventSet;
import org.joshsim.lang.interpret.StringLiteralUtil;
import org.joshsim.lang.interpret.fragment.ProgramBuilder;
import org.joshsim.lang.interpret.fragment.josh.ConversionsFragment;
import org.joshsim.lang.interpret.fragment.josh.EntityFragment;
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
import org.joshsim.lang.interpret.fragment.josh.ProgramFragment;
import org.joshsim.lang.interpret.fragment.josh.StateFragment;
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
  private final KnownEventSet knownEventSet;

  /**
   * Create a new stanza visitor.
   *
   * @param toolbox Toolbox through which to access supporting objects.
   */
  public JoshStanzaVisitor(DelegateToolbox toolbox) {
    parent = toolbox.getParent();
    valueFactory = toolbox.getValueFactory();
    knownEventSet = toolbox.getKnownEventSet();
  }

  /**
   * Visit a stanza which defines logic for a specific organism / agent state.
   *
   * @param ctx The stanza context to visit.
   * @return A fragment representing the stanza logic.
   */
  public JoshFragment visitStateStanza(JoshLangParser.StateStanzaContext ctx) {
    List<EventHandlerGroupBuilder> groups = new ArrayList<>();
    // The state name is a STR_ token whose text includes the surrounding quotes. Strip them so the
    // stored state name matches the (now unquoted) string value an entity's state attribute holds
    // (see JoshValueVisitor.visitString); otherwise state-machine dispatch never matches.
    String stateName = StringLiteralUtil.stripQuotes(ctx.getChild(2).getText());

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
   * Capture an origin-dispatched init stanza onto the entity being built.
   *
   * <p>A {@code start init through "<origin>" ... end init} block supplies the init handlers that
   * run for a cohort created via {@code create ... through "<origin>"}. Its body handlers are
   * written without an event suffix (e.g. {@code age = ...}), so they parse as {@code constant}
   * groups; this re-keys each group to the per-origin init variant event
   * ({@link KnownEventSet#initEventFor}) and registers it as an ordinary handler group. Dispatch is
   * a compile-time desugar: a {@code create ... through "<origin>"} fast-forwards this variant
   * event <em>instead of</em> the base {@code init} (pure replace), so the variant must supply
   * every attribute the cohort needs at birth.</p>
   *
   * @param ctx The init stanza to capture.
   * @param entityBuilder The entity builder to attach the variant init handlers to.
   * @param entityType The enclosing stanza type; origin init is meaningless on patch / simulation,
   *     which are never created via {@code create ... through}.
   */
  private void captureInitThrough(JoshLangParser.InitStanzaContext ctx,
        EntityBuilder entityBuilder, String entityType) {
    if ("patch".equals(entityType) || "simulation".equals(entityType)) {
      throw new IllegalArgumentException(String.format(
          "start init through blocks are not allowed inside a %s stanza; %s entities are not "
          + "created via `create ... through` so their init cannot be origin-dispatched.",
          entityType, entityType));
    }

    // The origin is a STR_ token whose text includes the surrounding quotes; strip them so it
    // matches the (unquoted) origin threaded through `create ... through "<origin>"`.
    String origin = StringLiteralUtil.stripQuotes(ctx.STR_().getText());
    String initEvent = KnownEventSet.initEventFor(origin);

    for (JoshLangParser.EventHandlerGeneralContext handlerCtx : ctx.eventHandlerGeneral()) {
      for (EventHandlerGroupBuilder groupBuilder
          : handlerCtx.accept(parent).getEventHandlerGroups()) {
        String attribute = groupBuilder.buildKey().getAttribute();
        // Force the event to the variant init regardless of how the handler parsed ("constant").
        groupBuilder.setEventKey(EventKey.of(attribute, initEvent));
        entityBuilder.addEventHandlerGroup(groupBuilder.buildKey(), groupBuilder.build());
      }
    }
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

    EntityBuilder entityBuilder = new EntityBuilder(valueFactory, knownEventSet);
    // Name must be set before ensureStateDefaultHandler(), which scopes its per-init-event
    // defaults to this entity's own declared origins via knownEventSet.getInitEvents(name).
    entityBuilder.setName(identifier);
    // An `update` stanza's defaults would otherwise clobber the base's real state handlers once
    // EntityBuilder.combineWith layers this builder's entries on top: ensureStateDefaultHandler
    // seeds a default for every origin the entity type has ANYWHERE in the program (per
    // knownEventSet), not just the ones this stanza redeclares, so an `update` block that only
    // adds one origin would otherwise overwrite the base's real handlers for every other origin
    // with empty-string defaults. `update` only ever needs to contribute what it explicitly
    // declares; the base it merges onto already carries its own complete defaults.
    boolean isUpdate = "update".equals(ctx.getChild(0).getText());
    if (!isUpdate) {
      entityBuilder.ensureStateDefaultHandler();
    }

    for (int innerIndex = 0; innerIndex < numInner; innerIndex++) {
      int childIndex = innerIndex + 3;
      if (ctx.getChild(childIndex) instanceof JoshLangParser.InitStanzaContext initCtx) {
        captureInitThrough(initCtx, entityBuilder, entityType);
        continue;
      }

      if (ctx.getChild(childIndex) instanceof JoshLangParser.PhasesStanzaContext) {
        // Already captured by JoshLangEventSetVisitor's pre-pass; declares no handlers of its own.
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

    return new EntityFragment(prototype, getOverwriteBehavior(ctx));
  }

  /**
   * Determine how an entity stanza's opening keyword relates to a prior same-named entity.
   *
   * @param ctx The entity stanza context whose opening keyword to inspect.
   * @return The overwrite behavior for {@code start} / {@code replace} / {@code update}.
   * @throws IllegalArgumentException if the opening keyword is not one of the three above.
   */
  private EntityOverwriteBehavior getOverwriteBehavior(JoshLangParser.EntityStanzaContext ctx) {
    String opener = ctx.getChild(0).getText();
    return switch (opener) {
      case "start" -> EntityOverwriteBehavior.NOT_SPECIFIED;
      case "replace" -> EntityOverwriteBehavior.OVERWRITE;
      case "update" -> EntityOverwriteBehavior.UPDATE;
      default -> throw new IllegalArgumentException("Unknown entity stanza opener: " + opener);
    };
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
   * <p>An {@code import} is resolved by {@code JoshImportPreprocessor} before this visitor ever
   * runs: it is spliced out and replaced with the imported file's own text. Reaching this method
   * means either the current execution environment does not preprocess imports (e.g. WASM or a
   * cloud worker, which receive a single source string with no file system to resolve against) or
   * preprocessing was skipped.</p>
   *
   * @param ctx The import statement context to visit.
   * @return A fragment representing the import statement.
   * @throws RuntimeException as imports are not supported outside of file-based execution.
   */
  public JoshFragment visitImportStatement(JoshLangParser.ImportStatementContext ctx) {
    throw new RuntimeException("Imports are not supported in this execution environment.");
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
