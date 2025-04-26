/**
 * Logic for interfacing with WebAssembly components.
 * 
 * @license BSD-3-Clause
 */


/**
 * Wrapper class for WebAssembly layer functionality.
 *
 * Wrapper around the TeaVM export to WASM, managing interaction with the WASM VM and exported
 * functions through a WebWorker.
 */
class WasmLayer {
  
  /**
   * Creates a new WASM layer wrapper.
   */
  constructor(stepCallback) {
    const self = this;
    self._stepCallback = stepCallback;
    self._worker = new Worker("/js/wasm.worker.js");
    self._initialized = false;
    self._datasetBuilder = null;
    self._initPromise = new Promise((resolve, reject) => {
      self._worker.onmessage = (e) => {
        const { type, result, error, success } = e.data;
        if (error) {
          reject(new Error(error));
          return;
        }
        if (type === "init" && success) {
          self._initialized = true;
          resolve();
        }
      };
    });
    self._worker.postMessage({ type: "init" });
  }

  /**
   * Validates code for errors using the WASM layer.
   * 
   * @param {string} code - The code to validate.
   * @returns {Promise<CodeErrorMaybe>} Object containing any validation errors.
   */
  async getError(code) {
    const self = this;
    await self._initPromise;
    
    return new Promise((resolve, reject) => {
      self._worker.onmessage = (e) => {
        const { type, result, error } = e.data;
        if (error) {
          reject(new Error(error));
          return;
        }
        if (type === "validate") {
          resolve(new CodeErrorMaybe(result));
        }
      };
      self._worker.postMessage({ type: "validate", data: code });
    });
  }

  /**
   * Gets available simulations from the provided code.
   * 
   * @param {string} code - The code to extract simulations from.
   * @returns {Promise<Array<string>>} Array of simulation names.
   */
  async getSimulations(code) {
    const self = this;
    await self._initPromise;
    
    return new Promise((resolve, reject) => {
      self._worker.onmessage = (e) => {
        const { type, result, error } = e.data;
        if (error) {
          reject(new Error(error));
          return;
        }
        if (type === "getSimulations") {
          resolve(result.split(","));
        }
      };
      self._worker.postMessage({ type: "getSimulations", data: code });
    });
  }

  /**
   * Runs a simulation using the WASM layer.
   * 
   * @param {string} code - The code containing the simulation.
   * @param {string} simulationName - Name of simulation to run.
   * @returns {Promise} Promise which resolves to the complete dataset when the simulation is
   *     concluded.
   */
  async runSimulation(code, simulationName) {
    const self = this;
    await self._initPromise;

    self._datasetBuilder = new SimulationResultBuilder();
    
    return new Promise((resolve, reject) => {
      self._worker.onmessage = (e) => {
        const { type, error, success } = e.data;
        if (error) {
          reject(new Error(error));
          return;
        }
        if (type === "runSimulation" && success) {
          // Have to throw us on top of the event queue
          const completedBuilder = self._datasetBuilder;
          setTimeout(() => { resolve(completedBuilder.build()); }, 100);
        } else if (type === "reportStep") {
          self._onStepCompleted(e.data.result);
        } else if (type === "outputDatum") {
          const rawInput = e.data.result;
          const parsed = new OutputDatum(rawInput["target"], rawInput["attributes"]);
          self._datasetBuilder.add(parsed);
        }
      };
      self._worker.postMessage({ 
        type: "runSimulation", 
        data: { code, simulationName } 
      });
    });
  }

  _onStepCompleted(numComplete) {
    const self = this;
    self._stepCallback(numComplete);
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
    self._patchResults = [];
    self._entityResults = [];
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
  }

  /**
   * Constructs and returns a SimulationResult from the collected output records.
   * 
   * @returns {SimulationResult} A new SimulationResult containing all collected records.
   */
  build() {
    const self = this;
    return new SimulationResult(self._simResults, self._patchResults, self._entityResults);
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
    return this._target;
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
    if (!this._attributes.has(name)) {
      throw "Value for attribute " + name + " not found.";
    }
    return this._attributes.get(name);
  }

}


/**
 * Record of a simulation's results by target type.
 */
class SimulationResult {

  /**
   * Creates a new simulation result container.
   * 
   * @param {Array<OutputDatum>} simResults - Collection of simulation-level output records.
   * @param {Array<OutputDatum>} patchResults - Collection of patch-level output records.
   * @param {Array<OutputDatum>} entityResults - Collection of entity-level output records.
   */
  constructor(simResults, patchResults, entityResults) {
    const self = this;
    self._simResults = simResults;
    self._patchResults = patchResults;
    self._entityResults = entityResults;
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
  
}

  
/**
 * Record of a possible code error or indication that no error was found.
 */
/**
 * Record of a possible code error or indication that no error was found.
 */
class CodeErrorMaybe {
  
  /**
   * Creates a new code error wrapper.
   * 
   * @param {string} errorStr - The error message if one exists, or empty string if no error.
   */
  constructor(errorStr) {
    const self = this;
    self._errorStr = errorStr;
  }

  /**
   * Checks if an error was encountered..
   * 
   * @returns {boolean} True if there is an error message, false otherwise.
   */
  hasError() {
    const self = this;
    return self._errorStr !== "";
  }

  /**
   * Gets the error message.
   * 
   * @returns {string} The error message or empty string if no error exists.
   */
  getError() {
    const self = this;
    return self._errorStr;
  }
}


let wasmLayer = null;

/**
 * Gets or creates the WASM layer singleton.
 * 
 * @param {function} stepCallback - Callback function that will be called with the number of
 *     completed steps when a simulation step finishes. Required when creating a new layer.
 * @returns {WasmLayer} The WASM layer singleton instance.
 * @throws {Error} If stepCallback is undefined when creating a new layer.
 */
function getWasmLayer(stepCallback) {
  if (wasmLayer === null) {
    if (stepCallback === undefined) {
      throw "Provide step callback to make a new wasm layer.";
    }

    wasmLayer = new WasmLayer(stepCallback);
  }

  return wasmLayer;
}


export {getWasmLayer};
