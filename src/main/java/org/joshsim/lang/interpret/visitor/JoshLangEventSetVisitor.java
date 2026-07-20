/**
 * Pre-pass visitor collecting the event names a Josh program declares.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor;

import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.KnownEventSet;
import org.joshsim.lang.interpret.StringLiteralUtil;

/**
 * Visitor that discovers the set of valid event names in a Josh program before the main walk.
 *
 * <p>Runs ahead of {@link JoshParserToMachineVisitor} and produces a {@link KnownEventSet} which is
 * threaded into the {@code DelegateToolbox}. Today it collects the per-origin init variant events
 * declared by each entity's own {@code start init through "<origin>"} blocks, keyed to that entity
 * type so an origin declared on one entity (e.g. {@code Tree}) is never treated as valid for a
 * different entity (e.g. {@code Shrub}); the base init and standard substep events are always
 * present via {@link KnownEventSet}'s defaults. Later work can extend this to user-declared substep
 * events, scoped per simulation the same way init variants are scoped per entity here.</p>
 */
public class JoshLangEventSetVisitor extends JoshLangBaseVisitor<KnownEventSet> {

  /**
   * Create a new event-set discovery visitor.
   */
  public JoshLangEventSetVisitor() {
    super();
  }

  /**
   * Collect the per-origin init variants declared directly on this entity stanza.
   *
   * <p>{@code initStanza} nodes only ever appear directly nested in an {@code entityStanza} (see
   * the grammar), so this is the one place the declaring entity's name is known; each variant is
   * registered against that name rather than a program-wide bucket.</p>
   *
   * @param ctx The entity stanza (organism, patch, etc.) to collect declared init origins from.
   * @return The init variants declared by this entity, combined with anything found deeper in the
   *     tree (there is currently nothing else to find, since entities cannot nest).
   */
  @Override
  public KnownEventSet visitEntityStanza(JoshLangParser.EntityStanzaContext ctx) {
    String entityType = ctx.identifier().getText();
    KnownEventSet result = new KnownEventSet();
    for (JoshLangParser.InitStanzaContext initCtx : ctx.initStanza()) {
      String origin = StringLiteralUtil.stripQuotes(initCtx.STR_().getText());
      result.addInitOrigin(entityType, origin);
    }
    return result;
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
