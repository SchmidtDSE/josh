package org.joshsim.engine.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventHandlerGroupBuilder {
  ArrayList<EventHandler> eventHandlers;
  Optional<String> state;

  public EventHandlerGroup build() {
    return new EventHandlerGroup();
  }

  public void setState(String state) {
    this.state = Optional.of(state);
  }

  public void add(EventHandler eventHandler) {
    eventHandlers.add(eventHandler);
  }

  public void addAll(List<EventHandler> eventHandlers) {
    eventHandlers.addAll(eventHandlers);
  }

  public void clear() {
    eventHandlers.clear();
  }
}
