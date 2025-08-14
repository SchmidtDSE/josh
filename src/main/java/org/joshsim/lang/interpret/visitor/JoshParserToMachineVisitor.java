/**
 * Visitor for Josh sources that parses to an interpreter runtime builder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.visitor;

import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.antlr.JoshLangBaseVisitor;
import org.joshsim.lang.antlr.JoshLangParser;
import org.joshsim.lang.interpret.BridgeGetter;
import org.joshsim.lang.interpret.fragment.Fragment;
import org.joshsim.lang.interpret.visitor.delegates.DelegateToolbox;
import org.joshsim.lang.interpret.visitor.delegates.JoshConfigVisitor;
import org.joshsim.lang.interpret.visitor.delegates.JoshDistributionVisitor;
import org.joshsim.lang.interpret.visitor.delegates.JoshExternalVisitor;
import org.joshsim.lang.interpret.visitor.delegates.JoshFunctionVisitor;
import org.joshsim.lang.interpret.visitor.delegates.JoshLogicalVisitor;
import org.joshsim.lang.interpret.visitor.delegates.JoshMathematicsVisitor;
import org.joshsim.lang.interpret.visitor.delegates.JoshStanzaVisitor;
import org.joshsim.lang.interpret.visitor.delegates.JoshStringOperationVisitor;
import org.joshsim.lang.interpret.visitor.delegates.JoshTypesUnitsVisitor;
import org.joshsim.lang.interpret.visitor.delegates.JoshValueVisitor;


/**
 * Visitor which parses Josh source by using Fragments.
 *
 * <p>Visitor which acts as composite of delegates which handle sections of the language
 * functionality where each produces a Fragment. These fragments handle the fact that each handler
 * produces a separate component with a different type that get combined into callable actions which
 * actually get executed at runtime.</p>
 */
@SuppressWarnings("checkstyle:MissingJavaDocMethod")  // Can't use override because of generics.
public class JoshParserToMachineVisitor extends JoshLangBaseVisitor<Fragment> {
  private final JoshValueVisitor valueVisitor;
  private final JoshExternalVisitor externalVisitor;
  private final JoshConfigVisitor configVisitor;
  private final JoshMathematicsVisitor mathematicsVisitor;
  private final JoshStringOperationVisitor stringOperationVisitor;
  private final JoshLogicalVisitor logicalVisitor;
  private final JoshDistributionVisitor distributionVisitor;
  private final JoshTypesUnitsVisitor typesUnitsVisitor;
  private final JoshFunctionVisitor functionVisitor;
  private final JoshStanzaVisitor stanzaVisitor;

  /**
   * Create a new visitor which has some commonly used values cached.
   *
   * @param valueFactory The factory to use in building engine values within this visitor.
   * @param bridgeGetter The bridge getter to use in accessing a bridge for operations.
   */
  public JoshParserToMachineVisitor(EngineValueFactory valueFactory, BridgeGetter bridgeGetter) {
    super();

    DelegateToolbox toolbox = new DelegateToolbox(this, valueFactory, bridgeGetter);
    valueVisitor = new JoshValueVisitor(toolbox);
    externalVisitor = new JoshExternalVisitor(toolbox);
    configVisitor = new JoshConfigVisitor(toolbox);
    mathematicsVisitor = new JoshMathematicsVisitor(toolbox);
    stringOperationVisitor = new JoshStringOperationVisitor(toolbox);
    logicalVisitor = new JoshLogicalVisitor(toolbox);
    distributionVisitor = new JoshDistributionVisitor(toolbox);
    typesUnitsVisitor = new JoshTypesUnitsVisitor(toolbox);
    functionVisitor = new JoshFunctionVisitor(toolbox);
    stanzaVisitor = new JoshStanzaVisitor(toolbox);
  }

  public Fragment visitIdentifier(JoshLangParser.IdentifierContext ctx) {
    return valueVisitor.visitIdentifier(ctx);
  }

  public Fragment visitNumber(JoshLangParser.NumberContext ctx) {
    return valueVisitor.visitNumber(ctx);
  }

  public Fragment visitUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    return valueVisitor.visitUnitsValue(ctx);
  }

  public Fragment visitString(JoshLangParser.StringContext ctx) {
    return valueVisitor.visitString(ctx);
  }

  public Fragment visitBool(JoshLangParser.BoolContext ctx) {
    return valueVisitor.visitBool(ctx);
  }

  public Fragment visitAllExpression(JoshLangParser.AllExpressionContext ctx) {
    return valueVisitor.visitAllExpression(ctx);
  }

  public Fragment visitExternalValue(JoshLangParser.ExternalValueContext ctx) {
    return externalVisitor.visitExternalValue(ctx);
  }

  public Fragment visitExternalValueAtTime(JoshLangParser.ExternalValueAtTimeContext ctx) {
    return externalVisitor.visitExternalValueAtTime(ctx);
  }

  public Fragment visitConfigValue(JoshLangParser.ConfigValueContext ctx) {
    return configVisitor.visitConfigValue(ctx);
  }

  public Fragment visitConfigValueWithDefault(JoshLangParser.ConfigValueWithDefaultContext ctx) {
    return configVisitor.visitConfigValueWithDefault(ctx);
  }

  public Fragment visitMapLinear(JoshLangParser.MapLinearContext ctx) {
    return mathematicsVisitor.visitMapLinear(ctx);
  }

  public Fragment visitMapParam(JoshLangParser.MapParamContext ctx) {
    return mathematicsVisitor.visitMapParam(ctx);
  }

  public Fragment visitMapParamParam(JoshLangParser.MapParamParamContext ctx) {
    return mathematicsVisitor.visitMapParamParam(ctx);
  }

  public Fragment visitAdditionExpression(JoshLangParser.AdditionExpressionContext ctx) {
    return mathematicsVisitor.visitAdditionExpression(ctx);
  }

  public Fragment visitMultiplyExpression(JoshLangParser.MultiplyExpressionContext ctx) {
    return mathematicsVisitor.visitMultiplyExpression(ctx);
  }

  public Fragment visitPowExpression(JoshLangParser.PowExpressionContext ctx) {
    return mathematicsVisitor.visitPowExpression(ctx);
  }

  public Fragment visitParenExpression(JoshLangParser.ParenExpressionContext ctx) {
    return mathematicsVisitor.visitParenExpression(ctx);
  }

  public Fragment visitLimitBoundExpression(JoshLangParser.LimitBoundExpressionContext ctx) {
    return mathematicsVisitor.visitLimitBoundExpression(ctx);
  }

  public Fragment visitLimitMinExpression(JoshLangParser.LimitMinExpressionContext ctx) {
    return mathematicsVisitor.visitLimitMinExpression(ctx);
  }

  public Fragment visitLimitMaxExpression(JoshLangParser.LimitMaxExpressionContext ctx) {
    return mathematicsVisitor.visitLimitMaxExpression(ctx);
  }

  public Fragment visitSingleParamFunctionCall(JoshLangParser.SingleParamFunctionCallContext ctx) {
    return mathematicsVisitor.visitSingleParamFunctionCall(ctx);
  }

  public Fragment visitConcatExpression(JoshLangParser.ConcatExpressionContext ctx) {
    return stringOperationVisitor.visitConcatExpression(ctx);
  }

  public Fragment visitLogicalExpression(JoshLangParser.LogicalExpressionContext ctx) {
    return logicalVisitor.visitLogicalExpression(ctx);
  }

  public Fragment visitCondition(JoshLangParser.ConditionContext ctx) {
    return logicalVisitor.visitCondition(ctx);
  }

  public Fragment visitConditional(JoshLangParser.ConditionalContext ctx) {
    return logicalVisitor.visitConditional(ctx);
  }

  public Fragment visitFullConditional(JoshLangParser.FullConditionalContext ctx) {
    return logicalVisitor.visitFullConditional(ctx);
  }

  public Fragment visitFullElifBranch(JoshLangParser.FullElifBranchContext ctx) {
    return logicalVisitor.visitFullElifBranch(ctx);
  }

  public Fragment visitFullElseBranch(JoshLangParser.FullElseBranchContext ctx) {
    return logicalVisitor.visitFullElseBranch(ctx);
  }

  public Fragment visitSlice(JoshLangParser.SliceContext ctx) {
    return distributionVisitor.visitSlice(ctx);
  }

  public Fragment visitSampleSimple(JoshLangParser.SampleSimpleContext ctx) {
    return distributionVisitor.visitSampleSimple(ctx);
  }

  public Fragment visitSampleParam(JoshLangParser.SampleParamContext ctx) {
    return distributionVisitor.visitSampleParam(ctx);
  }

  public Fragment visitSampleParamReplacement(JoshLangParser.SampleParamReplacementContext ctx) {
    return distributionVisitor.visitSampleParamReplacement(ctx);
  }

  public Fragment visitUniformSample(JoshLangParser.UniformSampleContext ctx) {
    return distributionVisitor.visitUniformSample(ctx);
  }

  public Fragment visitNormalSample(JoshLangParser.NormalSampleContext ctx) {
    return distributionVisitor.visitNormalSample(ctx);
  }

  public Fragment visitCast(JoshLangParser.CastContext ctx) {
    return typesUnitsVisitor.visitCast(ctx);
  }

  public Fragment visitCastForce(JoshLangParser.CastForceContext ctx) {
    return typesUnitsVisitor.visitCastForce(ctx);
  }

  public Fragment visitNoopConversion(JoshLangParser.NoopConversionContext ctx) {
    return typesUnitsVisitor.visitNoopConversion(ctx);
  }

  public Fragment visitActiveConversion(JoshLangParser.ActiveConversionContext ctx) {
    return typesUnitsVisitor.visitActiveConversion(ctx);
  }

  public Fragment visitCreateVariableExpression(
      JoshLangParser.CreateVariableExpressionContext ctx) {
    return typesUnitsVisitor.visitCreateVariableExpression(ctx);
  }

  public Fragment visitAttrExpression(JoshLangParser.AttrExpressionContext ctx) {
    return typesUnitsVisitor.visitAttrExpression(ctx);
  }

  public Fragment visitPosition(JoshLangParser.PositionContext ctx) {
    return typesUnitsVisitor.visitPosition(ctx);
  }

  public Fragment visitCreateSingleExpression(JoshLangParser.CreateSingleExpressionContext ctx) {
    return typesUnitsVisitor.visitCreateSingleExpression(ctx);
  }

  public Fragment visitAssignment(JoshLangParser.AssignmentContext ctx) {
    return typesUnitsVisitor.visitAssignment(ctx);
  }

  public Fragment visitSpatialQuery(JoshLangParser.SpatialQueryContext ctx) {
    return typesUnitsVisitor.visitSpatialQuery(ctx);
  }

  public Fragment visitLambda(JoshLangParser.LambdaContext ctx) {
    return functionVisitor.visitLambda(ctx);
  }

  public Fragment visitReturn(JoshLangParser.ReturnContext ctx) {
    return functionVisitor.visitReturn(ctx);
  }

  public Fragment visitFullBody(JoshLangParser.FullBodyContext ctx) {
    return functionVisitor.visitFullBody(ctx);
  }

  public Fragment visitEventHandlerGroupMemberInner(
      JoshLangParser.EventHandlerGroupMemberInnerContext ctx) {
    return functionVisitor.visitEventHandlerGroupMemberInner(ctx);
  }

  public Fragment visitConditionalIfEventHandlerGroupMember(
      JoshLangParser.ConditionalIfEventHandlerGroupMemberContext ctx) {
    return functionVisitor.visitConditionalIfEventHandlerGroupMember(ctx);
  }

  public Fragment visitConditionalElifEventHandlerGroupMember(
      JoshLangParser.ConditionalElifEventHandlerGroupMemberContext ctx) {
    return functionVisitor.visitConditionalElifEventHandlerGroupMember(ctx);
  }

  public Fragment visitConditionalElseEventHandlerGroupMember(
      JoshLangParser.ConditionalElseEventHandlerGroupMemberContext ctx) {
    return functionVisitor.visitConditionalElseEventHandlerGroupMember(ctx);
  }

  public Fragment visitEventHandlerGroupSingle(JoshLangParser.EventHandlerGroupSingleContext ctx) {
    return functionVisitor.visitEventHandlerGroupSingle(ctx);
  }

  public Fragment visitEventHandlerGroupMultiple(
      JoshLangParser.EventHandlerGroupMultipleContext ctx) {
    return functionVisitor.visitEventHandlerGroupMultiple(ctx);
  }

  public Fragment visitEventHandlerGeneral(JoshLangParser.EventHandlerGeneralContext ctx) {
    return functionVisitor.visitEventHandlerGeneral(ctx);
  }

  public Fragment visitStateStanza(JoshLangParser.StateStanzaContext ctx) {
    return stanzaVisitor.visitStateStanza(ctx);
  }

  public Fragment visitEntityStanza(JoshLangParser.EntityStanzaContext ctx) {
    return stanzaVisitor.visitEntityStanza(ctx);
  }

  public Fragment visitUnitStanza(JoshLangParser.UnitStanzaContext ctx) {
    return stanzaVisitor.visitUnitStanza(ctx);
  }

  public Fragment visitConfigStatement(JoshLangParser.ConfigStatementContext ctx) {
    return stanzaVisitor.visitConfigStatement(ctx);
  }

  public Fragment visitImportStatement(JoshLangParser.ImportStatementContext ctx) {
    return stanzaVisitor.visitImportStatement(ctx);
  }

  public Fragment visitProgram(JoshLangParser.ProgramContext ctx) {
    return stanzaVisitor.visitProgram(ctx);
  }

}
