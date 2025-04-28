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
      self._entityAttributes,
      self._minX,
      self._minY,
      self._maxX,
      self._maxY
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
    const self = this;
    
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
   * @param {Map<string, number>} valuePerTimestep - Array where each element corresponds to the
   *     value requested by the user in which the zeroth element is the first timestep.
   * @param {Map<string, number>} gridPerTimestep - Cell by cell (patch by patch) values requested
   *     by the user in which the key is the integer timestep followed by a comma followed by the x
   *     coordinate followed by a comma followed by the y coordinate where x and y are rounded to
   *     the nearest hundredth.
   */
  constructor(minX, minY, maxX, maxY, valuePerTimestep, gridPerTimestep) {
    const self = this;
    self._minX = minX;
    self._minY = minY;
    self._maxX = maxX;
    self._maxY = maxY;
    self._valuePerTimestep = valuePerTimestep;
    self._gridPerTimestep = gridPerTimestep;
    self._timesteps = Array.of(...self._valuePerTimestep.keys());
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
   * Gets the timesteps in the result set.
   * 
   * @returns {Array<number>} All timesteps found in this result set.
   */
  getTimesteps() {
    const self = this;
    return self._timesteps;
  }

  /**
   * Get the minimum timestep found within this dataset.
   *
   * @returns {number} Minimum timestep from this dataset.
   */
  getMinTimestep() {
    const self = this;
    return math.min(self._timesteps);
  }

  /**
   * Get the maximum timestep found within this dataset.
   *
   * @returns {number} Maximum timestep from this dataset.
   */
  getMaxTimestep() {
    const self = this;
    return math.max(self._timesteps);
  }

  /**
   * Gets the value for a specific replicate.
   * 
   * @param {number} timestep - The timestep to retrieve.
   * @returns {number} The value for the specified timestep.
   */
  getTimestepValue(timestep) {
    const self = this;
    return self._valuePerTimestep.get(timestep);
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
    
    if (!self._gridPerTimestep.has(key)) {
      throw new Error(`Grid value not found for timestep=${timestep}, x=${x}, y=${y}`);
    }
    
    return self._gridPerTimestep.get(key);
  }
  
}


/**
 * Information about the metadata from a simulation including grid initalization information.
 */
class SimulationMetadata {

  /**
   * Create a new metadata record.
   *
   * @param {number} startX - The minimum horizontal position of a patch in grid space where
   *     coordinates in degrees are automatically converted to a grid with 0, 0 in upper left.
   * @param {number} startY - The minimum vertical position of a patch in grid space where
   *     coordinates in degrees are automatically converted to a grid with 0, 0 in upper left.
   * @param {number} endX - The maximum horizontal position of a patch in grid space.
   * @param {number} endY - The maximum vertical positoin of a patch in grid space.
   * @param {number} patchSize - The size of each patch or cell, typically 1.
   */
  constructor(startX, startY, endX, endY, patchSize) {
    const self = this;
    self._startX = startX;
    self._startY = startY;
    self._endX = endX;
    self._endY = endY;
  }

  /**
   * Gets the minimum horizontal position of a patch in grid space.
   * 
   * @returns {number} The starting X coordinate where coordinates in degrees are 
   *     automatically converted to a grid with 0,0 in upper left.
   */
  getStartX() {
    const self = this;
    return self._startX;
  }

  /**
   * Gets the minimum vertical position of a patch in grid space.
   * 
   * @returns {number} The starting Y coordinate where coordinates in degrees are
   *     automatically converted to a grid with 0,0 in upper left.
   */
  getStartY() {
    const self = this;
    return self._startY;
  }

  /**
   * Gets the maximum horizontal position of a patch in grid space.
   * 
   * @returns {number} The ending X coordinate in grid space.
   */
  getEndX() {
    const self = this;
    return self._endX;
  }

  /**
   * Gets the maximum vertical position of a patch in grid space.
   * 
   * @returns {number} The ending Y coordinate in grid space.
   */
  getEndY() {
    const self = this;
    return self._endY;
  }

  /**
   * Gets the size of each patch/cell in the grid.
   * 
   * @returns {number} The patch size, typically 1.
   */
  getPatchSize() {
    const self = this;
    return self._patchSize;
  }
  
}


/**
 * Summarize datasets according to query specified by the user by operating on patch results.
 *
 * Summarize datasets according to query specified by the user using the strategies specified in
 * METRIC_STRATEGIES and, if applicable, CONDITIONALS. This will only summarize patch-level results.
 *
 * @param {Array<SimulationResult>} target - The simulation results with one element per replicate.
 * @param {DataQuery} query - Description of the query that the user is trying to execute with the
 *     information for the metric to be generated or summarized.
 * @returns {SummarizedResult} The target replicates summarized both where there is one overall
 *     value per timestep (summarized across all patches and replicates) and where there is one
 *     value per patch per timestep (summarized across all replicates per patch / timestep).
 */
function summarizeDatasets(target, query) {
  if (target.length == 0) {
    throw "Requires at least one replicate to summarize.";
  }
  
  const strategy = METRIC_STRATEGIES[query.getMetric()];
  if (!strategy) {
    throw `Unknown metric: ${query.getMetric()}`;
  }

  const variable = query.getVariable();
  const metricType = query.getMetricType();
  const targetA = query.getTargetA();
  const targetB = query.getTargetB();
  const curriedStrategy = (values) => strategy(values, metricType, targetA, targetB);

  const targetFlat = target.flatMap((x) => x.getPatchResults()).filter((x) => x.hasValue(variable));

  /**
   * Get a map from key to all values from records with that key.
   *
   * @param {function} keyGetter - The funciton taking an OutputDatum from which the key should be
   *     returned.
   * @returns {Map<string, Array<number>>} Mapping from key to values found for all records with the
   *     key and with the target attribute present.
   */
  const getValuesByKey = (keyGetter) => {
    const keyedValuesUnsafe = targetFlat.map((x) => {
      return {"key": keyGetter(x), "value": x.getValue(variable)};
    });

    const keyedValues = keyedValuesUnsafe.filter((x) => x["key"] !== null);
    
    const valuesByKey = new Map();
    keyedValues.forEach((target) => {
      const key = target["key"];
      const value = target["value"];

      if (!valuesByKey.has(key)) {
        valuesByKey.set(key, []);
      }

      valuesByKey.get(key).push(value)
    });

    return valuesByKey;
  }

  /**
   * Get a map from key to the metric value from records with that key.
   *
   * @param {function} keyGetter - The funciton taking an OutputDatum from which the key should be
   *     returned.
   * @returns {Map<string, number>} Mapping from key to metric value.
   */
  const summarizeByKey = (keyGetter) => {
    const valuesByKey = getValuesByKey(keyGetter);
    const valueByKey = new Map();

    valuesByKey.keys().forEach((key) => {
      const values = valuesByKey.get(key);
      const value = curriedStrategy(values);
      valueByKey.set(key, value);
    });

    return valueByKey;
  };

  const firstReplicate = target[0];
  return new SummarizedResult(
    firstReplicate.getMinX(),
    firstReplicate.getMinY(), 
    firstReplicate.getMaxX(),
    firstReplicate.getMaxY(),
    summarizeByKey(getTimestepKey),
    summarizeByKey(getGridKey)
  );
}


/**
 * Extracts the timestep value from a given record.
 * 
 * @param {OutputDatum} record - The output record from which the step will be retrieved.
 * @returns {?number} The step value if it exists, or null if not present.
 */
function getTimestepKey(record) {
  if (!record.hasValue("step")) {
    return null;
  }

  return record.getValue("step");
}


/**
 * Extracts a composite key from a given record based on its timestep and position.
 *
 * @param {OutputDatum} record - The output record from which the key will be constructed.
 * @returns {?string} A string key in the format "timestep,x,y" if all values exist, or null if any
 *     are missing.
 */
function getGridKey(record) {
  const hasX = record.hasValue("position.x");
  const hasY = record.hasValue("position.y");
  const hasTime = record.hasValue("step");
  const hasRequired = hasX && hasY && hasTime;
  if (!hasRequired) {
    return null;
  }

  const timestep = Math.round(record.getValue("step"));
  const x = record.getValue("position.x").toFixed(2);
  const y = record.getValue("position.y").toFixed(2);
  return `${timestep},${x},${y}`;
}


export {DataQuery, OutputDatum, SimulationResult, SimulationResultBuilder, summarizeDatasets};
