
importScripts("/war/wasm-gc/JoshSim.wasm-runtime.js");

let wasmLayer = null;
let postMessage = null;

function reportStepComplete(stepCount) {
  postMessage({ type: "reportStep", success: true, result: stepCount })
}

self.onmessage = async function(e) {
  const { type, data } = e.data;

  postMessage = (x) => self.postMessage(x);
  
  if (type === "init") {
    wasmLayer = await TeaVM.wasmGC.load("/war/wasm-gc/JoshSim.wasm");
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
    }
  } catch (error) {
    self.postMessage({ type: "error", error: error.message });
  }
};
