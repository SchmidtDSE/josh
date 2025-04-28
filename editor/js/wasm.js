/**
 * Logic for interfacing with WebAssembly components.
 * 
 * @license BSD-3-Clause
 */

import {SimulationResult, SimulationResultBuilder, OutputDatum} from "model";


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
   * Get metadata about a simulation like grid configuration.
   *
   * @param {string} code - The code from which to get the simulation metadata.
   * @param {string} simulationName - The name of the simulation for which metadata is requested.
   * @returns {Promise<OutputDatum>} Promise resolving to the metadata read.
   */
  async getSimulationMetadata(code, simulationName) {
    const self = this;
    await self._initPromise;

    return new Promise((resolve, reject) => {
      self._worker.onmessage = (e) => {
        const { type, result, error } = e.data;
        if (e.data.error) {
          reject(new Error(error));
          return;
        }
        if (type === "getSimulationMetadata") {
          const parsed = new OutputDatum(result["target"], result["attributes"]);
          resolve(parsed);
        }
      };
      self._worker.postMessage({
        type: "getSimulationMetadata",
        data: {code: code, simulationName: simulationName}
      });
    });
  }

  /**
   * Runs a single simulation replicate using the WASM layer.
   * 
   * @param {string} code - The code containing the simulation.
   * @param {string} simulationName - Name of simulation to run.
   * @returns {Promise<SimulationResult>} Promise which resolves to the complete dataset when the
   *     simulation is concluded with data on this single replicate.
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
        data: { code: code, simulationName: simulationName } 
      });
    });
  }

  _onStepCompleted(numComplete) {
    const self = this;
    self._stepCallback(numComplete);
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
