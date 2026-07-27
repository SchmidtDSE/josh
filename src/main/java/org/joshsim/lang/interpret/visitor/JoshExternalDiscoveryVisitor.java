/**
 * Visitor to discover external data resources referenced by Josh scripts.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor;

import java.util.HashSet;
import java.util.Set;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;

/**
 * Visitor to discover external data resources referenced by Josh scripts.
 *
 * <p>This visitor traverses the parse tree and collects the name of every external resource the
 * script reads, so that callers can determine which {@code .jshd} / {@code .jshdz} files a model
 * needs without running it. Every read form contributes its name, including the temporal metadata
 * queries, so {@code external precipitation}, {@code external precipitation at year forcingYear},
 * and {@code first year of external precipitation} all discover {@code precipitation}.</p>
 *
 * <p>Only reads are collected. A {@code start external ...} stanza declares a source rather than
 * reading one, and the declaration alone does not require a preprocessed file to be supplied.</p>
 */
public class JoshExternalDiscoveryVisitor extends JoshLangBaseVisitor<Set<String>> {

  /**
   * Creates a new external discovery visitor.
   */
  public JoshExternalDiscoveryVisitor() {
    super();
  }

  @Override
  public Set<String> visitExternalValue(JoshLangParser.ExternalValueContext ctx) {
    return discover(ctx.name.getText(), ctx);
  }

  @Override
  public Set<String> visitExternalValueAtCoordinate(
      JoshLangParser.ExternalValueAtCoordinateContext ctx) {
    return discover(ctx.name.getText(), ctx);
  }

  @Override
  public Set<String> visitExternalValueAtTime(JoshLangParser.ExternalValueAtTimeContext ctx) {
    return discover(ctx.name.getText(), ctx);
  }

  @Override
  public Set<String> visitExternalFirstCoordinate(
      JoshLangParser.ExternalFirstCoordinateContext ctx) {
    return discover(ctx.name.getText(), ctx);
  }

  @Override
  public Set<String> visitExternalLastCoordinate(JoshLangParser.ExternalLastCoordinateContext ctx) {
    return discover(ctx.name.getText(), ctx);
  }

  @Override
  public Set<String> visitExternalTimeLength(JoshLangParser.ExternalTimeLengthContext ctx) {
    return discover(ctx.name.getText(), ctx);
  }

  @Override
  public Set<String> visitExternalTimeUnit(JoshLangParser.ExternalTimeUnitContext ctx) {
    return discover(ctx.name.getText(), ctx);
  }

  /**
   * Records a discovered resource name and continues traversal into the node's children.
   *
   * <p>Children are still visited because a coordinate expression may itself read another external
   * resource, as in {@code external rainfall at year (first year of external temperature)}.</p>
   *
   * @param name The name of the external resource that was read.
   * @param ctx The parse tree node in which the read appears.
   * @return The set of resource names discovered at and below this node.
   */
  private Set<String> discover(String name, org.antlr.v4.runtime.tree.RuleNode ctx) {
    Set<String> result = new HashSet<>();
    result.add(name);

    Set<String> childResult = visitChildren(ctx);
    if (childResult != null) {
      result.addAll(childResult);
    }

    return result;
  }

  @Override
  protected Set<String> defaultResult() {
    return new HashSet<>();
  }

  @Override
  protected Set<String> aggregateResult(Set<String> aggregate, Set<String> nextResult) {
    if (aggregate == null) {
      aggregate = new HashSet<>();
    }
    if (nextResult != null) {
      aggregate.addAll(nextResult);
    }
    return aggregate;
  }
}
