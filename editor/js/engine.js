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

    /**
     * Encode information required to run the simulation.
     *
     * @returns {FormData} Data to pass into the request.
     */
    const createFormData = () => {
      const formData = new FormData();
      
      formData.append("code", simCode);
      formData.append("name", runRequest.getSimName());
      formData.append("replicates", runRequest.getReplicates().toString());
      
      return formData;
    };

    /**
     * Create the body of the request.
     *
     * @returns {Object} Request body which can be passed to fetch.
     */
    const createRequest = () => {
      return {
        method: "POST",
        headers: {
          "api-key": self._apiKey
        },
        body: createFormData()
      };
    };

    /**
     * Build a new reponse reader which splits by newlines with an internal buffer.
     *
     * @param {Map<number, Array<OutputDatum>>} replicateResults - The mapping by integer replicate
     *     number to data parsed for that replicate.
     * @returns {function} Function to call with text returned by the remote.
     */
    const buildResponseReader = (replicateResults) => {
      let buffer = "";
      let completedReplicates = 0;

      const processResponse = (text) => {
        buffer += text;
        const lines = buffer.split("\n");
        buffer = lines.pop();

        lines.map((x) => x.trim()).forEach((line) => {
          if (parsed.type === "datum") {
            if (!replicateResults.has(parsed.replicate)) {
              replicateResults.set(parsed.replicate, []);
            }
            replicateResults.get(parsed.replicate).push(parsed.datum);
          } else if (parsed.type === "end") {
            completedReplicates++;
            onReplicateExternal(completedReplicates);
          }
        });
      };

      return processResponse;
    };
    
    return new Promise((resolve, reject) => {
      const replicateResults = new Map();
      
      const onFetchResponse = (response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        const processResponse = buildResponseReader(replicateResults);

        const readStream = () => {
          return reader.read().then((x) => {
            const done = x.done;
            const value = x.value;

            if (done) {
              if (buffer.trim()) {
                processResponse(buffer);
              }
              const results = Array.from(replicateResults.values());
              resolve(results);
              return;
            } else {
              processResponse(decoder.decode(value, {stream: true}));
              return readStream();
            }
          });
        }

        readStream().catch(error => {
          reject(error);
        });
      };
      
      fetch(self._leaderUrl, createRequest())
        .then(onFetchResponse)
        .catch((error) => { reject(error); });
    });
  }
  
}


/**
 * Record for a simulation run request.
 */
class RunRequest {

  /**
   * Creates a new run request.
   * 
   * @param {string} simName - Name of the simulation to run.
   * @param {number} replicates - Number of times to replicate the simulation.
   * @param {boolean} useServer - Flag indicating if a server should be used to execute this run.
   *     True if a server should be used and false if WASM / JS within the browser should be used.
   * @param {?apiKey} apiKey - The API key to use in authenticating with the remote server. Null
   *     if not using server. May be empty if running on the same server as that which is currently
   *     serving the editor.
   * @param {?string} endpoint - The URL at which the server can be found which should end in
   *     "/runSimulations" and, if it does not, it will be appended. Null if not using server. May
   *     be empty if running locally such that an empty string should be passed for API key. If null
   *     and useServer is true, will use a default.
   */
  constructor(simName, replicates, useServer, apiKey, endpoint) {
    const self = this;
    self._simName = simName;
    self._replicates = replicates;
  }

  /**
   * Gets the simulation name.
   * 
   * @returns {string} The simulation name.
   */
  getSimName() {
    const self = this;
    return self._simName;
  }

  /**
   * Gets the number of replicates.
   * 
   * @returns {number} The number of replicates.
   */
  getReplicates() {
    const self = this;
    return self._replicates;
  }
}


export {RemoteEngineBackend, RunRequest, WasmEngineBackend};
