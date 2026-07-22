/**
 * Pre-pass visitor collecting the event names a Josh program declares.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor;

import java.util.ArrayList;
import java.util.List;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.KnownEventSet;
import org.joshsim.lang.interpret.StringLiteralUtil;

/**
 * Visitor that discovers the set of valid event names in a Josh program before the main walk.
 *
 * <p>Runs ahead of {@link JoshParserToMachineVisitor} and produces a {@link KnownEventSet} which is
 * threaded into the {@code DelegateToolbox}. It collects the per-origin init variant events
 * declared by each entity's own {@code start init through "<origin>"} blocks, keyed to that entity
 * type so an origin declared on one entity (e.g. {@code Tree}) is never treated as valid for a
 * different entity (e.g. {@code Shrub}); the base init and default substep events are always
 * present via {@link KnownEventSet}'s defaults. It also collects a simulation's declared
 * {@code start phases ... end phases} block, if any.</p>
 */
public class JoshLangEventSetVisitor extends JoshLangBaseVisitor<KnownEventSet> {

  /**
   * Create a new event-set discovery visitor.
   */
  public JoshLangEventSetVisitor() {
    super();
  }

  /**
   * Collect the per-origin init variants and declared phases directly on this entity stanza.
   *
   * <p>{@code initStanza} and {@code phasesStanza} nodes only ever appear directly nested in an
   * {@code entityStanza} (see the grammar), so this is the one place the declaring entity's name
   * and stanza type are known; each init variant is registered against that name rather than a
   * program-wide bucket, while a declared phase order is program-wide.</p>
   *
   * @param ctx The entity stanza (organism, patch, simulation, etc.) to collect from.
   * @return The init variants and phase order declared by this entity, combined with anything found
   *     deeper in the tree (there is currently nothing else to find, since entities cannot nest).
   */
  @Override
  public KnownEventSet visitEntityStanza(JoshLangParser.EntityStanzaContext ctx) {
    String entityType = ctx.identifier().getText();
    KnownEventSet result = new KnownEventSet();
    for (JoshLangParser.InitStanzaContext initCtx : ctx.initStanza()) {
      String origin = StringLiteralUtil.stripQuotes(initCtx.STR_().getText());
      result.addInitOrigin(entityType, origin);
    }

    List<JoshLangParser.PhasesStanzaContext> phasesStanzas = ctx.phasesStanza();
    if (!phasesStanzas.isEmpty()) {
      String stanzaType = ctx.entityStanzaType(0).getText();
      if (!"simulation".equals(stanzaType)) {
        throw new IllegalArgumentException(String.format(
            "A `phases` block is only allowed inside a simulation stanza, not %s.", stanzaType));
      }
      if (phasesStanzas.size() > 1) {
        throw new IllegalArgumentException(
            "Only one `phases` block is allowed per simulation.");
      }
      result.declarePhases(getPhaseOrder(phasesStanzas.get(0)));
    }

    return result;
  }

  /**
   * Extract the declared phase names in order, enforcing the {@code with}/{@code then} shape.
   *
   * @param ctx The phases stanza to read.
   * @return The phase names in declaration order.
   */
  private List<String> getPhaseOrder(JoshLangParser.PhasesStanzaContext ctx) {
    List<JoshLangParser.PhaseDeclarationContext> declarations = ctx.phaseDeclaration();
    List<String> order = new ArrayList<>(declarations.size());
    for (int i = 0; i < declarations.size(); i++) {
      JoshLangParser.PhaseDeclarationContext declCtx = declarations.get(i);
      boolean isFirst = i == 0;
      boolean usesWith = declCtx.WITH_() != null;
      if (isFirst && !usesWith) {
        throw new IllegalArgumentException(
            "The first phase must be declared with `with phase <name>`.");
      }
      if (!isFirst && usesWith) {
        throw new IllegalArgumentException(
            "Only the first phase may use `with`; use `then phase <name>` after that.");
      }
      order.add(declCtx.name.getText());
    }
    return order;
  }

  @Override
  protected KnownEventSet defaultResult() {
    return new KnownEventSet();
  }

  @Override
  protected KnownEventSet aggregateResult(KnownEventSet aggregate, KnownEventSet nextResult) {
    if (aggregate == null) {
      return nextResult;
    }
    if (nextResult == null) {
      return aggregate;
    }
    return aggregate.combine(nextResult);
  }

}
