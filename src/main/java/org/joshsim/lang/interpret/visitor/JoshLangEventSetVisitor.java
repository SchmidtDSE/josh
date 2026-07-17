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
 * declared by {@code start init through "<origin>"} blocks; the base init and standard substep
 * events are always present via {@link KnownEventSet}'s defaults. Later work can extend this to
 * user-declared substep events.</p>
 */
public class JoshLangEventSetVisitor extends JoshLangBaseVisitor<KnownEventSet> {

  /**
   * Create a new event-set discovery visitor.
   */
  public JoshLangEventSetVisitor() {
    super();
  }

  @Override
  public KnownEventSet visitInitStanza(JoshLangParser.InitStanzaContext ctx) {
    String origin = StringLiteralUtil.stripQuotes(ctx.STR_().getText());
    KnownEventSet result = new KnownEventSet();
    result.addInitOrigin(origin);

    KnownEventSet childResult = visitChildren(ctx);
    return childResult == null ? result : result.combine(childResult);
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
