/**
 * Structures and logic for managing and manipulating data returned from simulations.
 *
 * @license BSD-3-Clause
 */

/**
 * Strategies for evaluating conditional statements within queries.
 * 
 * Set of conditional strateiges where x is the number to be checked, targetA is the first target
 * from the query being fulfilled, and targetB is the second target from the query being fulfilled.
 * The targetA will always be specified and acts as the threshold for exceeds and falls below.The
 * targetB will only be specified for is between. For is between, targetA is the minimum and targetB
 * is the maximum.
 */
const CONDITIONALS = {
  "exceeds": (x, targetA, targetB) => x > targetA,
  "falls below": (x, targetA, targetB) => x < targetA,
  "is between": (x, targetA, targetB) => x >= targetA && x <= targetB
}

/**
 * Strategies for generating a user-requested metric keyed by that metric name.
 *
 * Strategies for generating a user-requested metric keyed by that metric name where values are the
 * Array<number>s for which a metric is to be generated, type is the metric type like "exceeds" to
 * be used in parameterizing that metric, and targets A and B are used as inputs into that metric
 * generation if applicable.
 */
const METRIC_STRATEGIES = {
  "mean": (values, type, targetA, targetB) => math.mean(values),
  "median": (values, type, targetA, targetB) => math.median(values),
  "min": (values, type, targetA, targetB) => math.min(values),
  "max": (values, type, targetA, targetB) => math.max(values),
  "std": (values, type, targetA, targetB) => math.std(values),
  "probability": (values, type, targetA, targetB) => {
    const conditional = CONDITIONALS[type];
    const countTotal = values.length;
    
    const matching = values.filter((candidate) => conditional(candidate, targetA, targetB));
    const countMatching = matching.length;

    return countMatching / countTotal;
  }
};


/**
 * Record of a simulation's results by target type for a single replicate.
 */
class SimulationResult {

  /**
   * Creates a new simulation result container representing a single replicate.
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
   * @param {number} minX - The minimum X coordinate encountered.
   * @param {number} minY - The minimum Y coordinate encountered.
   * @param {number} maxX - The maximum X coordinate encountered.
   * @param {number} maxY - The maximum Y coordinate encountered.
   */
  constructor(simResults, simAttributes, patchResults, patchAttributes, entityResults,
        entityAttributes, minX, minY, maxX, maxY) {
    const self = this;
    self._simResults = simResults;
    self._simAttributes = simAttributes;
    self._patchResults = patchResults;
    self._patchAttributes = patchAttributes;
    self._entityResults = entityResults;
    self._entityAttributes = entityAttributes;
    self._minX = minX;
    self._minY = minY;
    self._maxX = maxX;
    self._maxY = maxY;
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

  /**
   * Gets the minimum X coordinate encountered.
   *
   * @returns {number} The minimum X coordinate.
   */
  getMinX() {
    const self = this;
    return self._minX;
  }

  /**
   * Gets the minimum Y coordinate encountered.
   *
   * @returns {number} The minimum Y coordinate.
   */
  getMinY() {
    const self = this;
    return self._minY;
  }

  /**
   * Gets the maximum X coordinate encountered.
   *
   * @returns {number} The maximum X coordinate.
   */
  getMaxX() {
    const self = this;
    return self._maxX;
  }

  /**
   * Gets the maximum Y coordinate encountered.
   *
   * @returns {number} The maximum Y coordinate.
   */
  getMaxY() {
    const self = this;
    return self._maxY;
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
    
    self._minX = null;
    self._minY = null;
    self._maxX = null;
    self._maxY = null;
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

    self._updateBounds(result);
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

  /**
   * Update the minimum and maximum x and y coordinates seen by this builder.
   *
   * Update the minimum and maximum x and y coordinates seen by this builder, using this result's
   * x and y coordinates as the minimum and maximum if no prior values seen. Note that this uses
   * position.x and position.y from result. If either position.x or position.y are not found, this
   * record is ignored.
   * 
   * @param {OutputDatum} result - The output record to add to the builder.
   */
  _updateBounds(result) {
    const hasPosX = result.hasValue("position.x");
    const hasPosY = result.hasValue("position.y");
    const hasPos = hasPosX && hasPosY;
    if (!hasPos) {
      return;
    }
    
    const posX = result.getValue("position.x");
    const posY = result.getValue("position.y");

    if (self._minX === null || posX < self._minX) {
      self._minX = posX;
    }
    
    if (self._minY === null || posY < self._minY) {
      self._minY = posY;
    }
    
    if (self._maxX === null || posX > self._maxX) {
      self._maxX = posX;
    }
    
    if (self._maxY === null || posY > self._maxY) {
      self._maxY = posY;
    }
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
   * Checks if the attribute with the given name exists in this record.
   *
   * @param {string} name - Name of the attribute to check.
   * @returns {boolean} True if the attribute exists, false otherwise.
   */
  hasValue(name) {
    const self = this;
    return self._attributes.has(name);
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
 * Record describing which variable the user wants to analyze and how.
 *
 * Record describing which variable exported from the script that the user wants to analyze and
 * indicate how those values should be reated (mean, median, etc). If the user is calculating
 * probabilities, this will also have one or two target values.
 */
class DataQuery {

  /**
   * Create a new record of a user-requested DataQuery.
   *
   * @param {string} variable The name of the variable as exported from the user's script to be
   *     analyzed.
   * @param {string} metric The kind of metric to be calculated like mean. This will be applied both
   *     at the simulation level (like mean across all patches across all timesteps) for the scrub
   *     element or similar and patch level (like mean for each patch across all timesteps).
   * @param {?string} metricType The sub-type of metric to be calculated. For example, if metric is
   *     probability, this may be exceeds or falls below. Ignored if the metric does not have sub-
   *     types and may be null in that case.
   * @param {?number} targetA The first reference value to use for probability metrics like the
   *     minimum threshold for proability of exceeds, maximum for probablity below, and minimum
   *     for probability within range. Should be null if not a probability (value ignored).
   * @param {?number} targetB The second reference value to use for probability metrics like the
   *     maximum for probability within range. Should be null if not a probability within range.
   */
  constructor(variable, metric, metricType, targetA, targetB) {
    const self = this;
    self._variable = variable;
    self._metric = metric;
    self._metricType = metricType;
    self._targetA = targetA;
    self._targetB = targetB;
  }

  /**
   * Get the variable name being analyzed.
   * 
   * @returns {string} The variable name.
   */
  getVariable() {
    const self = this;
    return self._variable;
  }

  /**
   * Get the metric type being calculated.
   * 
   * @returns {string} The metric type.
   */
  getMetric() {
    const self = this;
    return self._metric;
  }
  
  /**
   * Get the metric sub-type being calculated.
   * 
   * @returns {?string} The metric sub-type like "exceeds" or "falls below" or null if the given
   *     metric at getMetric does not have a sub-type.
   */
  getMetricType() {
    const self = this;
    return self._metricType;
  }


  /**
   * Get the first target value for probability metrics.
   * 
   * @returns {?number} The first target value or null.
   */
  getTargetA() {
    const self = this;
    return self._targetA;
  }

  /**
   * Get the second target value for probability metrics.
   * 
   * @returns {?number} The second target value or null.
   */
  getTargetB() {
    const self = this;
    return self._targetB;
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
    
    const timestepRounded = Math.round(timestep);
    const xRounded = Math.round(x);
    const yRounded = Math.round(y);
    const key = `${timestepRounded},${xRounded},${yRounded}`;
    
    if (!self._gridPerReplicate.has(key)) {
      throw new Error(`Grid value not found for timestep=${timestep}, x=${x}, y=${y}`);
    }
    
    return self._gridPerReplicate.get(key);
  }
  
}


/**
 * Summarize a dataset according to query specified by the user.
 *
 * Summarize a dataset according to query specified by the user using the strategies specified in
 * METRIC_STRATEGIES and, if applicable, CONDITIONALS.
 *
 * @param {Array<SimulationResult>} target - The simulation results with one element per replicate.
 * @param {DataQuery} query - Description of the query that the user is trying to execute with the
 *     information for the metric to be generated or summarized.
 * @returns {SummarizedResult} The target replicates summarized both where there is one overall
 *     value per timestep (summarized across all patches and replicates) and where there is one
 *     value per patch per timestep (summarized across all replicates per patch / timestep).
 */
function summarizeDataset(target, query) {
  // Get strategy for metric calculation
  const strategy = METRIC_STRATEGIES[query.getMetric()];
  if (!strategy) {
    throw new Error(`Unknown metric: ${query.getMetric()}`);
  }

  // Process each replicate
  const valuePerReplicate = target.map(replicate => {
    const patchResults = replicate.getPatchResults();
    const values = patchResults
      .filter(record => record.hasValue(query.getVariable()))
      .map(record => record.getValue(query.getVariable()));

    // Calculate metric for this replicate
    return strategy(values, query.getMetricType(), query.getTargetA(), query.getTargetB());
  });

  // Process grid values (patch by patch)
  const gridPerReplicate = new Map();
  
  // For each replicate
  target.forEach(replicate => {
    const patchResults = replicate.getPatchResults();
    
    // Group results by timestep and position
    patchResults.forEach(record => {
      if (!record.hasValue(query.getVariable())) return;
      
      if (record.hasValue("position.x") && record.hasValue("position.y") && record.hasValue("timestep")) {
        const timestep = Math.round(record.getValue("timestep"));
        const x = Math.round(record.getValue("position.x"));
        const y = Math.round(record.getValue("position.y"));
        const value = record.getValue(query.getVariable());
        
        const key = `${timestep},${x},${y}`;
        
        if (!gridPerReplicate.has(key)) {
          gridPerReplicate.set(key, []);
        }
        gridPerReplicate.get(key).push(value);
      }
    });
  });

  // Calculate metrics for each grid position
  for (const [key, values] of gridPerReplicate.entries()) {
    const metricValue = strategy(values, query.getMetricType(), query.getTargetA(), query.getTargetB());
    gridPerReplicate.set(key, metricValue);
  }

  // Get bounds from first replicate since they should all be the same
  const firstReplicate = target[0];
  return new SummarizedResult(
    firstReplicate.getMinX(),
    firstReplicate.getMinY(), 
    firstReplicate.getMaxX(),
    firstReplicate.getMaxY(),
    valuePerReplicate,
    gridPerReplicate
  );
}


export {DataQuery, OutputDatum, SimulationResult, SimulationResultBuilder};
