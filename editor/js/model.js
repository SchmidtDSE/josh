/**
 * Structures and logic for managing and manipulating data returned from simulations.
 *
 * @license BSD-3-Clause
 */


/**
 * Record of a simulation's results by target type.
 */
class SimulationResult {

  /**
   * Creates a new simulation result container.
   * 
   * @param {Array<OutputDatum>} simResults - Collection of simulation-level output records.
   * @param {Set<string>} simAttributes - Set of all attributes found across all elements of
   *     simResults.
   * @param {Array<OutputDatum>} patchResults - Collection of patch-level output records.
   * @param {Set<string>} patchAttributes - Set of all attributes found across all elements of
   *     patchResults.
   * @param {Array<OutputDatum>} entityResults - Collection of entity-level output records.
   * @param {Set<string>} entityAttributes - Set of all attributes found across all elements of
   *     entityResults.
   */
  constructor(simResults, simAttributes, patchResults, patchAttributes, entityResults,
        entityAttributes) {
    const self = this;
    self._simResults = simResults;
    self._simAttributes = simAttributes;
    self._patchResults = patchResults;
    self._patchAttributes = patchAttributes;
    self._entityResults = entityResults;
    self._entityAttributes = entityAttributes;
  }

  /**
   * Gets the collection of simulation-level output records.
   * 
   * @returns {Array<OutputDatum>} Array of simulation output records.
   */
  getSimResults() {
    const self = this;
    return self._simResults;
  }

  /**
   * Gets the collection of patch-level output records.
   * 
   * @returns {Array<OutputDatum>} Array of patch output records.
   */
  getPatchResults() {
    const self = this;
    return self._patchResults;
  }

  /**
   * Gets the collection of entity-level output records.
   * 
   * @returns {Array<OutputDatum>} Array of entity output records.
   */
  getEntityResults() {
    const self = this;
    return self._entityResults;
  }

  /**
   * Get all variables listed in the results for simulations.
   *
   * @returns {Set<string>} All of the vraiables listed on simulations.
   */
  getSimulationVariables() {
    const self = this;
    return self._simAttributes;
  }

  /**
   * Get all variables listed in the results for patches.
   *
   * @returns {Set<string>} All of the vraiables listed on patches.
   */
  getPatchVariables() {
    const self = this;
    return self._patchAttributes;
  }

  /**
   * Get all variables listed in the results for entities.
   *
   * @returns {Set<string>} All of the vraiables listed on entities.
   */
  getEntityVariables() {
    const self = this;
    return self._entityAttributes;
  }

}


/**
 * Builder for constructing simulation results from individual output records.
 */
class SimulationResultBuilder {

  /**
   * Creates a new simulation result builder with empty collections.
   */
  constructor() {
    const self = this;
    self._simResults = [];
    self._simAttributes = new Set();
    self._patchResults = [];
    self._patchAttributes = new Set();
    self._entityResults = [];
    self._entityAttributes = new Set();
  }

  /**
   * Adds a single output record to the appropriate collection based on its target type.
   * 
   * @param {OutputDatum} result - The output record to add to the builder.
   */
  add(result) {
    const self = this;
    const targetName = result.getTarget();
    
    const targetCollection = {
      "simulation": self._simResults,
      "patches": self._patchResults,
      "entites": self._entityResults
    }[targetName];
    targetCollection.push(result);

    const targetAttributes = {
      "simulation": self._simAttributes,
      "patches": self._patchAttributes,
      "entites": self._entityAttributes
    }[targetName];
    result.getAttributeNames().forEach((x) => targetAttributes.add(x));
  }

  /**
   * Constructs and returns a SimulationResult from the collected output records.
   * 
   * @returns {SimulationResult} A new SimulationResult containing all collected records.
   */
  build() {
    const self = this;
    return new SimulationResult(
      self._simResults,
      self._simAttributes,
      self._patchResults,
      self._patchAttributes,
      self._entityResults,
      self._entityAttributes
    );
  }

}

/**
 * Record describing an export from the engine running in WASM or JS emulation.
 */
class OutputDatum {

  /**
   * Create a new output record.
   *
   * @param {string} target - The name of the target as parsed from export URI.
   * @param {Map} attributes - Map from string name of attribute to the value of that attribute,
   *     either as a number if the input matched a number regex or a string otherwise.
   */
  constructor(target, attributes) {
    const self = this;
    self._target = target;
    self._attributes = attributes;
  }

  /**
   * Get the name of the target that this output record was for as parsed from export URI.
   *
   * @returns {string} The name of the target for this record.
   */
  getTarget() {
    const self = this;
    return self._target;
  }

  /**
   * Get all of the attributes within this record.
   *
   * @returns {Array<string>} The attribute names within this record.
   */
  getAttributeNames() {
    const self = this;
    return Array.from(self._attributes.keys());
  }

  /**
   * The value associated with the given attribute name.
   *
   * @throws Exception thrown if the value by the given name is not found.
   * @param {string} name - Name of the attribute for which a value should be retrieved.
   * @returns Attribute value Either as a number if the input matched a number regex or a string
   *     otherwise
   */
  getValue(name) {
    const self = this;
    if (!self._attributes.has(name)) {
      throw "Value for attribute " + name + " not found.";
    }
    return self._attributes.get(name);
  }

}


export {SimulationResult, SimulationResultBuilder, OutputDatum};
