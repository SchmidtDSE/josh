package org.joshsim.engine.entity.prototype;


public interface EntityPrototypeStore {

  EntityPrototype get(String entityName);

  boolean has(String entityName);

}
