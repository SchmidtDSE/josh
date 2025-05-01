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
   * Create a new engine fullfillment backend strategy which executes simulations via WASM.
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


/**
 * Engine backend strategy which uses a remote Josh server endpoint to execute simulations.
 */
class RemoteEngineBackend {

  /**
   * Create a new engine fullfillment backend strategy which executes via a remote server.
   *
   * @param {string} leaderUrl - Url ending in /runSimulations where requests to execute should be
   *     sent along with the API key and simulation code.
   * @param {string} apiKey - The API key to send to the runSimluations endpoint to authenticate.
   */
  constructor(leaderUrl, apiKey) {
    const self = this;
    self._leaderUrl = leaderUrl;
    self._apiKey = apiKey;
  }

  /**
   * Fulfill a run request.
   *
   * @param simCode {string} - The code to run in this simulation.
   * @param runRequest {RunRequest} - Information about what simulation should run and with how many
   *     replicates.
   * @param onStepExternal {function} - Callback to invoke when a single step is completed. This
   *     will not be invoked when using the remote engine backend.
   * @param onReplicateExternal {function} - Callback to invoke when a single replicate is
   *     completed. Will pass the number of replicates completed in the current execution.
   * @returns {Promise<Array<SimulationResult>>} Resolves to the per-replicate simulation results or
   *     rejects if it encounters a runtime error. This is after collecting results by replicate
   *     number as they may not be guaranteed to return in order from all backends.
   */
  execute(simCode, runRequest, onStepExternal, onReplicateExternal) {
    const formData = new FormData();
    formData.append('code', simCode);
    formData.append('name', runRequest.getSimName());
    formData.append('replicates', runRequest.getReplicates().toString());
    
    return new Promise((resolve, reject) => {
      const replicateResults = new Map();
      let completedReplicates = 0;
      
      fetch(this._leaderUrl, {
        method: 'POST',
        headers: {
          'api-key': this._apiKey
        },
        body: formData
      }).then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        
        function processText(text) {
          buffer += text;
          const lines = buffer.split('\n');
          buffer = lines.pop(); // Keep incomplete line in buffer
          
          for (const line of lines) {
            if (line.trim()) {
              const parsed = parseEngineResponse(line);
              if (parsed.type === 'datum') {
                if (!replicateResults.has(parsed.replicate)) {
                  replicateResults.set(parsed.replicate, []);
                }
                replicateResults.get(parsed.replicate).push(parsed.datum);
              } else if (parsed.type === 'end') {
                completedReplicates++;
                onReplicateExternal(completedReplicates);
              }
            }
          }
        }

        function readStream() {
          return reader.read().then(({done, value}) => {
            if (done) {
              if (buffer.trim()) {
                processText(buffer);
              }
              const results = Array.from(replicateResults.values());
              resolve(results);
              return;
            }
            
            processText(decoder.decode(value, {stream: true}));
            return readStream();
          });
        }

        readStream().catch(error => {
          reject(error);
        });
      }).catch(error => {
        reject(error);
      });
    });
  }
  
}


export {WasmEngineBackend};
