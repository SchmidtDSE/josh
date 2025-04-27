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


/**
 * Record describing a result which is summarized according to the instructions of the user.
 *
 * Record describing a result which is summarized according to the instructions of the user,
 * specifically a single metric on a single exported variable which is reported once per patch (cell
 * within the simluation grid) per timestep.
 */
class SummarizedResult {

  /**
   * Create a new record of a summarized result.
   *
   * @param {number} minX - The minimum x coordinate in the result set.
   * @param {number} minY - The minimum Y coordinate in the result set.
   * @param {number} maxX - The maximum x coordinate in the result set.
   * @param {number} maxY - The maximum Y coordinate in the result set.
   * @param {Array<number>} valuePerReplicate - Array where each element corresponds to the value
   *     requested by the user in which the zeroth element is the first replicate.
   * @param {Map<string, number>} gridPerReplicate - Cell by cell (patch by patch) values requested
   *     by the user in which the key is the integer timestep followed by a comma followed by the x
   *     coordinate followed by a comma followed by the y coordinate where x and y are rounded to
   *     the nearest integer.
   */
  constructor(minX, minY, maxX, maxY, valuePerReplicate, gridPerReplicate) {
    const self = this;
    self._minX = minX;
    self._minY = minY;
    self._maxX = maxX;
    self._maxY = maxY;
    self._valuePerReplicate = valuePerReplicate;
    self._gridPerReplicate = gridPerReplicate;
  }

  /**
   * Gets the minimum x coordinate in the result set.
   * 
   * @returns {number} The minimum x coordinate.
   */
  getMinX() {
    const self = this;
    return self._minX;
  }

  /**
   * Gets the minimum y coordinate in the result set.
   * 
   * @returns {number} The minimum y coordinate.
   */
  getMinY() {
    const self = this;
    return self._minY;
  }

  /**
   * Gets the maximum x coordinate in the result set.
   * 
   * @returns {number} The maximum x coordinate.
   */
  getMaxX() {
    const self = this;
    return self._maxX;
  }

  /**
   * Gets the maximum y coordinate in the result set.
   * 
   * @returns {number} The maximum y coordinate.
   */
  getMaxY() {
    const self = this;
    return self._maxY;
  }

  /**
   * Gets the number of replicates in the result set.
   * 
   * @returns {number} The number of replicates.
   */
  getNumReplicates() {
    const self = this;
    return self._valuePerReplicate.length;
  }

  /**
   * Gets the value for a specific replicate.
   * 
   * @param {number} replicateIndex - The index of the replicate to retrieve.
   * @returns {number} The value for the specified replicate.
   * @throws {Error} If the replicate index is out of bounds.
   */
  getReplicateValue(replicateIndex) {
    const self = this;
    if (replicateIndex < 0 || replicateIndex >= self._valuePerReplicate.length) {
      throw new Error("Replicate index out of bounds");
    }
    return self._valuePerReplicate[replicateIndex];
  }

  /**
   * Gets the grid value for a specific timestep and coordinate.
   * 
   * @param {number} timestep - The timestep to query.
   * @param {number} x - The x coordinate.
   * @param {number} y - The y coordinate.
   * @returns {number} The value at the specified grid location.
   * @throws {Error} If the grid location is not found.
   */
  getGridValue(timestep, x, y) {
    const self = this;
    const key = `${Math.round(timestep)},${Math.round(x)},${Math.round(y)}`;
    if (!self._gridPerReplicate.has(key)) {
      throw new Error(`Grid value not found for timestep=${timestep}, x=${x}, y=${y}`);
    }
    return self._gridPerReplicate.get(key);
  }
  
}


function summarizeDataset() {
  
}


export {SimulationResult, SimulationResultBuilder, OutputDatum};
