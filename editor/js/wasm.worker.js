
let wasmLayer = null;

self.onmessage = async function(e) {
  const { type, data } = e.data;
  
  if (type === 'init') {
    await TeaVM.wasmGC.load("/war/wasm-gc/JoshSim.wasm");
    wasmLayer = TeaVM;
    self.postMessage({ type: 'init', success: true });
    return;
  }

  if (!wasmLayer) {
    self.postMessage({ type: 'error', error: 'WASM layer not initialized' });
    return;
  }

  try {
    switch (type) {
      case 'validate':
        const errorStr = wasmLayer.exports.validate(data);
        self.postMessage({ type: 'validate', result: errorStr });
        break;
      
      case 'getSimulations':
        const simulationsStr = wasmLayer.exports.getSimulations(data);
        self.postMessage({ type: 'getSimulations', result: simulationsStr });
        break;
      
      case 'runSimulation':
        wasmLayer.exports.runSimulation(data.code, data.simulationName);
        self.postMessage({ type: 'runSimulation', success: true });
        break;
    }
  } catch (error) {
    self.postMessage({ type: 'error', error: error.message });
  }
};
