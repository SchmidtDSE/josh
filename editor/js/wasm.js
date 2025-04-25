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
          self._dataset.push(e.data.result);
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


class SimulationResultBuilder {

  constructor() {
    const self = this;
    self._simResults = [];
    self._patchResults = [];
    self._entityResults = [];
  }

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

  build() {
    const self = this;
    return new SimulationResult(self._simResults, self._patchResults, self._entityResults);
  }

}


class SimulationResult {

  constructor(simResults, patchResults, entityResults) {
    const self = this;
    self._simResults = simResults;
    self._patchResults = patchResults;
    self._entityResults = entityResults;
  }

  getSimResults() {
    const self = this;
    return self._simResults;
  }

  getPatchResults() {
    const self = this;
    return self._patchResults;
  }

  getEntityResults() {
    const self = this;
    return self._entityResults;
  }
  
}

  
/**
 * Class representing a possible code error or indication that no error was found.
 */
class CodeErrorMaybe {
  constructor(errorStr) {
    const self = this;
    self._errorStr = errorStr;
  }

  hasError() {
    const self = this;
    return self._errorStr !== "";
  }

  getError() {
    const self = this;
    return self._errorStr;
  }
}


let wasmLayer = null;

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
