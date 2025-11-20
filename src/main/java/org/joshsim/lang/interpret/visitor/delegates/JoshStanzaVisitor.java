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
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.prototype.ParentlessEntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.lang.antlr.JoshLangParser;
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

  /**
   * Create a new stanza visitor.
   *
   * @param toolbox Toolbox through which to access supporting objects.
   */
  public JoshStanzaVisitor(DelegateToolbox toolbox) {
    parent = toolbox.getParent();
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
    int numChildren = ctx.getChildCount();
    int numInner = numChildren - 5;

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

    EntityBuilder entityBuilder = new EntityBuilder();
    entityBuilder.setName(identifier);

    for (int innerIndex = 0; innerIndex < numInner; innerIndex++) {
      int childIndex = innerIndex + 3;
      JoshFragment childFragment = ctx.getChild(childIndex).accept(parent);

      for (EventHandlerGroupBuilder groupBuilder : childFragment.getEventHandlerGroups()) {
        entityBuilder.addEventHandlerGroup(groupBuilder.buildKey(), groupBuilder.build());
      }
    }

    // Add geoKey as a built-in attribute for organisms/agents and patches
    // This allows parent.geoKey to work by making geoKey a real attribute
    EntityType type = getEntityType(entityType);
    if (type == EntityType.AGENT || type == EntityType.PATCH) {
      entityBuilder.addAttribute("geoKey", null);
    }

    // Add debugFiles as built-in attributes for simulation entities
    // This allows debug output to be configured via debugFiles.patch, etc.
    if (type == EntityType.SIMULATION) {
      entityBuilder.addAttribute("debugFiles.patch", null);
      entityBuilder.addAttribute("debugFiles.organism", null);
      entityBuilder.addAttribute("debugFiles.agent", null);
    }

    EntityPrototype prototype = new ParentlessEntityPrototype(
        identifier,
        type,
        entityBuilder
    );

    return new EntityFragment(prototype);
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
   * <p>Uses a two-pass parsing approach to ensure unit aliases are registered before
   * they are used in constants or other expressions:</p>
   * <ul>
   *   <li>Pass 1: Parse all unit stanzas to register units and their aliases</li>
   *   <li>Pass 2: Parse all other elements (entities, constants, etc.) which may reference
   *       those aliases</li>
   * </ul>
   *
   * @param ctx The program context to visit.
   * @return A fragment representing the complete program.
   */
  public JoshFragment visitProgram(JoshLangParser.ProgramContext ctx) {
    ProgramBuilder builder = new ProgramBuilder();
    int numChildren = ctx.getChildCount();

    // PASS 1: Register all units and aliases first
    // This ensures that unit aliases like 'yr' are available before constants that use them
    for (int i = 0; i < numChildren; i++) {
      if (ctx.getChild(i) instanceof JoshLangParser.UnitStanzaContext) {
        JoshFragment childFragment = ctx.getChild(i).accept(parent);
        builder.add(childFragment);
      }
    }

    // PASS 2: Parse all other program elements (entities, constants, etc.)
    // These can now safely reference unit aliases that were registered in pass 1
    for (int i = 0; i < numChildren; i++) {
      if (!(ctx.getChild(i) instanceof JoshLangParser.UnitStanzaContext)) {
        JoshFragment childFragment = ctx.getChild(i).accept(parent);
        builder.add(childFragment);
      }
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
