importScripts("/war/js/JoshSim.js");
importScripts("/war/wasm-gc/JoshSim.wasm-runtime.js");

const NUMBER_REGEX = /(\+|\-)?\d+(\.\d+)?/;

let wasmLayer = null;
let postMessage = null;


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
      throw new Error(`Attribute '${name}' not found`);
    }
    return this._attributes.get(name);
  }

}


function reportStepComplete(stepCount) {
  postMessage({ type: "reportStep", success: true, result: stepCount })
}


function reportData(source) {
  const [target, attributesStr] = source.split(':', 2);
  const attributes = new Map();
  
  if (attributesStr) {
    const pairs = attributesStr.split('\t');
    for (const pair of pairs) {
      const [key, value] = pair.split('=', 2);
      if (key && value !== undefined) {
        // Convert to number if matches number regex
        attributes.set(key, NUMBER_REGEX.test(value) ? parseFloat(value) : value);
      }
    }
  }
  
  const datum = new OutputDatum(target, attributes);
  postMessage({ type: "outputDatum", success: true, result: datum });
}


self.onmessage = async function(e) {
  const { type, data } = e.data;

  postMessage = (x) => self.postMessage(x);
  
  if (type === "init") {
    try {
      wasmLayer = await TeaVM.wasmGC.load("/war/wasm-gc/JoshSim.wasm");
      console.log("Started engine thread with WASM.");
    } catch {
      wasmLayer = {"exports": {
        "validate": validate,
        "getSimulations": getSimulations,
        "runSimulation": runSimulation
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
    }
  } catch (error) {
    self.postMessage({ type: "error", error: error.message });
  }
};
