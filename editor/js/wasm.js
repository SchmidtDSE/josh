class WasmLayer {

  constructor(rawWasmLayer) {
    const self = this;
    self._rawWasmLayer = rawWasmLayer;
  }

  getError(code) {
    const self = this;
    const errorStr = self._rawWasmLayer.exports.validate(code);
    return new CodeErrorMaybe(errorStr);
  }

  getSimulations(code) {
    const self = this;
    const simulationsStr = self._rawWasmLayer.exports.getSimulations(code);
    return simulationsStr.split(",");
  }

  runSimulation(code, simulationName) {
    const self = this;
    self._rawWasmLayer.exports.runSimulation(code, simulationName);
  }

}



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
