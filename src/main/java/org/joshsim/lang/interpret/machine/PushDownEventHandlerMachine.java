package org.joshsim.lang.interpret.machine;

import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.EngineValue;
import org.joshsim.lang.interpret.action.EventHandlerAction;

public class PushDownEventHandlerMachine implements EventHandlerMachine {

  public PushDownEventHandlerMachine(Scope scope) {

  }

  @Override
  public EventHandlerMachine pushIdentifier(String name) {
    return null;
  }

  @Override
  public EventHandlerMachine push(EngineValue value) {
    return null;
  }

  @Override
  public EventHandlerMachine applyMap(String strategy) {
    return null;
  }

  @Override
  public EventHandlerMachine add() {
    return null;
  }

  @Override
  public EventHandlerMachine subtract() {
    return null;
  }

  @Override
  public EventHandlerMachine multiply() {
    return null;
  }

  @Override
  public EventHandlerMachine divide() {
    return null;
  }

  @Override
  public EventHandlerMachine pow() {
    return null;
  }

  @Override
  public EventHandlerMachine and() {
    return null;
  }

  @Override
  public EventHandlerMachine or() {
    return null;
  }

  @Override
  public EventHandlerMachine xor() {
    return null;
  }

  @Override
  public EventHandlerMachine neq() {
    return null;
  }

  @Override
  public EventHandlerMachine gt() {
    return null;
  }

  @Override
  public EventHandlerMachine lt() {
    return null;
  }

  @Override
  public EventHandlerMachine eq() {
    return null;
  }

  @Override
  public EventHandlerMachine lteq() {
    return null;
  }

  @Override
  public EventHandlerMachine gteq() {
    return null;
  }

  @Override
  public EventHandlerMachine slice() {
    return null;
  }

  @Override
  public EventHandlerMachine condition(EventHandlerAction positive) {
    return null;
  }

  @Override
  public EventHandlerMachine branch(EventHandlerAction posAction, EventHandlerAction negAction) {
    return null;
  }

  @Override
  public EventHandlerMachine sample(boolean withReplacement) {
    return null;
  }

  @Override
  public EventHandlerMachine cast(String newUnits, boolean force) {
    return null;
  }

  @Override
  public EventHandlerMachine bound(boolean hasLower, boolean hasUpper) {
    return null;
  }

  @Override
  public EventHandlerMachine makeEntity(String entityType) {
    return null;
  }

  @Override
  public EventHandlerMachine executeSpatialQuery() {
    return null;
  }

  @Override
  public EventHandlerMachine pushAttribute(String attrName) {
    return null;
  }

  @Override
  public EventHandlerMachine randUniform() {
    return null;
  }

  @Override
  public EventHandlerMachine randNorm() {
    return null;
  }

  @Override
  public EventHandlerMachine abs() {
    return null;
  }

  @Override
  public EventHandlerMachine ceil() {
    return null;
  }

  @Override
  public EventHandlerMachine count() {
    return null;
  }

  @Override
  public EventHandlerMachine floor() {
    return null;
  }

  @Override
  public EventHandlerMachine log10() {
    return null;
  }

  @Override
  public EventHandlerMachine ln() {
    return null;
  }

  @Override
  public EventHandlerMachine max() {
    return null;
  }

  @Override
  public EventHandlerMachine mean() {
    return null;
  }

  @Override
  public EventHandlerMachine min() {
    return null;
  }

  @Override
  public EventHandlerMachine round() {
    return null;
  }

  @Override
  public EventHandlerMachine std() {
    return null;
  }

  @Override
  public EventHandlerMachine sum() {
    return null;
  }

  @Override
  public EventHandlerMachine create() {
    return null;
  }

  @Override
  public EventHandlerMachine saveLocalVariable(String identifierName) {
    return null;
  }

  @Override
  public EventHandlerMachine end() {
    return null;
  }

  @Override
  public boolean isEnded() {
    return false;
  }

  @Override
  public EngineValue getResult() {
    return null;
  }
}
