
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
  constructor() {
    const self = this;
    self._worker = new Worker('/editor/js/wasm.worker.js');
    self._initialized = false;
    self._initPromise = new Promise((resolve, reject) => {
      self._worker.onmessage = (e) => {
        const { type, result, error, success } = e.data;
        if (error) {
          reject(new Error(error));
          return;
        }
        if (type === 'init' && success) {
          self._initialized = true;
          resolve();
        }
      };
    });
    self._worker.postMessage({ type: 'init' });
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
        if (type === 'validate') {
          resolve(new CodeErrorMaybe(result));
        }
      };
      self._worker.postMessage({ type: 'validate', data: code });
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
        if (type === 'getSimulations') {
          resolve(result.split(","));
        }
      };
      self._worker.postMessage({ type: 'getSimulations', data: code });
    });
  }

  /**
   * Runs a simulation using the WASM layer.
   * 
   * @param {string} code - The code containing the simulation.
   * @param {string} simulationName - Name of simulation to run.
   * @returns {Promise<void>}
   */
  async runSimulation(code, simulationName) {
    const self = this;
    await self._initPromise;
    
    return new Promise((resolve, reject) => {
      self._worker.onmessage = (e) => {
        const { type, error, success } = e.data;
        if (error) {
          reject(new Error(error));
          return;
        }
        if (type === 'runSimulation' && success) {
          resolve();
        }
      };
      self._worker.postMessage({ 
        type: 'runSimulation', 
        data: { code, simulationName } 
      });
    });
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

export {WasmLayer};
