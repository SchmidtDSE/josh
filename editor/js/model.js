/**
 * Structures and logic for managing and manipulating data returned from simulations.
 *
 * @license BSD-3-Clause
 */


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

    self._getterStrategies = {
      "simulation": () => self.getSimResults(),
      "patches": () => self.getPatchResults(),
      "entities": () => self.getEntityResults()
    };

    self._variableGetterStrategies = {
      "simulation": () => self.getSimVariables(),
      "patches": () => self.getPatchVariables(),
      "entities": () => self.getEntityVariables()
    };
  }
  
  /**
   * Get a series from this replicate given the series' name.
   *
   * @param {string} name - The name of the series like simulation or patches.
   * @returns {Array<OutputDatum>} Array of output records for the requested series.
   */
  getSeries(name) {
    const self = this;
    const getter = self._getterStrategies[name];

    if (getter === undefined) {
      throw "Unknown series: " + name;
    }

    return getter();
  }

  /**
   * Get attributes on a series from this replicate given the series' name.
   *
   * @param {string} name - The name of the series like simulation or patches.
   * @returns {Array<string>} Array of attributes on that series.
   */
  getVariables(name) {
    const self = this;
    const getter = self._variableGetterStrategies[name];

    if (getter === undefined) {
      throw "Unknown series: " + name;
    }

    return getter();
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
   * @param {?number} totalSteps - The total number of steps in the simulation. Defaults to null.
   * @param {?number} minLongitude - The minimum longitude within this grid. Defaults to null.
   * @param {?number} minLatitude - The minimum latitude within this grid. Defaults to null.
   * @param {?number} maxLongitude - The maximum longitude within this grid. Defaults to null.
   * @param {?number} maxLatitude - The maximum latitude within this grid. Defaults to null.
   * @param {?number} startStep - The starting step of the simulation (steps.low value). Defaults to null.
   */
  constructor(startX, startY, endX, endY, patchSize, totalSteps, minLongitude, minLatitude, maxLongitude,
        maxLatitude, startStep) {
    const self = this;
    self._startX = startX;
    self._startY = startY;
    self._endX = endX;
    self._endY = endY;
    self._patchSize = patchSize;

    const defaultToNull = (x) => x === undefined ? null : x;

    self._totalSteps = defaultToNull(totalSteps);
    self._startStep = defaultToNull(startStep);
    self._minLongitude = defaultToNull(minLongitude);
    self._minLatitude = defaultToNull(minLatitude);
    self._maxLongitude = defaultToNull(maxLongitude);
    self._maxLatitude = defaultToNull(maxLatitude);
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
   * onComplete
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

  /**
   * Gets the minimum longitude within this grid.
   *
   * @returns {?number} The minimum longitude, or null if grid not defined in degrees.
   */
  getMinLongitude() {
    const self = this;
    return self._minLongitude;
  }

  /**
   * Gets the minimum latitude within this grid.
   *
   * @returns {?number} The minimum latitude, or null if grid not defined in degrees.
   */
  getMinLatitude() {
    const self = this;
    return self._minLatitude;
  }

  /**
   * Gets the maximum longitude within this grid.
   *
   * @returns {?number} The maximum longitude, or null if grid not defined in degrees.
   */
  getMaxLongitude() {
    const self = this;
    return self._maxLongitude;
  }

  /**
   * Gets the maximum latitude within this grid.
   *
   * @returns {?number} The maximum latitude, or null if grid not defined in degrees.
   */
  getMaxLatitude() {
    const self = this;
    return self._maxLatitude;
  }

  /**
   * Gets the total number of steps in the simulation.
   *
   * @returns {?number} The total number of steps, or null if not specified.
   */
  getTotalSteps() {
    const self = this;
    return self._totalSteps;
  }

  /**
   * Gets the starting step of the simulation (steps.low value).
   *
   * @returns {?number} The starting step, or null if not specified.
   */
  getStartStep() {
    const self = this;
    return self._startStep;
  }

  /**
   * Determine if this record has latitude and longitude specified.
   *
   * @returns {boolean} True if latitude and longitudes are specified and false otherwise.
   */
  hasDegrees() {
    const self = this;
    const hasLongitude = self.getMinLongitude() !== null && self.getMaxLongitude() !== null;
    const hasLatitude = self.getMinLatitude() !== null && self.getMaxLatitude() !== null;
    return hasLongitude && hasLatitude;
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
   * @returns {?number} The value at the specified grid location.
   */
  getGridValue(timestep, x, y) {
    const self = this;

    const timestepRounded = Math.round(timestep);
    const xRounded = x.toFixed(2);
    const yRounded = y.toFixed(2);
    const key = `${timestepRounded},${xRounded},${yRounded}`;

    if (!self._gridPerTimestep.has(key)) {
      return null;
    }

    return self._gridPerTimestep.get(key);
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
    // Normalize target name: URL-decode and remove leading slash if present
    // The backend now uses standard URI parsing which URL-encodes the path
    const rawTargetName = result.getTarget();
    const decodedTargetName = decodeURIComponent(rawTargetName);
    const targetName = decodedTargetName.startsWith("/") ? decodedTargetName.substring(1) : decodedTargetName;

    const targetCollection = {
      "simulation": self._simResults,
      "patches": self._patchResults,
      "entities": self._entityResults
    }[targetName];

    if (targetCollection === undefined) {
      console.warn(`Unknown target type: ${targetName} (raw: ${rawTargetName})`);
      return;
    }

    targetCollection.push(result);

    const targetAttributes = {
      "simulation": self._simAttributes,
      "patches": self._patchAttributes,
      "entities": self._entityAttributes
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


export {
  OutputDatum,
  SimulationMetadata,
  SimulationResult,
  SimulationResultBuilder,
  SummarizedResult
};
