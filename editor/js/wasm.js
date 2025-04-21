
/**
 * Logic for interfacing with WebAssembly components.
 * 
 * @license BSD-3-Clause
 */

/**
 * Wrapper class for WebAssembly layer functionality.
 *
 * Wrapper around the TeaVM export to WASM, managing interaction with the WASM VM and exported
 * functions.
 */
class WasmLayer {

  /**
   * Creates a new WASM layer wrapper.
   * 
   * @param {Object} rawWasmLayer - The raw WASM layer containing VM and exported functions.
   */
  constructor(rawWasmLayer) {
    const self = this;
    self._rawWasmLayer = rawWasmLayer;
  }

  /**
   * Validates code for errors using the WASM layer.
   * 
   * @param {string} code - The code to validate.
   * @returns {CodeErrorMaybe} Object containing any validation errors.
   */
  getError(code) {
    const self = this;
    const errorStr = self._rawWasmLayer.exports.validate(code);
    return new CodeErrorMaybe(errorStr);
  }

  /**
   * Gets available simulations from the provided code.
   * 
   * @param {string} code - The code to extract simulations from.
   * @returns {Array<string>} Array of simulation names.
   */
  getSimulations(code) {
    const self = this;
    const simulationsStr = self._rawWasmLayer.exports.getSimulations(code);
    return simulationsStr.split(",");
  }

  /**
   * Runs a simulation using the WASM layer.
   * 
   * @param {string} code - The code containing the simulation.
   * @param {string} simulationName - Name of simulation to run.
   */
  runSimulation(code, simulationName) {
    const self = this;
    self._rawWasmLayer.exports.runSimulation(code, simulationName);
  }
}

/**
 * Class representing a possible code error or indication that no error was found.
 */
class CodeErrorMaybe {

  /**
   * Creates a new error wrapper.
   * 
   * @param {string} errorStr - The error message if any.
   */
  constructor(errorStr) {
    const self = this;
    self._errorStr = errorStr;
  }

  /**
   * Checks if there is an error present.
   * 
   * @returns {boolean} True if there is an error.
   */
  hasError() {
    const self = this;
    return self._errorStr !== "";
  }

  /**
   * Gets the error message if present.
   * 
   * @returns {string} The error message.
   */
  getError() {
    const self = this;
    return self._errorStr;
  }
}

export {WasmLayer};
