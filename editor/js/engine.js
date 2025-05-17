/**
 * Strategies for executing simulations in different backend engines including browser WASM.
 *
 * @license BSD-3-Clause
 */

import {OutputDatum, SimulationResultBuilder} from "model";
import {ExternalDataSerializer, ResponseReader} from "wire";


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
   * @param externalData {Object} - JSON serializable object containing external resources.
   * @param onStepExternal {function} - Callback to invoke when a single step is completed. This
   *     may not be called depending on backend and number of replicates. Will pass the number of
   *     steps completed in the current replicate.
   * @param onReplicateExternal {function} - Callback to invoke when a single replicate is
   *     completed. Will pass the number of replicates completed in the current execution.
   * @returns {Promise<Array<SimulationResult>>} Resolves to the per-replicate simulation results or
   *     rejects if it encounters a runtime error.
   */
  execute(simCode, runRequest, externalData, onStepExternal, onReplicateExternal) {
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
          externalData,
          multiReplicate ? (x) => x : (x) => onStepExternal(x),
          runRequest.getPreferBigDecimal()
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
   * @param {string} leaderUrl - Url ending in /runReplicates where requests to execute should be
   *     sent along with the API key and simulation code.
   * @param {string} apiKey - The API key to send to the runSimluations endpoint to authenticate.
   */
  constructor(leaderUrl, apiKey) {
    const self = this;
    self._leaderUrl = leaderUrl;
    self._apiKey = apiKey;
    self._external_data_serializer = new ExternalDataSerializer();
  }

  /**
   * Fulfill a run request.
   *
   * @param simCode {string} - The code to run in this simulation.
   * @param runRequest {RunRequest} - Information about what simulation should run and with how many
   *     replicates.
   * @param externalData {Object} - JSON serializable descfription of external data.
   * @param onStepExternal {function} - Callback to invoke when a single step is completed. This
   *     will not be invoked when using the remote engine backend.
   * @param onReplicateExternal {function} - Callback to invoke when a single replicate is
   *     completed. Will pass the number of replicates completed in the current execution.
   * @returns {Promise<Array<SimulationResult>>} Resolves to the per-replicate simulation results or
   *     rejects if it encounters a runtime error. This is after collecting results by replicate
   *     number as they may not be guaranteed to return in order from all backends.
   */
  execute(simCode, runRequest, externalData, onStepExternal, onReplicateExternal) {
    const self = this;

    /**
     * Encode information required to run the simulation.
     *
     * @returns {FormData} Data to pass into the request.
     */
    const createFormData = () => {
      const formData = new FormData();

      const externalDataStr = self._external_data_serializer.serialize(externalData);
      
      formData.append("code", simCode);
      formData.append("name", runRequest.getSimName());
      formData.append("replicates", runRequest.getReplicates().toString());
      formData.append("apiKey", self._apiKey);
      formData.append("externalData", externalDataStr);
      formData.append("favorBigDecimal", runRequest.getPreferBigDecimal() ? "true" : "false")
      
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
        body: createFormData()
      };
    };
    
    return new Promise((resolve, reject) => {
      const onFetchResponse = (response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        const responseReader = new ResponseReader(onReplicateExternal);

        const readStream = () => {
          return reader.read().then((x) => {
            const done = x.done;
            const value = x.value;

            if (done) {
              const buffer = responseReader.getBuffer().trim();
              if (buffer !== "") {
                responseReader.processResponse(buffer);
              }
              resolve(responseReader.getCompleteReplicates());
              return;
            } else {
              responseReader.processResponse(decoder.decode(value, {stream: true}));
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
   *     "/runReplicates" and, if it does not, it will be appended. Null if not using server. May
   *     be empty if running locally such that an empty string should be passed for API key. If null
   *     and useServer is true, will use a default.
   * @param {bool} preferBigDecimal - Flag indicating if non-integer numbers should prefer to be in
   *     BigDecimal or double. True for BigDecimal and false for double.
   * @throws {Error} If useServer is true but apiKey is null.
   */
  constructor(simName, replicates, useServer, apiKey, endpoint, preferBigDecimal) {
    const self = this;
    
    self._simName = simName;
    self._replicates = replicates;
    self._useServer = useServer;
    self._preferBigDecimal = preferBigDecimal;
    
    if (useServer && apiKey === null) {
      throw new Error("API key cannot be null when using server");
    }
    
    self._apiKey = apiKey;
    
    if (useServer && endpoint !== null && !endpoint.endsWith("/runReplicates")) {
      self._endpoint = endpoint + "/runReplicates";
    } else {
      self._endpoint = endpoint;
    }
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

  /**
   * Gets whether the request should use a server.
   * 
   * @returns {boolean} True if using server, false if using browser WASM/JS.
   */
  useServer() {
    const self = this;
    return self._useServer;
  }

  /**
   * Gets the server endpoint URL.
   * 
   * @returns {?string} The server endpoint URL or null if not using server.
   */
  getEndpoint() {
    const self = this;
    return self._endpoint;
  }

  /**
   * Gets the API key for server authentication.
   * 
   * @returns {?string} The API key or null if not using server.
   */
  getApiKey() {
    const self = this;
    return self._apiKey;
  }

  /**
   * Determine if decimal or double (64-bit float) is preferred.
   *
   * @returns {bool} Flag indicating if non-integer numbers should prefer to be in BigDecimal or
   *     double. True for BigDecimal and false for double.
   */
  getPreferBigDecimal() {
    const self = this;
    return self._preferBigDecimal;
  }
}


export {RemoteEngineBackend, RunRequest, WasmEngineBackend};
