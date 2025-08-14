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
import org.joshsim.lang.interpret.fragment.josh.JoshFragment;
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
public class JoshParserToMachineVisitor extends JoshLangBaseVisitor<JoshFragment> {
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

  public JoshFragment visitIdentifier(JoshLangParser.IdentifierContext ctx) {
    return valueVisitor.visitIdentifier(ctx);
  }

  public JoshFragment visitNumber(JoshLangParser.NumberContext ctx) {
    return valueVisitor.visitNumber(ctx);
  }

  public JoshFragment visitUnitsValue(JoshLangParser.UnitsValueContext ctx) {
    return valueVisitor.visitUnitsValue(ctx);
  }

  public JoshFragment visitString(JoshLangParser.StringContext ctx) {
    return valueVisitor.visitString(ctx);
  }

  public JoshFragment visitBool(JoshLangParser.BoolContext ctx) {
    return valueVisitor.visitBool(ctx);
  }

  public JoshFragment visitAllExpression(JoshLangParser.AllExpressionContext ctx) {
    return valueVisitor.visitAllExpression(ctx);
  }

  public JoshFragment visitExternalValue(JoshLangParser.ExternalValueContext ctx) {
    return externalVisitor.visitExternalValue(ctx);
  }

  public JoshFragment visitExternalValueAtTime(JoshLangParser.ExternalValueAtTimeContext ctx) {
    return externalVisitor.visitExternalValueAtTime(ctx);
  }

  public JoshFragment visitConfigValue(JoshLangParser.ConfigValueContext ctx) {
    return configVisitor.visitConfigValue(ctx);
  }

  public JoshFragment visitConfigValueWithDefault(
      JoshLangParser.ConfigValueWithDefaultContext ctx) {
    return configVisitor.visitConfigValueWithDefault(ctx);
  }

  public JoshFragment visitMapLinear(JoshLangParser.MapLinearContext ctx) {
    return mathematicsVisitor.visitMapLinear(ctx);
  }

  public JoshFragment visitMapParam(JoshLangParser.MapParamContext ctx) {
    return mathematicsVisitor.visitMapParam(ctx);
  }

  public JoshFragment visitMapParamParam(JoshLangParser.MapParamParamContext ctx) {
    return mathematicsVisitor.visitMapParamParam(ctx);
  }

  public JoshFragment visitAdditionExpression(JoshLangParser.AdditionExpressionContext ctx) {
    return mathematicsVisitor.visitAdditionExpression(ctx);
  }

  public JoshFragment visitMultiplyExpression(JoshLangParser.MultiplyExpressionContext ctx) {
    return mathematicsVisitor.visitMultiplyExpression(ctx);
  }

  public JoshFragment visitPowExpression(JoshLangParser.PowExpressionContext ctx) {
    return mathematicsVisitor.visitPowExpression(ctx);
  }

  public JoshFragment visitParenExpression(JoshLangParser.ParenExpressionContext ctx) {
    return mathematicsVisitor.visitParenExpression(ctx);
  }

  public JoshFragment visitLimitBoundExpression(JoshLangParser.LimitBoundExpressionContext ctx) {
    return mathematicsVisitor.visitLimitBoundExpression(ctx);
  }

  public JoshFragment visitLimitMinExpression(JoshLangParser.LimitMinExpressionContext ctx) {
    return mathematicsVisitor.visitLimitMinExpression(ctx);
  }

  public JoshFragment visitLimitMaxExpression(JoshLangParser.LimitMaxExpressionContext ctx) {
    return mathematicsVisitor.visitLimitMaxExpression(ctx);
  }

  public JoshFragment visitSingleParamFunctionCall(
      JoshLangParser.SingleParamFunctionCallContext ctx) {
    return mathematicsVisitor.visitSingleParamFunctionCall(ctx);
  }

  public JoshFragment visitConcatExpression(JoshLangParser.ConcatExpressionContext ctx) {
    return stringOperationVisitor.visitConcatExpression(ctx);
  }

  public JoshFragment visitLogicalExpression(JoshLangParser.LogicalExpressionContext ctx) {
    return logicalVisitor.visitLogicalExpression(ctx);
  }

  public JoshFragment visitCondition(JoshLangParser.ConditionContext ctx) {
    return logicalVisitor.visitCondition(ctx);
  }

  public JoshFragment visitConditional(JoshLangParser.ConditionalContext ctx) {
    return logicalVisitor.visitConditional(ctx);
  }

  public JoshFragment visitFullConditional(JoshLangParser.FullConditionalContext ctx) {
    return logicalVisitor.visitFullConditional(ctx);
  }

  public JoshFragment visitFullElifBranch(JoshLangParser.FullElifBranchContext ctx) {
    return logicalVisitor.visitFullElifBranch(ctx);
  }

  public JoshFragment visitFullElseBranch(JoshLangParser.FullElseBranchContext ctx) {
    return logicalVisitor.visitFullElseBranch(ctx);
  }

  public JoshFragment visitSlice(JoshLangParser.SliceContext ctx) {
    return distributionVisitor.visitSlice(ctx);
  }

  public JoshFragment visitSampleSimple(JoshLangParser.SampleSimpleContext ctx) {
    return distributionVisitor.visitSampleSimple(ctx);
  }

  public JoshFragment visitSampleParam(JoshLangParser.SampleParamContext ctx) {
    return distributionVisitor.visitSampleParam(ctx);
  }

  public JoshFragment visitSampleParamReplacement(
      JoshLangParser.SampleParamReplacementContext ctx) {
    return distributionVisitor.visitSampleParamReplacement(ctx);
  }

  public JoshFragment visitUniformSample(JoshLangParser.UniformSampleContext ctx) {
    return distributionVisitor.visitUniformSample(ctx);
  }

  public JoshFragment visitNormalSample(JoshLangParser.NormalSampleContext ctx) {
    return distributionVisitor.visitNormalSample(ctx);
  }

  public JoshFragment visitCast(JoshLangParser.CastContext ctx) {
    return typesUnitsVisitor.visitCast(ctx);
  }

  public JoshFragment visitCastForce(JoshLangParser.CastForceContext ctx) {
    return typesUnitsVisitor.visitCastForce(ctx);
  }

  public JoshFragment visitNoopConversion(JoshLangParser.NoopConversionContext ctx) {
    return typesUnitsVisitor.visitNoopConversion(ctx);
  }

  public JoshFragment visitActiveConversion(JoshLangParser.ActiveConversionContext ctx) {
    return typesUnitsVisitor.visitActiveConversion(ctx);
  }

  public JoshFragment visitCreateVariableExpression(
      JoshLangParser.CreateVariableExpressionContext ctx) {
    return typesUnitsVisitor.visitCreateVariableExpression(ctx);
  }

  public JoshFragment visitAttrExpression(JoshLangParser.AttrExpressionContext ctx) {
    return typesUnitsVisitor.visitAttrExpression(ctx);
  }

  public JoshFragment visitPosition(JoshLangParser.PositionContext ctx) {
    return typesUnitsVisitor.visitPosition(ctx);
  }

  public JoshFragment visitCreateSingleExpression(
      JoshLangParser.CreateSingleExpressionContext ctx) {
    return typesUnitsVisitor.visitCreateSingleExpression(ctx);
  }

  public JoshFragment visitAssignment(JoshLangParser.AssignmentContext ctx) {
    return typesUnitsVisitor.visitAssignment(ctx);
  }

  public JoshFragment visitSpatialQuery(JoshLangParser.SpatialQueryContext ctx) {
    return typesUnitsVisitor.visitSpatialQuery(ctx);
  }

  public JoshFragment visitLambda(JoshLangParser.LambdaContext ctx) {
    return functionVisitor.visitLambda(ctx);
  }

  public JoshFragment visitReturn(JoshLangParser.ReturnContext ctx) {
    return functionVisitor.visitReturn(ctx);
  }

  public JoshFragment visitFullBody(JoshLangParser.FullBodyContext ctx) {
    return functionVisitor.visitFullBody(ctx);
  }

  public JoshFragment visitEventHandlerGroupMemberInner(
      JoshLangParser.EventHandlerGroupMemberInnerContext ctx) {
    return functionVisitor.visitEventHandlerGroupMemberInner(ctx);
  }

  public JoshFragment visitConditionalIfEventHandlerGroupMember(
      JoshLangParser.ConditionalIfEventHandlerGroupMemberContext ctx) {
    return functionVisitor.visitConditionalIfEventHandlerGroupMember(ctx);
  }

  public JoshFragment visitConditionalElifEventHandlerGroupMember(
      JoshLangParser.ConditionalElifEventHandlerGroupMemberContext ctx) {
    return functionVisitor.visitConditionalElifEventHandlerGroupMember(ctx);
  }

  public JoshFragment visitConditionalElseEventHandlerGroupMember(
      JoshLangParser.ConditionalElseEventHandlerGroupMemberContext ctx) {
    return functionVisitor.visitConditionalElseEventHandlerGroupMember(ctx);
  }

  public JoshFragment visitEventHandlerGroupSingle(
      JoshLangParser.EventHandlerGroupSingleContext ctx) {
    return functionVisitor.visitEventHandlerGroupSingle(ctx);
  }

  public JoshFragment visitEventHandlerGroupMultiple(
      JoshLangParser.EventHandlerGroupMultipleContext ctx) {
    return functionVisitor.visitEventHandlerGroupMultiple(ctx);
  }

  public JoshFragment visitEventHandlerGeneral(JoshLangParser.EventHandlerGeneralContext ctx) {
    return functionVisitor.visitEventHandlerGeneral(ctx);
  }

  public JoshFragment visitStateStanza(JoshLangParser.StateStanzaContext ctx) {
    return stanzaVisitor.visitStateStanza(ctx);
  }

  public JoshFragment visitEntityStanza(JoshLangParser.EntityStanzaContext ctx) {
    return stanzaVisitor.visitEntityStanza(ctx);
  }

  public JoshFragment visitUnitStanza(JoshLangParser.UnitStanzaContext ctx) {
    return stanzaVisitor.visitUnitStanza(ctx);
  }

  public JoshFragment visitConfigStatement(JoshLangParser.ConfigStatementContext ctx) {
    return stanzaVisitor.visitConfigStatement(ctx);
  }

  public JoshFragment visitImportStatement(JoshLangParser.ImportStatementContext ctx) {
    return stanzaVisitor.visitImportStatement(ctx);
  }

  public JoshFragment visitProgram(JoshLangParser.ProgramContext ctx) {
    return stanzaVisitor.visitProgram(ctx);
  }

}
