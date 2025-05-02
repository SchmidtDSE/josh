importScripts("/war/js/JoshSim.js?v=0.0.1");
importScripts("/war/wasm-gc/JoshSim.wasm-runtime.js?v=0.0.1");
importScripts("/js/parse.js?v=0.0.1");

let wasmLayer = null;
let postMessage = null;


/**
 * Reports the completion of a simulation step to the main thread.
 * 
 * @param {number} stepCount - The number of steps completed in the simulation.
 */
function reportStepComplete(stepCount) {
  postMessage({ type: "reportStep", success: true, result: stepCount })
}


/**
 * Parses a data string from MemoryWriteStrategy and reports the parsed data to the main thread.
 * 
 * @param {string} source - The formatted string containing target name and key-value pairs.
 */
function reportData(source) {
  const datum = parseDatum(source);
  postMessage({ type: "outputDatum", success: true, result: datum });
}


/**
 * Report an error from the WASM execution.
 *
 * @param {string} message - Description of the message encountered.
 */
function reportError(message) {
  postMessage({ type: "error", success: false, error: message });
}


self.onmessage = async function(e) {
  const { type, data } = e.data;

  postMessage = (x) => self.postMessage(x);
  
  if (type === "init") {
    try {
      wasmLayer = await TeaVM.wasmGC.load("/war/wasm-gc/JoshSim.wasm");
      console.log("Started engine thread with WASM.");
    } catch (e) {
      console.log("Failed to load WASM, falling back to JS due to error:" + e);
      wasmLayer = {"exports": {
        "validate": validate,
        "getSimulations": getSimulations,
        "runSimulation": runSimulation,
        "getSimulationMetadata": getSimulationMetadata
      }};
      console.log("Started ending thread with JS fallback.");
    }
    self.postMessage({ type: "init", success: true });
    return;
  }

  if (!wasmLayer) {
    self.postMessage({ type: "error", error: "WASM layer not initialized" });
    return;
  }

  try {
    switch (type) {
      case "validate":
        const errorStr = wasmLayer.exports.validate(data);
        self.postMessage({ type: "validate", result: errorStr });
        break;
      
      case "getSimulations":
        const simulationsStr = wasmLayer.exports.getSimulations(data);
        self.postMessage({ type: "getSimulations", result: simulationsStr });
        break;
      
      case "runSimulation":
        wasmLayer.exports.runSimulation(data.code, data.simulationName);
        self.postMessage({ type: "runSimulation", success: true });
        break;

      case "getSimulationMetadata":
        const resultRaw = wasmLayer.exports.getSimulationMetadata(data.code, data.simulationName);
        const result = parseDatum(resultRaw);
        self.postMessage({ type: "getSimulationMetadata", success: true, result: result });
        break;
    }
  } catch (error) {
    self.postMessage({ type: "error", error: error.message });
  }
};
