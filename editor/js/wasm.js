/**
 * Logic for interfacing with WebAssembly components.
 * 
 * @license BSD-3-Clause
 */

import {SimulationMetadata, SimulationResult, SimulationResultBuilder, OutputDatum} from "model";
import {getDistanceMeters} from "util";
import {ExternalDataSerializer} from "wire";


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
    self._worker = new Worker("/js/wasm.worker.js");
    self._initialized = false;
    self._datasetBuilder = null;
    self._wireSerializer = new ExternalDataSerializer();
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
          const parsedToKeyValue = new OutputDatum(result["target"], result["attributes"]);
          const parsed = self._parseMetadata(parsedToKeyValue);
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
   * @param {Object} externalData - JSON-serializable object containing external data resources.
   * @param {function} stepCallback - The function to invoke when a step is completed, passing the
   *     number of steps completed.
   * @param {bool} preferBigDecimal - Flag indicating if BigDecimal should be preferred for numbers
   *     or not. True if BigDecimal and false if double / float64.
   * @returns {Promise<SimulationResult>} Promise which resolves to the complete dataset when the
   *     simulation is concluded with data on this single replicate.
   */
  async runSimulation(code, simulationName, externalData, stepCallback, preferBigDecimal) {
    const self = this;
    await self._initPromise;

    const externalDataStr = self._wireSerializer.serialize(externalData);
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
          stepCallback(e.data.result);
        } else if (type === "outputDatum") {
          const rawInput = e.data.result;
          const parsed = new OutputDatum(rawInput["target"], rawInput["attributes"]);
          self._datasetBuilder.add(parsed);
        }
      };
      self._worker.postMessage({ 
        type: "runSimulation",
        data: {
          code: code,
          simulationName: simulationName,
          data: externalDataStr,
          preferBigDecimal: preferBigDecimal
        } 
      });
    });
  }

  /**
   * Construct a metadata record from a parsed entity.
   *
   * Construct a metdata record from a entity which has raw data from the WASM worker parsed. It
   * expects input to have the following:
   *
   *  - "grid.size" which has a value like "1 count" or "30 m" or "30 meters" or "0.5 degree" or
   *    "0.5 degrees".
   *  - "grid.low" which has a value like "0 count latitude, 0 count longitude" or
   *    "34 degrees longitude, -116 degrees latitude".
   *  - "grid.high" which has a value like "10 count latitude, 10 count longitude" or
   *    "35 degrees longitude, -115 degrees latitude".
   *
   * If the start and end are indicated in degrees, they will be converted to count where x and y
   * both start at 0, 0 in the upper left-hand corner. Note that patch or cell centers are used so,
   * if the grid starts at 0, 0 count with a grid size of 1, then the first cell is at 0.5, 0.5.
   * This will convert degrees to meters but will otherwise not perform unit conversions, raising an
   * exception instead.
   *
   * @param {OutputDatum} input - Record which has parsed attributes and values returned by the WASM
   *     worker.
   * @returns {SimulationMetadata} Record summarizing input into a formalized metadata record.
   */
  _parseMetadata(input) {
    const self = this;

    const gridSizeParts = input.getValue("sizeStr").split(" ");
    const gridSize = parseFloat(gridSizeParts[0]);
    const gridUnits = gridSizeParts[1];

    const gridLowParts = input.getValue("startStr").split(", ");
    const pointUnits = gridLowParts[0].split(" ")[1];
    const gridHighParts = input.getValue("endStr").split(", ");
    
    let startX = 0, startY = 0, endX = 0, endY = 0;

    const usesDegrees = self._isDegrees(pointUnits);
    if (usesDegrees && self._isMeters(gridUnits)) {
      const lowLongitudeFirst = gridLowParts[0].split(" ")[2] === "longitude";
      const highLongitudeFirst = gridHighParts[0].split(" ")[2] === "longitude";
      
      const lowFirst = parseFloat(gridLowParts[0].split(" ")[0]);
      const lowSecond = parseFloat(gridLowParts[1].split(" ")[0]);
      const highFirst = parseFloat(gridHighParts[0].split(" ")[0]);
      const highSecond = parseFloat(gridHighParts[1].split(" ")[0]);

      const lowLon = lowLongitudeFirst ? lowFirst : lowSecond;
      const lowLat = lowLongitudeFirst ? lowSecond : lowFirst;
      const highLon = highLongitudeFirst ? highFirst : highSecond;
      const highLat = highLongitudeFirst ? highSecond : highFirst;
      
      const width = self._getDistanceMeters(lowLon, lowLat, highLon, lowLat);
      const height = self._getDistanceMeters(lowLon, lowLat, lowLon, highLat);
      
      endX = Math.ceil(width / gridSize);
      endY = Math.ceil(height / gridSize);
      
      if (usesDegrees) {
        return new SimulationMetadata(
          startX,
          startY,
          endX,
          endY,
          1,
          Math.min(lowLon, highLon),
          Math.min(lowLat, highLat),
          Math.max(lowLon, highLon),
          Math.max(lowLat, highLat)
        );
      } else {
        return new SimulationMetadata(startX, startY, endX, endY, gridSize);
      }
    } else if (gridUnits === pointUnits) {
      startX = parseFloat(gridLowParts[0].split(" ")[0]);
      startY = parseFloat(gridLowParts[1].split(" ")[0]);
      endX = parseFloat(gridHighParts[0].split(" ")[0]);
      endY = parseFloat(gridHighParts[1].split(" ")[0]);
      return new SimulationMetadata(startX, startY, endX, endY, gridSize);
    } else {
      throw `Cannot use web editor for grid with unequal units ${gridUnits} and ${pointUnits}.`;
    }
  }

  /**
   * Get the distance in meters between two coordinates provided in degrees using Haversine.
   *
   * @param {number} startLongitude - The first point longitude in degrees.
   * @param {number} startLatitude - The first point latitude in degrees.
   * @param {number} endLongitude - The second point longitude in degrees.
   * @param {number} endLatitude - The second point latitude in degrees.
   * @return {number} Absolute approximate distance between these two points.
   */
  _getDistanceMeters(startLongitude, startLatitude, endLongitude, endLatitude) {
    return getDistanceMeters(startLongitude, startLatitude, endLongitude, endLatitude);
  }

  /**
   * Get if a units string is describing count.
   *
   * @param {string} unitsStr - The units string to check.
   * @returns {boolean} True if this units string represents count and false otherwise.
   */
  _isCount(unitsStr) {
    const self = this;
    return unitsStr === "count" || unitsStr === "counts"
  }

  /**
   * Get if a units string is describing count.
   *
   * @param {string} unitsStr - The units string to check.
   * @returns {boolean} True if this units string represents count and false otherwise.
   */
  _isMeters(unitsStr) {
    const self = this;
    return unitsStr === "m" || unitsStr === "meter" || unitsStr === "meters";
  }

  /**
   * Get if a units string is describing count.
   *
   * @param {string} unitsStr - The units string to check.
   * @returns {boolean} True if this units string represents count and false otherwise.
   */
  _isDegrees(unitsStr) {
    const self = this;
    return unitsStr === "degree" || unitsStr === "degrees";
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


export {WasmLayer};
