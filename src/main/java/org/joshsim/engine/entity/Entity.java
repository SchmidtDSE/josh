package org.joshsim.engine.entity;

import java.util.HashMap;
import java.util.Optional;
import org.joshsim.engine.value.EngineValue;

public abstract class Entity implements Lockable, AttributeManaging {
  String name;
  Iterable<EventHandlerGroup> eventHandlers;
  HashMap<String, EngineValue> attributes;
  

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Iterable<EventHandlerGroup> getEventHandlers() {
    return eventHandlers;
  }

  @Override
  public Iterable<EventHandlerGroup> getEventHandlers(String attribute, String event) {
    // TODO Auto-generated method stub
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    return Optional.ofNullable(attributes.get(name));
  }

  @Override
  public void setAttributeValue(String name, EngineValue value) {
    attributes.put(name, value);
  }

  @Override
  public void lock() {
    
  }

  @Override
  public void unlock() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'unlock'");
  }
  
}
