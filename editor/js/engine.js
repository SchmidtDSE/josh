/**
 * Strategies for executing simulations in different backend engines including browser WASM.
 *
 * @license BSD-3-Clause
 */


/**
 * An engine executional backend strategy that uses WASM (or JS emulation) in the browser.
 */
class WasmEngineBackend {

  /**
   * Create a new engine fullfillment backend strategy which executes simulations via WASM
   *
   * @param {WasmLayer} wasmLayer - The WASM layer through which the simulations should execute.
   */
  constructor(wasmLayer) {
    const self = this;
    self._wasmLayer = wasmLayer;
  }

  /**
   * Fulfill a run request.
   *
   * @param simCode {string} - The code to run in this simulation.
   * @param runRequest {RunRequest} - Information about what simulation should run and with how many
   *     replicates.
   * @param onStepExternal {function} - Callback to invoke when a single step is completed. This
   *     may not be called depending on backend and number of replicates. Will pass the number of
   *     steps completed in the current replicate.
   * @param onReplicateExternal {function} - Callback to invoke when a single replicate is
   *     completed. Will pass the number of replicates completed in the current execution.
   * @returns {Promise<Array<SimulationResult>>} Resolves to the per-replicate simulation results or
   *     rejects if it encounters a runtime error.
   */
  execute(simCode, runRequest, onStepExternal, onReplicateExternal) {
    const self = this;

    const simName = runRequest.getSimName();
    const numReplicatesToRun = runRequest.getReplicates();
    const multiReplicate = numReplicatesToRun > 1;
    
    return new Promise((resolve, reject) => {
      const replicateResults = [];

      const onSimulationComplete = (results) => {
        replicateResults.push(results);
       
        const replicatesCompleteCount = replicateResults.length;
        onReplicateExternal(replicatesCompleteCount);
        if (replicatesCompleteCount >= numReplicatesToRun) {
          resolve(replicateResults);
        } else {
          runReplicate();
        }
      };

      const onError = (error) => {
        reject(error);
      };

      const runReplicate = () => {
        self._wasmLayer.runSimulation(
          simCode,
          simName,
          multiReplicate ? (x) => x : (x) => onStepExternal(x)
        ).then(
          (x) => { onSimulationComplete(x); },
          (x) => { onError(x); }
        );
      };

      runReplicate();
    });
  }
  
}


export {WasmEngineBackend};
