
/**
 * Main entry point for the Josh web editor application.
 * 
 * Initializes and coordinates the different components of the editor including file handling, code
 * editing, run panel, and results display.
 * 
 * @license BSD-3-Clause
 */

import {EditorPresenter} from "editor";
import {RemoteEngineBackend, WasmEngineBackend} from "engine";
import {FilePresenter} from "file";
import {ResultsPresenter} from "results";
import {RunPanelPresenter} from "run";
import {WasmLayer} from "wasm";


/**
 * Main presenter class that coordinates all editor and results display components.
 */
class MainPresenter {

  /**
   * Creates a new MainPresenter instance.
   */
  constructor(initalizationCallback) {
    const self = this;

    self._currentRequest = null;
    self._replicatesCompleted = 0;
    self._replicateResults = [];
    self._metadata = null;

    self._wasmLayer = new WasmLayer();

    self._filePresenter = new FilePresenter("file-buttons", (code) => {
      self._editorPresenter.setCode(code);
      self._onCodeChange(code);
    });
    
    self._editorPresenter = new EditorPresenter("code-editor", (code) => {
      self._onCodeChange(code);
    });

    self._runPresenter = new RunPanelPresenter(
      "code-buttons-panel",
      () => self._wasmLayer.getSimulations(self._editorPresenter.getCode()),
      (request) => { self._executeRunRequest(request); }
    );

    self._resultsPresenter = new ResultsPresenter("results");

    const priorCode = self._filePresenter.getCodeInFile();
    if (priorCode) {
      self._editorPresenter.setCode(priorCode);
    }

    self._showContents();
  }

  /**
   * Shows the main editor interface and hide the loading screen.
   */
  _showContents() {
    const self = this;
    document.getElementById("loading").style.display = "none";
    document.getElementById("main-holder").style.display = "block";
  }

  /**
   * Callback for when code is changed in the editor.
   *
   * @param {string} code - The content of the editor.
   */
  _onCodeChange(code) {
    const self = this;
    self._filePresenter.saveCodeToFile(code);
    
    self._wasmLayer.getError(code).then((errorMaybe) => {
      if (errorMaybe.hasError()) {
        self._editorPresenter.showError(errorMaybe.getError());
        self._runPresenter.hideButtons();
      } else {
        self._editorPresenter.hideError();
        self._runPresenter.showButtons();
      }
    });
  }

  /**
   * Executes a simulation run request.
   *
   * @param {Object} request - The run request containing simulation parameters.
   */
  _executeRunRequest(request) {
    const self = this;
    
    self._resultsPresenter.onSimStart();
    self._runPresenter.hideButtons();

    self._currentRequest = request;
    self._replicatesCompleted = 0;
    self._replicateResults = [];

    const simCode = self._editorPresenter.getCode();
    const simName = self._currentRequest.getSimName();

    self._wasmLayer.getSimulationMetadata(simCode, simName).then(
      (metadata) => {
        self._metadata = metadata;
        self._executeInBackend();
      },
      (x) => { self._onError(x); }
    );
  }

  /**
   * Execute the simulation code in the currently configured engine backend.
   */
  _executeInBackend() {
    const self = this;
    const engineBackend = self._buildEngineBackend();
    engineBackend.execute(
      self._editorPresenter.getCode(),
      self._currentRequest,
      (x) => self._onStepCompleted(x, "steps"),
      (x) => self._onStepCompleted(x, "replicates")
    ).then(
      (x) => self._onRunComplete(x),
      (x) => self._onError(x)
    );
  }

  /**
   * Build the backend given the current request.
   */
  _buildEngineBackend() {
    const self = this;

    if (self._currentRequest === null) {
      throw "No request active. Cannot build backend";
    }
    
    if (self._currentRequest.useServer()) {
      return new RemoteEngineBackend(
        self._currentRequest.getEndpoint(),
        self._currentRequest.getApiKey()
      );
    } else {
      return new WasmEngineBackend(self._wasmLayer);
    }
  }
  
  /**
   * Callback for when a simulation run is completed.
   *
   * @param {Array<SimulationResult>} results - The completed simulation results.
   */
  _onRunComplete(results) {
    const self = this;
    self._replicateResults = results;
    self._runPresenter.showButtons();
    self._resultsPresenter.onComplete(self._metadata, self._replicateResults);
  }

  /**
   * Callback for when a simulation step is completed.
   *
   * @param {number} numCompleted - The number of items completed.
   * @param {string} typeCompleted - The type of items being reported like steps or replicates.
   */
  _onStepCompleted(numCompleted, typeCompleted) {
    const self = this;
    self._resultsPresenter.onStep(numCompleted, typeCompleted);
  }

  /**
   * Handles errors that occur during simulation run requests.
   *
   * @param {string} message - The error message to be displayed.
   */
  _onError(message) {
    const self = this;
    
    self._runPresenter.showButtons();
    self._resultsPresenter.onError(message);
  }
}

/**
 * Initializes the editor and file handling components.
 * 
 * @returns {MainPresenter} The main presenter instance.
 */
function main() {
  return new MainPresenter();
}

export {main};
